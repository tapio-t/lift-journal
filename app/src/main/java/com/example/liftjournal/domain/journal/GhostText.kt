package com.example.liftjournal.domain.journal

import com.example.liftjournal.domain.model.JournalEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Ghost text looks back at most 8 weeks for the same weekday (SPEC §3.4). */
const val GHOST_LOOKBACK_DAYS = 56L

private val HAS_DIGIT = Regex("\\d")

/**
 * The ghost lines for [date]: the most recent earlier entry on the same weekday, within 8 weeks,
 * that has recorded sets. Only lines with a digit are used, so notes are left out. Null when there's none.
 */
fun ghostLinesFor(date: LocalDate, entries: List<JournalEntry>): List<String>? {
    val previous = entries
        .filter { it.date.isBefore(date) && it.date.dayOfWeek == date.dayOfWeek && it.exercises.isNotEmpty() }
        .maxByOrNull { it.date }
        ?: return null
    if (ChronoUnit.DAYS.between(previous.date, date) > GHOST_LOOKBACK_DAYS) return null
    return previous.text.split('\n').map { it.trim() }.filter { HAS_DIGIT.containsMatchIn(it) }.ifEmpty { null }
}

/** One line of the ghost overlay: what's typed (drawn invisible) and the ghost remainder after it. */
data class GhostLine(val typed: String, val remainder: String)

/** Each typed character covers one ghost character in the same line; the rest of the ghost line stays visible. */
fun ghostOverlay(typedText: String, ghostLines: List<String>): List<GhostLine> {
    val typedLines = typedText.split('\n')
    val lineCount = maxOf(typedLines.size, ghostLines.size)
    return (0 until lineCount).map { index ->
        val typed = typedLines.getOrElse(index) { "" }
        val ghost = ghostLines.getOrElse(index) { "" }
        GhostLine(typed, ghost.drop(typed.length))
    }
}

/** Text and cursor position after accepting ghost text. */
data class TextWithCursor(val text: String, val cursor: Int)

/**
 * Tab fills in the rest of the current ghost line when the cursor is at the end of the line and what's
 * typed so far is a case-insensitive prefix of it (prototype `acceptGhost`). Returns null when Tab does nothing.
 */
fun acceptGhostLine(text: String, cursor: Int, ghostLines: List<String>): TextWithCursor? {
    if (ghostLines.isEmpty() || cursor !in 0..text.length) return null
    val before = text.substring(0, cursor)
    val lineIndex = before.count { it == '\n' }
    val lineStart = before.lastIndexOf('\n') + 1
    val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
    val typedLine = text.substring(lineStart, lineEnd)
    val ghostLine = ghostLines.getOrElse(lineIndex) { "" }
    val canAccept = cursor == lineEnd &&
        typedLine.length < ghostLine.length &&
        ghostLine.lowercase().startsWith(typedLine.lowercase())
    if (!canAccept) return null
    val addition = ghostLine.substring(typedLine.length)
    return TextWithCursor(text.substring(0, cursor) + addition + text.substring(cursor), cursor + addition.length)
}
