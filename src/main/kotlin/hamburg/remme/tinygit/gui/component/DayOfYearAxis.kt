package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.endOfDay
import hamburg.remme.tinygit.monthOfYearFormat
import hamburg.remme.tinygit.noon
import hamburg.remme.tinygit.startOfDay
import hamburg.remme.tinygit.weeksBetween
import javafx.scene.chart.Axis
import java.time.LocalDate
import java.time.Year

class DayOfYearAxis : Axis<LocalDate>() {

    var step: Double = 0.0
        private set
    private lateinit var firstDay: LocalDate
    private lateinit var lastDay: LocalDate
    private var length: Double = 0.0

    override fun autoRange(length: Double): Any {
        this.length = length
        val now = LocalDate.now()
        lastDay = Year.of(now.year).atMonth(now.month).atEndOfMonth()
        firstDay = Year.of(now.year - 1).atMonth(now.month).atDay(1)
        return firstDay to lastDay
    }

    override fun getRange() = firstDay to lastDay

    override fun setRange(range: Any, animate: Boolean) {
        @Suppress("UNCHECKED_CAST")
        range as Pair<LocalDate, LocalDate>
        range.let {
            firstDay = it.first
            lastDay = it.second
        }
        step = length / firstDay.weeksBetween(lastDay)
    }

    override fun getTickMarkLabel(date: LocalDate) = date.format(monthOfYearFormat)!!

    override fun calculateTickValues(length: Double, range: Any) = (0..12).map { firstDay.plusMonths(it.toLong()) }

    override fun getDisplayPosition(date: LocalDate) = firstDay.weeksBetween(date) * step

    override fun getValueForDisplay(displayPosition: Double) = throw UnsupportedOperationException()

    override fun isValueOnAxis(date: LocalDate) = date.noon.isAfter(firstDay.startOfDay) && date.noon.isBefore(lastDay.endOfDay)

    override fun getZeroPosition() = getDisplayPosition(firstDay)

    override fun toNumericValue(date: LocalDate) = getDisplayPosition(date)

    override fun toRealValue(value: Double) = throw UnsupportedOperationException()

}
