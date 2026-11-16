package com.example.liftjournal.domain.journal

import com.example.liftjournal.domain.format.formatDayMonthYear
import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.time.yearMonth
import java.time.YearMonth

private val WHITESPACE_RUN = Regex("\\s+")

/** Months that have entries, newest first, for the search month tags (SPEC §3.2). */
fun monthsWithEntries(entries: List<JournalEntry>): List<YearMonth> =
    entries.filter { it.text.isNotBlank() }.map { it.date.yearMonth }.distinct().sortedDescending()

/**
 * Entries matching a month filter and a query, newest first. Every typed word must appear in the
 * entry's text or its `dd.mm.yyyy` date, ignoring case.
 */
fun searchEntries(entries: List<JournalEntry>, query: String, month: YearMonth?): List<JournalEntry> {
    val words = query.trim().lowercase().split(WHITESPACE_RUN).filter { it.isNotEmpty() }
    return entries
        .filter { it.text.isNotBlank() }
        .filter { month == null || it.date.yearMonth == month }
        .filter { entry ->
            val haystack = "${entry.text} ${formatDayMonthYear(entry.date)}".lowercase()
            words.all { haystack.contains(it) }
        }
        .sortedByDescending { it.date }
}
