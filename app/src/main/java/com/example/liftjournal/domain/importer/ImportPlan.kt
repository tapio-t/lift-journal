package com.example.liftjournal.domain.importer

import com.example.liftjournal.domain.journal.appendEntryText
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.parser.EntryParser
import java.time.LocalDate

/** What to do with an imported date that already has an entry (PLAN phase 7). */
enum class ImportConflictMode {
    /** Add the imported text below the existing text. The default. */
    APPEND,

    /** Leave dates that already have an entry as they are. */
    SKIP_EXISTING,
}

/**
 * The writes an import makes. [entriesToWrite] holds each changed entry's full new text. Nothing is ever deleted.
 */
data class ImportPlan(
    val entriesToWrite: List<StoredEntry>,
    val newDates: List<LocalDate>,
    val appendedDates: List<LocalDate>,
    val unchangedDates: List<LocalDate>,
)

/**
 * Works out the import against the current entries ([existing] text by date). An entry whose text is already
 * contained in the existing entry is skipped, so importing the same file again changes nothing.
 */
fun planImport(imported: List<StoredEntry>, existing: Map<LocalDate, String>, mode: ImportConflictMode): ImportPlan {
    val toWrite = mutableListOf<StoredEntry>()
    val newDates = mutableListOf<LocalDate>()
    val appended = mutableListOf<LocalDate>()
    val unchanged = mutableListOf<LocalDate>()
    imported.forEach { entry ->
        val current = existing[entry.date]
        when {
            current == null -> {
                toWrite += entry
                newDates += entry.date
            }
            mode == ImportConflictMode.SKIP_EXISTING || current.contains(entry.text.trim()) -> unchanged += entry.date
            else -> {
                toWrite += StoredEntry(entry.date, appendEntryText(current, entry.text))
                appended += entry.date
            }
        }
    }
    return ImportPlan(toWrite, newDates, appended, unchanged)
}

/** What the preview shows before importing (PLAN phase 7). */
data class ImportPreview(
    val entryCount: Int,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val datesWithEntries: List<LocalDate>,
    val unreadableParts: Int,
    val ignoredLines: List<String>,
    val plan: ImportPlan,
)

/**
 * Summarises an import. [ImportPreview.unreadableParts] counts lines without sets (kept as notes) plus tokens the
 * parser couldn't read; both stay in the entry text.
 */
fun previewImport(textImport: TextImport, existing: Map<LocalDate, String>, mode: ImportConflictMode): ImportPreview {
    val entries = textImport.entries
    return ImportPreview(
        entryCount = entries.size,
        firstDate = entries.minOfOrNull { it.date },
        lastDate = entries.maxOfOrNull { it.date },
        datesWithEntries = entries.map { it.date }.filter { it in existing },
        unreadableParts = entries.sumOf { entry ->
            val parsed = EntryParser.parse(entry.text)
            parsed.notes.size + parsed.errors.size
        },
        ignoredLines = textImport.ignoredLines,
        plan = planImport(entries, existing, mode),
    )
}
