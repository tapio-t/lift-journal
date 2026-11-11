package com.example.liftjournal.domain.stats

import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.parser.ParsedExercise
import com.example.liftjournal.domain.parser.volumeKg
import com.example.liftjournal.domain.time.mondayOfWeek
import java.time.LocalDate

/** An exercise within one year of the all-time view; [weeklyTopsKg] maps each trained week's Monday to its top set. */
data class ExerciseYear(val volumeKg: Double, val sets: Int, val weeklyTopsKg: Map<LocalDate, Double>) {
    /** Average weekly top set over the weeks the exercise was trained (SPEC §5.4). */
    val averageWeeklyTopKg: Double get() = if (weeklyTopsKg.isEmpty()) 0.0 else weeklyTopsKg.values.average()
}

/** One year in the all-time view. [months] counts from the first logged month and up to the current month. */
data class YearTotals(
    val year: Int,
    val months: Int,
    val totalKg: Double,
    val workouts: Int,
    val byExercise: Map<String, ExerciseYear>,
) {
    val averageKgPerMonth: Double get() = totalKg / months

    fun exerciseAverageKgPerMonth(key: String): Double = (byExercise[key]?.volumeKg ?: 0.0) / months
}

/** Everything the all-time views of Progress show: one bar per year, averages only (SPEC §5.4). */
data class AllTimeStats(
    val years: List<YearTotals>,
    val totalKg: Double,
    val workouts: Int,
    val exercises: List<ExerciseTotals>,
) {
    val spanMonths: Int get() = years.sumOf { it.months }

    val averageKgPerMonth: Double get() = totalKg / maxOf(1, spanMonths)
}

/** Builds the all-time view, oldest year first. Future entries are ignored. */
fun allTimeStats(entries: List<JournalEntry>, today: LocalDate, bodyweightKg: Double?): AllTimeStats {
    val pastEntries = entries.filter { !it.date.isAfter(today) }.sortedBy { it.date }
    val firstDate = pastEntries.firstOrNull()?.date ?: today
    val builders = availableYears(entries, today).sorted()
        .associateWith { year -> YearBuilder(year, monthsCounted(year, firstDate, today)) }
    val exercises = ExerciseTotalsCollector(bodyweightKg)
    var totalKg = 0.0
    var workouts = 0

    pastEntries.forEach { entry ->
        val year = builders.getValue(entry.date.year)
        if (entry.isWorkout) {
            year.workouts++
            workouts++
        }
        entry.exercises.forEach { exercise ->
            val volume = exercise.volumeKg(bodyweightKg)
            exercises.add(exercise)
            year.add(exercise, volume, entry.date.mondayOfWeek(), bodyweightKg)
            totalKg += volume
        }
    }

    return AllTimeStats(builders.values.map { it.build() }, totalKg, workouts, exercises.weightedByVolume())
}

/** The first year counts from its first logged month; the current year counts up to the current month. */
private fun monthsCounted(year: Int, firstDate: LocalDate, today: LocalDate): Int {
    val lastMonthIndex = if (year == today.year) today.monthValue - 1 else LAST_MONTH_INDEX
    val firstMonthIndex = if (year == firstDate.year) firstDate.monthValue - 1 else 0
    return maxOf(1, lastMonthIndex - firstMonthIndex + 1)
}

private class YearBuilder(private val year: Int, private val months: Int) {
    var workouts = 0
    private var totalKg = 0.0
    private val volumes = LinkedHashMap<String, Double>()
    private val setCounts = LinkedHashMap<String, Int>()
    private val weeklyTops = LinkedHashMap<String, MutableMap<LocalDate, Double>>()

    fun add(exercise: ParsedExercise, volumeKg: Double, monday: LocalDate, bodyweightKg: Double?) {
        totalKg += volumeKg
        volumes[exercise.key] = (volumes[exercise.key] ?: 0.0) + volumeKg
        setCounts[exercise.key] = (setCounts[exercise.key] ?: 0) + exercise.sets.size
        val tops = weeklyTops.getOrPut(exercise.key) { LinkedHashMap() }
        exercise.countedSetWeights(bodyweightKg).maxOrNull()?.let { heaviest ->
            tops[monday] = maxOf(tops[monday] ?: 0.0, heaviest)
        }
    }

    fun build(): YearTotals {
        val byExercise = volumes.keys.associateWith { key ->
            ExerciseYear(volumes.getValue(key), setCounts.getValue(key), weeklyTops[key].orEmpty().toMap())
        }
        return YearTotals(year, months, totalKg, workouts, byExercise)
    }
}
