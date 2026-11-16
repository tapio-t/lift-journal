package com.example.liftjournal.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/** Weeks start on Monday (ISO 8601). */
fun LocalDate.mondayOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** ISO week number, 1–53. */
val LocalDate.isoWeek: Int get() = get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

/** The year an ISO week belongs to, which differs from the calendar year around New Year. */
val LocalDate.isoWeekYear: Int get() = get(IsoFields.WEEK_BASED_YEAR)

val LocalDate.yearMonth: YearMonth get() = YearMonth.from(this)

private val STORAGE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** Dates are stored as `yyyy-MM-dd` (CLAUDE.md). */
fun LocalDate.toStorageString(): String = format(STORAGE_FORMAT)

/** Parses a stored `yyyy-MM-dd` date, or returns null for anything else. */
fun parseStorageDate(text: String): LocalDate? =
    runCatching { LocalDate.parse(text.trim(), STORAGE_FORMAT) }.getOrNull()

/** A source of "today", so date-dependent code can be tested. */
fun interface TodayProvider {
    fun today(): LocalDate
}

/** The device's current date in its time zone. */
val SystemToday = TodayProvider { LocalDate.now() }
