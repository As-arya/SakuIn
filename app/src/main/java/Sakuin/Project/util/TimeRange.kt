package Sakuin.Project.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Half-open time range [startMillis, endMillis) in epoch millis. */
data class TimeRange(val startMillis: Long, val endMillis: Long) {
    init {
        require(endMillis >= startMillis) { "end must not be before start" }
    }

    operator fun contains(millis: Long): Boolean = millis >= startMillis && millis < endMillis
}

private fun LocalDate.startMillis(zone: ZoneId) = atStartOfDay(zone).toInstant().toEpochMilli()

fun YearMonth.toTimeRange(zone: ZoneId = ZoneId.systemDefault()) =
    TimeRange(atDay(1).startMillis(zone), plusMonths(1).atDay(1).startMillis(zone))

fun Year.toTimeRange(zone: ZoneId = ZoneId.systemDefault()) =
    TimeRange(atDay(1).startMillis(zone), plusYears(1).atDay(1).startMillis(zone))

/** Monday-to-Sunday week containing this date. */
fun LocalDate.weekTimeRange(zone: ZoneId = ZoneId.systemDefault()): TimeRange {
    val monday = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return TimeRange(monday.startMillis(zone), monday.plusWeeks(1).startMillis(zone))
}

/** From the start of [first] up to the end of [last], inclusive of both months. */
fun monthsTimeRange(first: YearMonth, last: YearMonth, zone: ZoneId = ZoneId.systemDefault()) =
    TimeRange(first.toTimeRange(zone).startMillis, last.toTimeRange(zone).endMillis)
