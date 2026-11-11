package com.example.liftjournal.domain.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// The app's copy is English, so dates use English names like the prototype.
private val APP_LOCALE: Locale = Locale.ENGLISH
private val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", APP_LOCALE)
private const val DAYS_IN_WEEK_AFTER_MONDAY = 6L

/** `09.11.2026`: the journal date header (SPEC §3.1). */
fun formatDayMonthYear(date: LocalDate): String = date.format(DAY_MONTH_YEAR)

/** `Nov`. */
fun Month.shortName(): String = getDisplayName(TextStyle.SHORT, APP_LOCALE)

/** `November`. */
fun Month.longName(): String = getDisplayName(TextStyle.FULL, APP_LOCALE)

/** `Thu`. */
fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, APP_LOCALE)

/** `Nov 2026`: a month tag in Journals search. */
fun formatMonthTag(month: YearMonth): String = "${month.month.shortName()} ${month.year}"

/** `22 Oct`. */
fun formatShortDate(date: LocalDate): String = "${date.dayOfMonth} ${date.month.shortName()}"

/** `Thu 22 Oct`, with the year added when it isn't the current year (prototype `fmtLong`). */
fun formatLongDate(date: LocalDate, today: LocalDate): String {
    val base = "${date.dayOfWeek.shortName()} ${formatShortDate(date)}"
    return if (date.year == today.year) base else "$base ${date.year}"
}

/** `19–25 Oct`, or `27 Oct – 2 Nov` when the week spans two months (prototype `weekRange`). */
fun formatWeekRange(monday: LocalDate): String {
    val sunday = monday.plusDays(DAYS_IN_WEEK_AFTER_MONDAY)
    if (monday.month == sunday.month) return "${monday.dayOfMonth}–${sunday.dayOfMonth} ${sunday.month.shortName()}"
    return "${formatShortDate(monday)} – ${formatShortDate(sunday)}"
}
