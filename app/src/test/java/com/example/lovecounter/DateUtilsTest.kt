package com.example.lovecounter

import com.example.lovecounter.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class DateUtilsTest {

    @Test
    fun monthGridUsesFourFiveOrSixCompleteWeeks() {
        assertEquals(28, DateUtils.monthGridDays(date(2015, Calendar.FEBRUARY, 1)).size)
        assertEquals(35, DateUtils.monthGridDays(date(2024, Calendar.FEBRUARY, 1)).size)
        assertEquals(35, DateUtils.monthGridDays(date(2024, Calendar.APRIL, 1)).size)
        assertEquals(42, DateUtils.monthGridDays(date(2022, Calendar.JULY, 1)).size)
    }

    @Test
    fun monthGridKeepsWeekdayOrderAndIncludesMonthEnd() {
        val february = DateUtils.monthGridDays(date(2015, Calendar.FEBRUARY, 1))
        assertEquals(Calendar.SUNDAY, february.first().date.get(Calendar.DAY_OF_WEEK))
        assertEquals(1, february.first().date.get(Calendar.DAY_OF_MONTH))
        assertTrue(february.first().isCurrentMonth)
        assertEquals(28, february.last().date.get(Calendar.DAY_OF_MONTH))
        assertTrue(february.last().isCurrentMonth)

        val sixWeekMonth = DateUtils.monthGridDays(date(2022, Calendar.JULY, 1))
        assertEquals(31, sixWeekMonth.single { it.isCurrentMonth && it.date.get(Calendar.DAY_OF_MONTH) == 31 }
            .date.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SUNDAY, sixWeekMonth.first().date.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun monthGridSupportsLeapYearsAndAdjacentMonthCells() {
        val grid = DateUtils.monthGridDays(date(2024, Calendar.FEBRUARY, 1))
        assertTrue(grid.any { it.isCurrentMonth && it.date.get(Calendar.DAY_OF_MONTH) == 29 })
        assertTrue(grid.any { !it.isCurrentMonth && it.date.get(Calendar.MONTH) == Calendar.JANUARY })
        assertTrue(grid.any { !it.isCurrentMonth && it.date.get(Calendar.MONTH) == Calendar.MARCH })
    }

    @Test
    fun changingYearClampsLeapDayAndDateRange() {
        val leapDay = date(2024, Calendar.FEBRUARY, 29)
        assertDateEquals(date(2025, Calendar.FEBRUARY, 28), DateUtils.dateInYear(leapDay, 2025))

        val minDate = date(2024, Calendar.FEBRUARY, 15)
        val maxDate = date(2025, Calendar.MARCH, 10)
        assertFalse(DateUtils.isWithinDateRange(date(2024, Calendar.FEBRUARY, 14), minDate, maxDate))
        assertTrue(DateUtils.isWithinDateRange(minDate, minDate, maxDate))
        assertTrue(DateUtils.isWithinDateRange(maxDate, minDate, maxDate))
        assertFalse(DateUtils.isWithinDateRange(date(2025, Calendar.MARCH, 11), minDate, maxDate))
        assertDateEquals(minDate, DateUtils.clampDate(date(2024, Calendar.JANUARY, 1), minDate, maxDate))
        assertDateEquals(maxDate, DateUtils.clampDate(date(2025, Calendar.MARCH, 11), minDate, maxDate))
    }

    @Test
    fun dateFormattingAndParsingKeepExistingStoredFormats() {
        val timestamp = date(2024, Calendar.MARCH, 5).timeInMillis
        assertEquals("05/03/2024", DateUtils.formatDate(timestamp, Locale.US))
        assertDateEquals(date(2024, Calendar.MARCH, 5), DateUtils.parseDate("05/03/2024"))
        assertDateEquals(date(2024, Calendar.MARCH, 5), DateUtils.parseDate("5/3/2024"))
        assertDateEquals(date(2024, Calendar.MARCH, 5), DateUtils.parseDate("2024-03-05"))
        assertNull(DateUtils.parseDate("31/02/2024"))
        assertNull(DateUtils.parseDate("05/03/2024 trailing"))
        assertNull(DateUtils.parseDate(""))
    }

    @Test
    fun legacySlashAndIsoDatesRemainParsableForProfile() {
        assertNotNull(DateUtils.parseDate("11/09/1995"))
        assertNotNull(DateUtils.parseDate("1995-09-11"))
    }

    private fun assertDateEquals(expected: Calendar, actual: Calendar?) {
        assertNotNull(actual)
        val parsed = actual ?: throw AssertionError("Expected a parsed date")
        assertEquals(expected.get(Calendar.YEAR), parsed.get(Calendar.YEAR))
        assertEquals(expected.get(Calendar.MONTH), parsed.get(Calendar.MONTH))
        assertEquals(expected.get(Calendar.DAY_OF_MONTH), parsed.get(Calendar.DAY_OF_MONTH))
    }

    private fun date(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day)
        }
}
