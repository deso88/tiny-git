package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView

class FileDiffView : StackPane() {

    private val webView = WebView()

    init {
        children += webView

        webView.isContextMenuEnabled = false

        setContent(null)
    }

    // TODO: solve with observable?
    fun setFile(repository: LocalRepository, file: LocalFile) {
        setContent(when (file.status) {
            LocalFile.Status.ADDED -> LocalGit.diffCached(repository, file)
            LocalFile.Status.CHANGED -> LocalGit.diffCached(repository, file)
            LocalFile.Status.REMOVED -> LocalGit.diffCached(repository, file)
            LocalFile.Status.MODIFIED -> LocalGit.diff(repository, file)
            LocalFile.Status.UNTRACKED -> LocalGit.diff(repository, file)
        }.diff)
        webView.childrenUnmodifiable.forEach { println(it) }
    }

    // TODO: solve with observable?
    fun clearFile() {
        setContent(null)
    }

    private fun setContent(diff: String? = "") {
        webView.engine.loadContent("""
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        background-color: #3c3f41;
                        color: #ccc;
                        font: 12px "Liberation Mono", monospace;
                    }
                    hr {
                        height: 1px;
                        background-color: #aaa;
                        border: none;
                    }
                    .line-list {
                        position: absolute;
                        min-width: 100%;
                        font-size: 13px;
                    }
                    .line-number {
                        padding: 3px 6px;
                        text-align: right;
                        color: rgba(255,255,255,0.6);
                        background-color: #535759;
                    }
                    .line-number.header {
                        padding: 6px 0;
                        background-color: #4e6e80;
                    }
                    .line-number.added {
                        background-color: #4e8054;
                    }
                    .line-number.removed {
                        background-color: #804e4e;
                    }
                    .code {
                        width: 100%;
                        white-space: nowrap;
                    }
                    .code.header {
                        color: #aaa;
                        background-color: #354b57;
                    }
                    .code.added {
                        background-color: #36593b;
                    }
                    .code.removed {
                        background-color: #593636;
                    }
                    .code.eof {
                        color: rgba(255,255,255,0.6);
                    }
                    .marker {
                        margin-left: 4px;
                        padding: 0 2px;
                        color: rgba(255,255,255,0.45);
                        background-color: rgba(255,255,255,0.15);
                        border-radius: 2px;
                        font-size: 11px;
                    }
                </style>
            </head>
            <body>
                <table class="line-list" cellpadding="0" cellspacing="0">
                    ${format(diff)}
                </table>
            </body>
            </html>
        """)
    }

    private fun format(diff: String?): String {
        if (diff == null) {
            return ""
        } else if (diff.isBlank() || diff.contains("Binary files differ")) {
            return """
                <tr>
                    <td class="line-number header">&nbsp;</td>
                    <td class="line-number header">&nbsp;</td>
                    <td class="code header">&nbsp;@@ No changes detected or binary file @@</td>
                </tr>
            """
        }
        val blocks = mutableListOf<DiffBlock>()
        var blockNumber = -1
        val numbers = arrayOf(0, 0)
        return diff.replace("\r\n", "\$CR$\n")
                .replace("\n", "\$LF$\n")
                .split("\\r?\\n".toRegex())
                .dropLast(1)
                .dropWhile { !it.isBlockHeader() }
                .onEach { if (it.isBlockHeader()) blocks += parseBlockHeader(it) }
                .map { it.replace("&", "&amp;") }
                .map { it.replace("<", "&lt;") }
                .map { it.replace(">", "&gt;") }
                .map { it.replace(" ", "&nbsp;") }
                .map {
                    if (it.isBlockHeader()) {
                        blockNumber++
                        numbers[0] = blocks[blockNumber].number1
                        numbers[1] = blocks[blockNumber].number2
                    }
                    formatLine(it, numbers, blocks[blockNumber])
                }
                .joinToString("")
    }

    private fun String.isBlockHeader() = this.startsWith("@@")

    private fun parseBlockHeader(line: String): DiffBlock {
        val match = ".*?(\\d+,\\d+).*?(\\d+,\\d+).*".toRegex().matchEntire(line)!!.groups
        return DiffBlock(
                match[1]!!.value.substringBefore(",").toInt(),
                match[1]!!.value.substringAfter(",").toInt(),
                match[2]!!.value.substringBefore(",").toInt(),
                match[2]!!.value.substringAfter(",").toInt())
    }

    private fun formatLine(line: String, numbers: Array<Int>, block: DiffBlock): String {
        if (line.isBlockHeader()) {
            return """
                <tr>
                    <td class="line-number header">&nbsp;</td>
                    <td class="line-number header">&nbsp;</td>
                    <td class="code header">&nbsp;@@ -${block.number1},${block.length1} +${block.number2},${block.length2} @@</td>
                </tr>
            """
        }
        val code: String
        val codeClass: String
        val oldLineNumber: String
        val newLineNumber: String
        when {
            line.startsWith("+") -> {
                newLineNumber = numbers[1]++.toString()
                oldLineNumber = "&nbsp;"
                code = line.replaceMarkers()
                codeClass = "added"
            }
            line.startsWith("-") -> {
                newLineNumber = "&nbsp;"
                oldLineNumber = numbers[0]++.toString()
                code = line.replaceMarkers()
                codeClass = "removed"
            }
            line.startsWith("\\") -> {
                newLineNumber = "&nbsp;"
                oldLineNumber = "&nbsp;"
                code = line.stripMarkers()
                codeClass = "eof"
            }
            else -> {
                oldLineNumber = numbers[0]++.toString()
                newLineNumber = numbers[1]++.toString()
                code = line.stripMarkers()
                codeClass = "&nbsp;"
            }
        }
        return """
            <tr>
                <td class="line-number $codeClass">$oldLineNumber</td>
                <td class="line-number $codeClass">$newLineNumber</td>
                <td class="code $codeClass">$code</td>
            </tr>
        """
    }

    private fun String.replaceMarkers() =
            this.replace("\\\$CR\\$\\\$LF\\$$".toRegex(), "<span class=\"marker\">&#92;r&#92;n</span>")
                    .replace("\\\$LF\\$$".toRegex(), "<span class=\"marker\">&#92;n</span>")

    private fun String.stripMarkers() = this.replace("(\\\$CR\\$)?\\\$LF\\$$".toRegex(), "")

    private class DiffBlock(val number1: Int, val length1: Int, val number2: Int, val length2: Int)

}
