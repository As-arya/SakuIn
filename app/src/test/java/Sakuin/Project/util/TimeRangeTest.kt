package Sakuin.Project.util

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeRangeTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0) =
        ZonedDateTime.of(y, m, d, h, 0, 0, 0, jakarta).toInstant().toEpochMilli()

    @Test
    fun monthRangeCoversWholeMonthInLocalTime() {
        val range = YearMonth.of(2026, 2).toTimeRange(jakarta)
        assertEquals(millis(2026, 2, 1), range.startMillis)
        assertEquals(millis(2026, 3, 1), range.endMillis)
    }

    @Test
    fun rangeIsHalfOpen() {
        val range = YearMonth.of(2026, 10).toTimeRange(jakarta)
        assertTrue(range.startMillis in range)
        assertTrue(millis(2026, 10, 31, 23) in range)
        assertFalse(range.endMillis in range)
    }

    @Test
    fun weekRunsMondayToSunday() {
        // 22 Oct 2026 is a Thursday.
        val range = LocalDate.of(2026, 10, 22).weekTimeRange(jakarta)
        assertEquals(millis(2026, 10, 19), range.startMillis)
        assertEquals(millis(2026, 10, 26), range.endMillis)
    }

    @Test
    fun yearRange() {
        val range = Year.of(2026).toTimeRange(jakarta)
        assertEquals(millis(2026, 1, 1), range.startMillis)
        assertEquals(millis(2027, 1, 1), range.endMillis)
    }

    @Test
    fun monthsRangeIncludesBothEnds() {
        val range = monthsTimeRange(YearMonth.of(2026, 5), YearMonth.of(2026, 10), jakarta)
        assertEquals(millis(2026, 5, 1), range.startMillis)
        assertEquals(millis(2026, 11, 1), range.endMillis)
    }
}
