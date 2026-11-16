package com.example.liftjournal.domain.export

import com.example.liftjournal.domain.format.formatPlainNumber

private const val ALPHABET_SIZE = 26
private const val MIN_COLUMN_WIDTH = 8
private const val MAX_COLUMN_WIDTH = 60
private const val COLUMN_WIDTH_PADDING = 2

/** XML 1.0 forbids most control characters, so they are dropped (prototype `xesc`). */
private val XML_FORBIDDEN_CHARACTERS = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]")

/** Escapes text for XML content and attribute values. */
fun escapeXml(text: String): String = text
    .replace(XML_FORBIDDEN_CHARACTERS, "")
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

/** `A`, `B`, … `Z`, `AA`: the spreadsheet column name for a zero-based [index]. */
fun columnName(index: Int): String {
    var remaining = index + 1
    val name = StringBuilder()
    while (remaining > 0) {
        val letter = (remaining - 1) % ALPHABET_SIZE
        name.insert(0, 'A' + letter)
        remaining = (remaining - 1) / ALPHABET_SIZE
    }
    return name.toString()
}

/** A number as it is written into the file, e.g. `82.5` or `250`. */
fun Cell.Number.xmlValue(): String = formatPlainNumber(value)

/** Column widths in characters: the longest first line in the column plus padding, between 8 and 60. */
fun columnWidths(rows: List<List<Cell>>): List<Int> {
    val columnCount = rows.maxOfOrNull { it.size } ?: 0
    return (0 until columnCount).map { column ->
        val longest = rows.maxOfOrNull { row -> row.getOrNull(column)?.displayLength() ?: 0 } ?: 0
        (longest + COLUMN_WIDTH_PADDING).coerceIn(MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH)
    }
}

private fun Cell.displayLength(): Int = when (this) {
    is Cell.Text -> value.lineSequence().first().length
    is Cell.Number -> xmlValue().length
    Cell.Empty -> 0
}
