package com.example.liftjournal.domain

import com.example.liftjournal.domain.model.JournalEntry
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

private val EXERCISES = listOf(
    "squat" to 100.0,
    "bench press" to 80.0,
    "deadlift" to 140.0,
    "overhead press" to 50.0,
    "barbell row" to 70.0,
    "romanian deadlift" to 100.0,
)
private val TRAINING_DAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
private const val SKIP_CHANCE = 0.1
private const val WEIGHT_STEP = 2.5
private const val YEARLY_GAIN = 10.0
private const val DAYS_PER_YEAR = 365.0
private const val EXERCISES_PER_DAY = 3

/**
 * Realistic-looking training data for tests only (CLAUDE.md: no sample-data buttons in the app). Three sessions
 * a week with slowly rising weights, a pull-up line and the odd note. The same [seed] always gives the same data.
 */
fun generateTrainingJournal(from: LocalDate, to: LocalDate, seed: Int = 1): List<JournalEntry> {
    val random = Random(seed)
    return generateSequence(from) { it.plusDays(1) }
        .takeWhile { !it.isAfter(to) }
        .filter { it.dayOfWeek in TRAINING_DAYS && random.nextDouble() > SKIP_CHANCE }
        .map { date -> entry(date.toString(), sessionText(date, from, random)) }
        .toList()
}

private fun sessionText(date: LocalDate, start: LocalDate, random: Random): String {
    val yearsIn = (date.toEpochDay() - start.toEpochDay()) / DAYS_PER_YEAR
    val lines = EXERCISES.shuffled(random).take(EXERCISES_PER_DAY).map { (name, base) ->
        val weight = roundToStep(base + yearsIn * YEARLY_GAIN + random.nextInt(-2, 3) * WEIGHT_STEP)
        "$name $weight x 5, 5, ${random.nextInt(3, 6)}"
    }
    val extras = listOfNotNull(
        "pull-up bw x ${random.nextInt(5, 10)}, ${random.nextInt(4, 9)}".takeIf { random.nextBoolean() },
        "felt good today".takeIf { random.nextInt(5) == 0 },
    )
    return (lines + extras).joinToString("\n")
}

private fun roundToStep(kg: Double): Double = Math.round(kg / WEIGHT_STEP) * WEIGHT_STEP
