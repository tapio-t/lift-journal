package com.example.liftjournal.domain.model

import com.example.liftjournal.domain.parser.ParsedEntry
import com.example.liftjournal.domain.parser.ParsedExercise
import java.time.LocalDate

/** One journal entry. The date is its identity; [parsed] is always derived from [text]. */
data class JournalEntry(val date: LocalDate, val text: String, val parsed: ParsedEntry) {
    val exercises: List<ParsedExercise> get() = parsed.exercises

    /** A workout is a date whose entry has at least one recorded set (SPEC §4). */
    val isWorkout: Boolean get() = exercises.any { it.sets.isNotEmpty() }
}
