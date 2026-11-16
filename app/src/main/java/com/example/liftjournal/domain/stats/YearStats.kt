package com.example.liftjournal.domain.stats

import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.parser.ParsedExercise
import com.example.liftjournal.domain.parser.effectiveWeightKg
import com.example.liftjournal.domain.parser.volumeKg
import com.example.liftjournal.domain.time.isoWeek
import com.example.liftjournal.domain.time.mondayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Volume and set count of one exercise within a month. */
data class ExerciseVolume(val volumeKg: Double, val sets: Int)

/** One month of a year view (SPEC §5.3). [monthIndex] is 0 for January. */
data class MonthTotals(
    val monthIndex: Int,
    val totalKg: Double,
    val workouts: Int,
    val byExercise: Map<String, ExerciseVolume>,
)

/** The heaviest set of a week; ties go to more reps. */
data class TopSet(val weightKg: Double, val reps: Int, val date: LocalDate)

/** One ISO week of a year view, with each exercise's top set. */
data class WeekTops(val monday: LocalDate, val isoWeek: Int, val topSets: Map<String, TopSet>)

/** Everything the year views of Progress show (prototype `periodData` for one year). */
data class YearStats(
    val year: Int,
    val isCurrentYear: Boolean,
    val months: List<MonthTotals>,
    val weeks: List<WeekTops>,
    val totalKg: Double,
    val workouts: Int,
    val exercises: List<ExerciseTotals>,
) {
    /** January up to the current month, or all twelve months for past years. */
    val spanMonths: Int get() = months.size
}

private const val DAYS_TO_THURSDAY = 3L

/**
 * Builds the year view: months from January to the current month (or December for past years) and
 * ISO weeks from the week containing 1 January up to the current week. Future entries are ignored.
 */
fun yearStats(entries: List<JournalEntry>, year: Int, today: LocalDate, bodyweightKg: Double?): YearStats {
    val isCurrentYear = year == today.year
    val lastMonthIndex = if (isCurrentYear) today.monthValue - 1 else LAST_MONTH_INDEX
    val firstMonday = LocalDate.of(year, 1, 1).mondayOfWeek()
    val lastMonday = (if (isCurrentYear) today else LocalDate.of(year, 12, 31)).mondayOfWeek()
    val months = Array(lastMonthIndex + 1) { MonthBuilder() }
    val weeks = weekMondays(firstMonday, lastMonday).map { WeekBuilder(it) }
    val exercises = ExerciseTotalsCollector(bodyweightKg)
    var totalKg = 0.0
    var workouts = 0

    entries.filter { it.date.year == year && !it.date.isAfter(today) }.sortedBy { it.date }.forEach { entry ->
        val month = months[entry.date.monthValue - 1]
        val week = weeks.getOrNull(ChronoUnit.WEEKS.between(firstMonday, entry.date.mondayOfWeek()).toInt())
        if (entry.isWorkout) {
            month.workouts++
            workouts++
        }
        entry.exercises.forEach { exercise ->
            val volume = exercise.volumeKg(bodyweightKg)
            exercises.add(exercise)
            month.add(exercise, volume)
            totalKg += volume
            week?.add(exercise, entry.date, bodyweightKg)
        }
    }

    return YearStats(
        year = year,
        isCurrentYear = isCurrentYear,
        months = months.mapIndexed { index, builder -> builder.build(index) },
        weeks = weeks.map { it.build() },
        totalKg = totalKg,
        workouts = workouts,
        exercises = exercises.weightedByVolume(),
    )
}

/** Years that have entries up to today, plus the current year, newest first (SPEC §5.2). */
fun availableYears(entries: List<JournalEntry>, today: LocalDate): List<Int> =
    (entries.filter { !it.date.isAfter(today) }.map { it.date.year } + today.year).distinct().sortedDescending()

internal const val LAST_MONTH_INDEX = 11

private fun weekMondays(first: LocalDate, last: LocalDate): List<LocalDate> =
    generateSequence(first) { it.plusWeeks(1) }.takeWhile { !it.isAfter(last) }.toList()

private class MonthBuilder {
    var totalKg = 0.0
    var workouts = 0
    private val byExercise = LinkedHashMap<String, ExerciseVolume>()

    fun add(exercise: ParsedExercise, volumeKg: Double) {
        totalKg += volumeKg
        val previous = byExercise[exercise.key] ?: ExerciseVolume(0.0, 0)
        byExercise[exercise.key] = ExerciseVolume(previous.volumeKg + volumeKg, previous.sets + exercise.sets.size)
    }

    fun build(monthIndex: Int) = MonthTotals(monthIndex, totalKg, workouts, byExercise.toMap())
}

private class WeekBuilder(private val monday: LocalDate) {
    private val topSets = LinkedHashMap<String, TopSet>()

    fun add(exercise: ParsedExercise, date: LocalDate, bodyweightKg: Double?) {
        exercise.sets.forEach { set ->
            val weight = set.effectiveWeightKg(bodyweightKg)?.takeIf { it > 0 } ?: return@forEach
            val candidate = TopSet(weight, set.reps, date)
            val current = topSets[exercise.key]
            if (current == null || candidate.beats(current)) topSets[exercise.key] = candidate
        }
    }

    // Weeks are labelled by the ISO week of their Thursday, which is the same for every day of the week.
    fun build() = WeekTops(monday, monday.plusDays(DAYS_TO_THURSDAY).isoWeek, topSets.toMap())
}

private fun TopSet.beats(other: TopSet): Boolean =
    weightKg > other.weightKg || (weightKg == other.weightKg && reps > other.reps)
