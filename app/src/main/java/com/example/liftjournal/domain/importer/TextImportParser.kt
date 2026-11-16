package com.example.liftjournal.domain.importer

import com.example.liftjournal.domain.backup.mergeEntriesByDate
import com.example.liftjournal.domain.model.StoredEntry
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.DateTimeException
import java.time.LocalDate

/** Entries read from a text file, plus the non-blank lines before the first date, which are ignored. */
data class TextImport(val entries: List<StoredEntry>, val ignoredLines: List<String>)

private const val TWO_DIGIT_YEAR_BASE = 2000
private val BYTE_ORDER_MARK = Char(0xFEFF)
private val WINDOWS_1252: Charset = Charset.forName("windows-1252")

/** English and Finnish weekday names and abbreviations that may come before a date (PLAN phase 7). */
private val WEEKDAY_WORDS = listOf(
    "mon", "monday", "tue", "tues", "tuesday", "wed", "wednesday", "thu", "thur", "thurs", "thursday",
    "fri", "friday", "sat", "saturday", "sun", "sunday",
    "ma", "ti", "ke", "to", "pe", "la", "su",
    "maanantai", "tiistai", "keskiviikko", "torstai", "perjantai", "lauantai", "sunnuntai",
)

private val WEEKDAY_PREFIX = Regex("""^(\p{L}+)[.,]?\s+""")
private val DAY_MONTH_YEAR = Regex("""^(\d{1,2})\.(\d{1,2})\.(\d{4}|\d{2})$""")
private val ISO_DATE = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

/**
 * Splits a text file into entries (PLAN phase 7): a line that is only a date, optionally after a weekday word,
 * starts an entry, and every line up to the next date line is its text. Dates that appear twice are merged.
 */
fun parseImportText(text: String): TextImport {
    val ignored = mutableListOf<String>()
    val entries = mutableListOf<StoredEntry>()
    var currentDate: LocalDate? = null
    val currentLines = mutableListOf<String>()

    fun finishEntry() {
        val date = currentDate ?: return
        entries += StoredEntry(date, currentLines.joinToString("\n").trim())
        currentLines.clear()
    }

    text.trimStart(BYTE_ORDER_MARK).lines().forEach { line ->
        val date = parseDateLine(line)
        when {
            date != null -> {
                finishEntry()
                currentDate = date
            }
            currentDate != null -> currentLines += line.trimEnd()
            line.isNotBlank() -> ignored += line.trim()
        }
    }
    finishEntry()
    return TextImport(mergeEntriesByDate(entries), ignored)
}

/** The date on a date-only line such as `9.11.2026`, `ma 9.11.26` or `2026-11-09`, or null for any other line. */
fun parseDateLine(line: String): LocalDate? {
    val trimmed = line.trim()
    val weekday = WEEKDAY_PREFIX.find(trimmed)
    val datePart = if (weekday != null && weekday.groupValues[1].lowercase() in WEEKDAY_WORDS) {
        trimmed.substring(weekday.range.last + 1)
    } else {
        trimmed
    }
    return parseDayMonthYear(datePart) ?: parseIsoDate(datePart)
}

private fun parseDayMonthYear(text: String): LocalDate? {
    val match = DAY_MONTH_YEAR.matchEntire(text) ?: return null
    val (day, month, year) = match.destructured
    val fullYear = if (year.length == 2) TWO_DIGIT_YEAR_BASE + year.toInt() else year.toInt()
    return dateOrNull(fullYear, month.toInt(), day.toInt())
}

private fun parseIsoDate(text: String): LocalDate? {
    val match = ISO_DATE.matchEntire(text) ?: return null
    val (year, month, day) = match.destructured
    return dateOrNull(year.toInt(), month.toInt(), day.toInt())
}

/** `31.2.2026` looks like a date but isn't one; such a line stays part of the entry text. */
private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? = try {
    LocalDate.of(year, month, day)
} catch (_: DateTimeException) {
    null
}

/** Reads file bytes as UTF-8 (with or without a byte order mark), falling back to Windows-1252. */
fun decodeImportBytes(bytes: ByteArray): String = try {
    Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()
} catch (_: CharacterCodingException) {
    String(bytes, WINDOWS_1252)
}
