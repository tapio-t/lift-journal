package com.example.liftjournal.domain.parser

/**
 * Parses journal text into exercises and sets (SPEC §4, prototype `parseText`).
 *
 * Examples: `deadlift 50 x 5, 5, 5` · `squat 100 x 5, 5, 110 x 3` · `pull-up bw x 8` ·
 * `squat (60 x 10), 100 x 5` (the part in parentheses is not recorded).
 * Exercises can be split by line or by comma; a line with only reps or `weight x reps`
 * continues the previous exercise. Lines without digits are notes.
 */
object EntryParser {
    /** Parses [text]. Never throws: anything unreadable ends up in [ParsedEntry.errors]. */
    fun parse(text: String): ParsedEntry {
        val session = ParseSession()
        text.split('\n').forEach(session::readLine)
        return session.result()
    }
}

// A weight is 80 · 72.5 · bw · 0.5 bw · 1/2 bw.
private const val WEIGHT = """((?:\d+(?:\.\d+)?|\d+\s*/\s*\d+)?\s*bw|\d+(?:\.\d+)?)"""
private const val TIMES_REPS = """\s*(?:kg)?\s*[x×*]\s*(\d+)$"""

/** `80 x 5`: weight and reps for the current exercise. */
private val WEIGHT_AND_REPS = Regex("^$WEIGHT$TIMES_REPS", RegexOption.IGNORE_CASE)

/** `squat 80 x 5`: a name, then weight and reps. */
private val NAME_WEIGHT_AND_REPS = Regex("^(.+?)[\\s:–-]+$WEIGHT$TIMES_REPS", RegexOption.IGNORE_CASE)

/** `5`: reps at the previous weight. */
private val REPS_ONLY = Regex("^(\\d+)$")

/** A bare exercise name, e.g. `squat` before `(60 x 10), 100 x 5` on the same line. */
private val NAME_ONLY = Regex("^[a-zäöåü][a-zäöåü .'/-]*$", RegexOption.IGNORE_CASE)
private val HAS_LETTER = Regex("[a-zäöåü]", RegexOption.IGNORE_CASE)
private val HAS_DIGIT = Regex("\\d")
private val HAS_PARENTHESIS = Regex("[()]")

// Anything in parentheses is not recorded; an unclosed "(" hides the rest of the line.
private val PARENTHESISED = Regex("\\([^)]*\\)?")
private val TOKEN_SEPARATORS = Regex("[,;]+")
private val WHITESPACE_RUN = Regex("\\s+")
private val TRAILING_NAME_SEPARATORS = Regex("[\\s:–-]+$")
private const val NO_BREAK_SPACE = ' '

/** Builds the exercise list while reading lines in order. Lives only for one [EntryParser.parse] call. */
private class ParseSession {
    private val exercisesByKey = LinkedHashMap<String, MutableExercise>()
    private val errors = mutableListOf<String>()
    private val notes = mutableListOf<String>()
    private var current: MutableExercise? = null
    private var lastWeight: WrittenWeight? = null
    private var pendingName: String? = null

    fun readLine(raw: String) {
        val original = raw.replace(NO_BREAK_SPACE, ' ').trim()
        if (original.isEmpty()) return
        val line = original.replace(PARENTHESISED, " ").replace(")", " ").trim()
        if (line.isEmpty()) return
        if (!HAS_DIGIT.containsMatchIn(line)) {
            readLineWithoutDigits(original, line)
            return
        }
        line.split(TOKEN_SEPARATORS)
            .map { it.trim().replace(WHITESPACE_RUN, " ") }
            .filter { it.isNotEmpty() }
            .forEach(::readToken)
    }

    fun result() = ParsedEntry(
        exercises = exercisesByKey.values.filter {
            it.sets.isNotEmpty()
        }.map { ParsedExercise(it.name, it.sets.toList()) },
        errors = errors.toList(),
        notes = notes.toList(),
    )

    /** "(warm-up) squat" names the exercise for the next line; any other text is a note. */
    private fun readLineWithoutDigits(original: String, line: String) {
        current = null
        val namesNextExercise = HAS_PARENTHESIS.containsMatchIn(original) && NAME_ONLY.matches(line)
        if (namesNextExercise) {
            pendingName = cleanName(line)
        } else {
            notes += original
            pendingName = null
        }
    }

    private fun readToken(token: String) {
        WEIGHT_AND_REPS.find(token)?.let { return readWeightAndReps(token, it) }
        val named = NAME_WEIGHT_AND_REPS.find(token)
        if (named != null && HAS_LETTER.containsMatchIn(named.groupValues[1])) return readNamedSet(token, named)
        REPS_ONLY.find(token)?.let { return readRepsOnly(token, it) }
        if (NAME_ONLY.matches(token)) {
            pendingName = cleanName(token)
            current = null
            return
        }
        errors += token
    }

    private fun readWeightAndReps(token: String, match: MatchResult) {
        val pending = pendingName
        if (current == null && pending != null) {
            current = exerciseNamed(pending)
            pendingName = null
        }
        val exercise = current ?: return run { errors += token }
        val reps = match.groupValues[2].toIntOrNull() ?: return run { errors += token }
        val weight = parseWrittenWeight(match.groupValues[1])
        lastWeight = weight
        exercise.sets += weight.toSet(reps)
    }

    private fun readNamedSet(token: String, match: MatchResult) {
        val reps = match.groupValues[3].toIntOrNull() ?: return run { errors += token }
        val exercise = exerciseNamed(cleanName(match.groupValues[1]))
        val weight = parseWrittenWeight(match.groupValues[2])
        current = exercise
        pendingName = null
        lastWeight = weight
        exercise.sets += weight.toSet(reps)
    }

    private fun readRepsOnly(token: String, match: MatchResult) {
        val exercise = current
        val weight = lastWeight
        val reps = match.groupValues[1].toIntOrNull()
        if (exercise == null || weight == null || reps == null) {
            errors += token
            return
        }
        exercise.sets += weight.toSet(reps)
    }

    private fun exerciseNamed(name: String): MutableExercise =
        exercisesByKey.getOrPut(normalizeExerciseName(name)) { MutableExercise(name) }

    private fun cleanName(raw: String): String = raw.replace(TRAILING_NAME_SEPARATORS, "")
        .replace(WHITESPACE_RUN, " ")
        .trim()
        .replaceFirstChar { it.uppercase() }
}

private class MutableExercise(val name: String) {
    val sets = mutableListOf<ExerciseSet>()
}
