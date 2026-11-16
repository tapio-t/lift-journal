package com.example.liftjournal.domain.stats

import com.example.liftjournal.domain.model.JournalEntry

/** An exercise the user has logged, with how many entries contain it. */
data class KnownExercise(val key: String, val name: String, val entryCount: Int)

/** All logged exercises, most frequent first, then by name (goal form exercise list). */
fun knownExercises(entries: List<JournalEntry>): List<KnownExercise> {
    val byKey = LinkedHashMap<String, KnownExercise>()
    entries.sortedBy { it.date }.forEach { entry ->
        entry.exercises.forEach { exercise ->
            val previous = byKey[exercise.key]
            byKey[exercise.key] = previous?.copy(entryCount = previous.entryCount + 1)
                ?: KnownExercise(exercise.key, exercise.name, 1)
        }
    }
    return byKey.values.sortedWith(compareByDescending<KnownExercise> { it.entryCount }.thenBy { it.name })
}

/** The display name for a normalised exercise key: its first spelling, or the key capitalised. */
fun exerciseDisplayName(key: String, entries: List<JournalEntry>): String =
    entries.sortedBy { it.date }.firstNotNullOfOrNull { entry -> entry.exercises.firstOrNull { it.key == key }?.name }
        ?: key.replaceFirstChar { it.uppercase() }
