package com.example.liftjournal.domain

import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.parser.EntryParser
import java.time.LocalDate

/** Builds a parsed entry from an ISO date and text, for tests. */
fun entry(date: String, text: String): JournalEntry {
    val day = LocalDate.parse(date)
    return JournalEntry(day, text, EntryParser.parse(text))
}

fun date(iso: String): LocalDate = LocalDate.parse(iso)
