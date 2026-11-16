package com.example.liftjournal.domain.export

/** One spreadsheet cell. Numbers are written as numbers so spreadsheets can calculate with them (SPEC §6.1). */
sealed interface Cell {
    data class Text(val value: String) : Cell

    data class Number(val value: Double) : Cell

    data object Empty : Cell
}

/** A named sheet. The first row is the header, which writers make bold and frozen. */
data class Sheet(val name: String, val rows: List<List<Cell>>)

/**
 * Builds a row from plain values: strings become text, numbers become numbers and null leaves the cell empty.
 * NaN and infinity aren't valid spreadsheet numbers, so they are left empty too.
 */
fun row(vararg values: Any?): List<Cell> = values.map { value ->
    when (value) {
        null -> Cell.Empty
        is kotlin.Number -> value.toDouble().takeIf { it.isFinite() }?.let { Cell.Number(it) } ?: Cell.Empty
        else -> Cell.Text(value.toString())
    }
}
