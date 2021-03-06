package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.TaskListener
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asFilteredList
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.TagService
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.alignment
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.contextMenu
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.progressIndicator
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.builder.textInputDialog
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.tooltip
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.Icons
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.layout.Priority

private const val DEFAULT_STYLE_CLASS = "commit-log-view"
private const val CONTENT_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__content"
private const val INDICATOR_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__indicator"
private const val SEARCH_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__search"
private const val OVERLAY_STYLE_CLASS = "overlay"

/**
 * Displaying basically the output of `git log`. Each log entry can be selected to display the details of that
 * [Commit].
 * This is relying heavily on the [GraphListView] and its skin for displaying the log graph and commit list.
 *
 * There is also a context menu added to the [GraphListView] for commit related actions.
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
 *   ┃ ToolBar                              ┃
 *   ┠──────────────────────────────────────┨
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┃ GraphListView                        ┃
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
 *   ┃                                      ┃
 *   ┃ CommitDetailsView                    ┃
 *   ┃                                      ┃
 *   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 * ```
 *
 *
 * @todo should the actions be exposed and used in the menu bar as well?!
 * @todo loading more commits while scrolling down is buggy
 *
 * @see GraphListView
 * @see CommitDetailsView
 */
class CommitLogView : Tab() {

    private val state = TinyGit.get<State>()
    private val logService = TinyGit.get<CommitLogService>()
    private val filterableCommits = logService.commits.asFilteredList()
    private val branchService = TinyGit.get<BranchService>()
    private val tagService = TinyGit.get<TagService>()
    private val graph = GraphListView(filterableCommits)
    private val graphSelection get() = graph.selectionModel.selectedItem

    init {
        text = I18N["commitLog.tab"]
        graphic = Icons.list()
        isClosable = false

        val checkoutCommit = Action(I18N["commitLog.checkout"], { Icons.check() }, disabled = state.canCheckoutCommit.not(),
                handler = { checkoutCommit(graphSelection) })
        val resetToCommit = Action(I18N["commitLog.reset"], { Icons.refresh() }, disabled = state.canResetToCommit.not(),
                handler = { resetToCommit(graphSelection) })
        val tagCommit = Action(I18N["commitLog.tag"], { Icons.tag() }, disabled = state.canTagCommit.not(),
                handler = { tagCommit(graphSelection) })

        graph.items.addListener(ListChangeListener { graph.selectionModel.selectedItem ?: graph.selectionModel.selectFirst() })
        graph.selectionModel.selectedItemProperty().addListener { _, _, it -> logService.activeCommit.set(it) }
        graph.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(checkoutCommit, resetToCommit, tagCommit)
        }
//        TODO
//        graph.setOnScroll {
//            if (it.deltaY < 0) {
//                val index = graph.items.size - 1
//                service.logMore()
//                graph.scrollTo(index)
//            }
//        }
//        graph.setOnKeyPressed {
//            if (it.code == KeyCode.DOWN && graph.selectionModel.selectedItem == graph.items.last()) {
//                service.logMore()
//                graph.scrollTo(graph.selectionModel.selectedItem)
//            }
//        }

        val indicator = FetchIndicator()
        content = vbox {
            addClass(DEFAULT_STYLE_CLASS)

            +toolBar {
                +stackPane {
                    addClass(SEARCH_STYLE_CLASS)
                    +Icons.search().alignment(Pos.CENTER_LEFT)
                    +textField {
                        promptText = "${I18N["commitLog.search"]} (beta)"
                        textProperty().addListener { _, _, it -> filterCommits(it) }
                    }
                }
                addSpacer()
                +indicator
                +comboBox<CommitLogService.CommitType> {
                    items.addAll(CommitLogService.CommitType.values())
                    valueProperty().bindBidirectional(logService.commitType)
                    valueProperty().addListener { _, _, it -> graph.isGraphVisible = !it.isNoMerges }
                }
                +comboBox<CommitLogService.Scope> {
                    items.addAll(CommitLogService.Scope.values())
                    valueProperty().bindBidirectional(logService.scope)
                }
            }
            +stackPane {
                vgrow(Priority.ALWAYS)
                +splitPane {
                    addClass(CONTENT_STYLE_CLASS)
                    vgrow(Priority.ALWAYS)
                    +graph
                    +CommitDetailsView()
                }
                +stackPane {
                    addClass(OVERLAY_STYLE_CLASS)
                    visibleWhen(Bindings.isEmpty(graph.items))
                    managedWhen(visibleProperty())
                    +label { text = I18N["commitLog.noCommits"] }
                }
            }
        }

        logService.logListener = indicator
        logService.logErrorHandler = { errorAlert(TinyGit.window, I18N["dialog.cannotFetch.header"], it) }
    }

    private fun filterCommits(value: String?) {
        if (value != null && value.isNotBlank()) filterableCommits.setPredicate {
            it.author.contains(value, true)
                    || it.shortId.contains(value, true)
                    || it.fullMessage.contains(value, true)
        }
        else filterableCommits.setPredicate { true }
    }

    private fun checkoutCommit(commit: Commit) {
        branchService.checkoutCommit(
                commit,
                { errorAlert(TinyGit.window, I18N["dialog.cannotCheckout.header"], I18N["dialog.cannotCheckout.text"]) })
    }

    private fun resetToCommit(commit: Commit) {
        if (!confirmWarningAlert(TinyGit.window, I18N["dialog.resetBranch.header"], I18N["dialog.resetBranch.button"], I18N["dialog.resetBranch.text", commit.shortId])) return
        branchService.reset(commit)
    }

    private fun tagCommit(commit: Commit) {
        textInputDialog(TinyGit.window, I18N["dialog.tag.header"], I18N["dialog.tag.button"], Icons.tag()) {
            tagService.tag(
                    commit,
                    it,
                    { errorAlert(TinyGit.window, I18N["dialog.cannotTag.header"], I18N["dialog.cannotTag.text", it]) })
        }
    }

    /**
     * An indicator to be shown in the toolbar while fetching from remote.
     */
    private class FetchIndicator : HBoxBuilder(), TaskListener {

        private val visible = SimpleBooleanProperty()

        init {
            addClass(INDICATOR_STYLE_CLASS)
            visibleWhen(visible)
            managedWhen(visibleProperty())
            +progressIndicator(0.5).tooltip(I18N["commitLog.fetching"])
        }

        override fun started() = visible.set(true)

        override fun done() = visible.set(false)

    }

}
