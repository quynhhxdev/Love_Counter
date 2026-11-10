package com.example.lovecounter

import com.example.lovecounter.utils.handle.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class StartDateCalculationTest {

    @Test
    fun calculateDaysUsesCalendarDatesAndClampsFutureDates() {
        assertEquals(0L, daysBetween(date(2026, Calendar.NOVEMBER, 4), date(2026, Calendar.NOVEMBER, 4)))
        assertEquals(1L, daysBetween(date(2026, Calendar.NOVEMBER, 3), date(2026, Calendar.NOVEMBER, 4)))
        assertEquals(3L, daysBetween(date(2026, Calendar.NOVEMBER, 1), date(2026, Calendar.NOVEMBER, 4)))
        assertEquals(4L, daysBetween(date(2026, Calendar.OCTOBER, 31), date(2026, Calendar.NOVEMBER, 4)))
        assertEquals(1L, daysBetween(date(2024, Calendar.FEBRUARY, 28), date(2024, Calendar.FEBRUARY, 29)))
        assertEquals(2L, daysBetween(date(2024, Calendar.FEBRUARY, 28), date(2024, Calendar.MARCH, 1)))
        assertEquals(1L, daysBetween(date(2025, Calendar.DECEMBER, 31), date(2026, Calendar.JANUARY, 1)))
        assertEquals(0L, daysBetween(date(2026, Calendar.NOVEMBER, 5), date(2026, Calendar.NOVEMBER, 4)))
        assertEquals(0L, DateUtils.calculateDaysBetweenDates(0L, date(2026, Calendar.NOVEMBER, 4)))
    }

    private fun daysBetween(start: Long, today: Long): Long =
        DateUtils.calculateDaysBetweenDates(start, today)

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis
}
