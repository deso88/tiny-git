package hamburg.remme.tinygit

import javafx.application.Platform
import javafx.beans.binding.IntegerExpression
import javafx.beans.property.IntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.streams.toList

val shortDateFormat = DateTimeFormatter.ofPattern("d. MMM yyyy")!!
val dateFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy")!!
val shortDateTimeFormat = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val dateTimeFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!

fun systemOffset() = ZoneId.systemDefault().rules.getOffset(Instant.now())!!

fun localDateTime(epochSecond: Long) = LocalDateTime.ofEpochSecond(epochSecond, 0, systemOffset())!!

fun LocalDate.atEndOfDay() = atTime(LocalTime.MAX)!!

fun Year.numberOfWeeks(): Int {
    val firstDay = atDay(1)
    val lastDay = atDay(length())
    return if (isLeap) when {
        firstDay.dayOfWeek == DayOfWeek.WEDNESDAY && lastDay.dayOfWeek == DayOfWeek.THURSDAY -> 53
        firstDay.dayOfWeek == DayOfWeek.THURSDAY && lastDay.dayOfWeek == DayOfWeek.FRIDAY -> 53
        else -> 52
    } else when {
        firstDay.dayOfWeek == DayOfWeek.THURSDAY && lastDay.dayOfWeek == DayOfWeek.THURSDAY -> 53
        else -> 52
    }
}

fun printError(message: String) {
    System.err.println(message)
}

fun String.asResource() = TinyGit::class.java.getResource(this).toExternalForm()!!

fun String.asPath() = Paths.get(this)!!

fun String.asFile() = asPath().toFile()!!

fun Path.exists() = Files.exists(this)

fun Path.delete() = Files.delete(this)

fun Path.read() = Files.readAllBytes(this).toString(StandardCharsets.UTF_8)

fun Path.readLines() = Files.lines(this).use { it.toList() }

fun Path.readFirst() = Files.lines(this).use { it.findFirst() }.orElse("")!!

fun Path.write(text: String) = Files.write(this, text.toByteArray())!!

fun String.normalize() = replace('\\', '/')

fun String.shorten() = normalize().split('/').last()

fun String.htmlEncode() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

fun String.htmlEncodeSpaces() = replace(" ", "&nbsp;")

fun String.htmlEncodeAll() = htmlEncode().htmlEncodeSpaces()

fun <T> observableList(vararg items: T) = FXCollections.observableArrayList<T>(*items)!!

fun <T : Comparable<T>> ObservableList<T>.addSorted(items: Collection<T>) = items.forEach { item ->
    val index = indexOfFirst { it < item }
    if (index < 0) add(item) else add(index, item)
}

fun <T> ObservableList<T>.addSorted(items: Collection<T>, comparator: (T, T) -> Int) = items.forEach { item ->
    val index = indexOfFirst { comparator.invoke(it, item) < 0 }
    if (index < 0) add(item) else add(index, item)
}

fun IntegerProperty.inc() = set(get() + 1)

fun IntegerProperty.dec() = set(get() - 1)

fun IntegerExpression.equals0() = isEqualTo(0)!!

fun IntegerExpression.unequals0() = isNotEqualTo(0)!!

fun IntegerExpression.greater0() = greaterThan(0)!!

fun IntegerExpression.greater1() = greaterThan(1)!!

inline fun <T> measureTime(type: String, message: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    val value = block.invoke()
    val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
    val async = if (!Platform.isFxApplicationThread()) "[async]" else ""
    val log = String.format("[%6.3fs] %7s %-18s: %s", totalTime, async, type, message)
    if (totalTime < 1) println(log) else printError(log)
    return value
}
