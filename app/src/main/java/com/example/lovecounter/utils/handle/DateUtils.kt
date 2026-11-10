package com.example.lovecounter.utils.handle

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

data class CalendarGridDay(
    val date: Calendar,
    val isCurrentMonth: Boolean
)

object DateUtils {
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    private const val DISPLAY_DATE_PATTERN = "dd/MM/yyyy"
    private const val ISO_DATE_PATTERN = "yyyy-MM-dd"

    fun calculateDaysBetweenDates(startTimestamp: Long, todayTimestamp: Long = System.currentTimeMillis()): Long {
        if (startTimestamp <= 0L) return 0L
        val startDay = localDateAsUtcDay(startTimestamp)
        val todayDay = localDateAsUtcDay(todayTimestamp)
        return (todayDay - startDay).coerceAtLeast(0L)
    }

    fun formatDate(timestamp: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(DISPLAY_DATE_PATTERN, locale).format(Date(timestamp))

    fun parseDate(value: String, locale: Locale = Locale.US): Calendar? {
        val pattern = if (value.contains('-')) ISO_DATE_PATTERN else DISPLAY_DATE_PATTERN
        val formatter = SimpleDateFormat(pattern, locale).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = formatter.parse(value, position)
            ?.takeIf { position.index == value.length }
            ?: return null
        return Calendar.getInstance().apply { timeInMillis = parsed.time }
    }

    fun monthGridDays(month: Calendar): List<CalendarGridDay> {
        val firstDay = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val leadingDays = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cellCount = ((leadingDays + daysInMonth + 6) / 7) * 7

        return (0 until cellCount).map { index ->
            val date = (firstDay.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, index - leadingDays)
            }
            CalendarGridDay(
                date = date,
                isCurrentMonth = date.get(Calendar.YEAR) == firstDay.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == firstDay.get(Calendar.MONTH)
            )
        }
    }

    fun startOfDay(date: Calendar): Calendar =
        (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    fun isWithinDateRange(date: Calendar, minDate: Calendar, maxDate: Calendar): Boolean {
        val day = startOfDay(date)
        return !day.before(startOfDay(minDate)) && !day.after(startOfDay(maxDate))
    }

    fun clampDate(date: Calendar, minDate: Calendar, maxDate: Calendar): Calendar {
        val day = startOfDay(date)
        return when {
            day.before(startOfDay(minDate)) -> startOfDay(minDate)
            day.after(startOfDay(maxDate)) -> startOfDay(maxDate)
            else -> date.clone() as Calendar
        }
    }

    fun dateInYear(date: Calendar, year: Int): Calendar =
        (date.clone() as Calendar).apply {
            val dayOfMonth = get(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.DAY_OF_MONTH, minOf(dayOfMonth, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }

    private fun localDateAsUtcDay(timestamp: Long): Long {
        val localDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val utcDate = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                localDate.get(Calendar.YEAR),
                localDate.get(Calendar.MONTH),
                localDate.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }
        return utcDate.timeInMillis / MILLIS_PER_DAY
    }
}
