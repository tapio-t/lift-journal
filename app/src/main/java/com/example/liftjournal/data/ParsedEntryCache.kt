package com.example.liftjournal.data

import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.parser.EntryParser
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps parsed entries keyed by (date, text) so unchanged entries aren't re-parsed on every
 * database emission. Parsed sets are always re-derivable from the text (SPEC §7).
 */
class ParsedEntryCache {
    private data class Key(val date: LocalDate, val text: String)

    private val parsed = ConcurrentHashMap<Key, JournalEntry>()

    /** Returns parsed entries for [rows] and forgets entries that are no longer present. */
    fun parseAll(rows: List<Pair<LocalDate, String>>): List<JournalEntry> {
        val keys = rows.map { (date, text) -> Key(date, text) }
        val result = keys.map { key ->
            parsed.getOrPut(key) { JournalEntry(key.date, key.text, EntryParser.parse(key.text)) }
        }
        parsed.keys.retainAll(keys.toSet())
        return result
    }
}
