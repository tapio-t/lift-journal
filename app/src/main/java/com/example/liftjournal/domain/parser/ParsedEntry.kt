package com.example.liftjournal.domain.parser

/**
 * One recorded set. A bodyweight set has [weightKg] = 0 and a [bodyweightMultiplier]
 * (1.0 for `bw`, 0.5 for `0.5 bw`); its real weight depends on the bodyweight setting.
 */
data class ExerciseSet(val weightKg: Double, val reps: Int, val bodyweightMultiplier: Double? = null) {
    val isBodyweight: Boolean get() = bodyweightMultiplier != null
}

/** An exercise with its sets, named with the first spelling seen in the entry. */
data class ParsedExercise(val name: String, val sets: List<ExerciseSet>) {
    /** Case- and space-insensitive identity used to group the same exercise across entries. */
    val key: String get() = normalizeExerciseName(name)
}

/** Result of parsing an entry's text (SPEC §4). [errors] are unreadable tokens; [notes] are lines without digits. */
data class ParsedEntry(val exercises: List<ParsedExercise>, val errors: List<String>, val notes: List<String>) {
    companion object {
        val EMPTY = ParsedEntry(emptyList(), emptyList(), emptyList())
    }
}

private val WHITESPACE_RUN = Regex("\\s+")

/** Lower-cases a name and collapses repeated spaces, so "Bench  Press" and "bench press" match. */
fun normalizeExerciseName(name: String): String = name.lowercase().replace(WHITESPACE_RUN, " ").trim()
