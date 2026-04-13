package com.moneynote.app.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val vietnameseLocale = Locale("vi", "VN")

    fun formatDate(date: Long): String = formatter("dd/MM/yyyy (EEE)").format(Date(date))

    fun formatMonth(date: Long): String = formatter("MM/yyyy").format(Date(date))

    fun formatTime(date: Long): String = formatter("HH:mm").format(Date(date))

    fun dayBounds(date: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    fun monthBounds(date: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return start to cal.timeInMillis
    }

    fun weekBounds(date: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val deltaToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        cal.add(Calendar.DAY_OF_MONTH, deltaToMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 7)
        cal.add(Calendar.MILLISECOND, -1)
        return start to cal.timeInMillis
    }

    fun shiftDay(date: Long, delta: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.add(Calendar.DAY_OF_MONTH, delta)
        return cal.timeInMillis
    }

    fun shiftMonth(date: Long, delta: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.add(Calendar.MONTH, delta)
        return cal.timeInMillis
    }

    fun dayOfMonth(date: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun buildDayInMonth(monthDate: Long, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = monthDate
        cal.set(Calendar.DAY_OF_MONTH, day)
        return cal.timeInMillis
    }

    fun monthGrid(monthDate: Long): List<Int?> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = monthDate
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstWeekday = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) // Monday first
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<Int?>()
        repeat(firstWeekday) { cells.add(null) }
        for (d in 1..daysInMonth) cells.add(d)
        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    private fun formatter(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, vietnameseLocale)
    }
}
