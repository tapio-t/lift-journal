package com.example.liftjournal.domain.progress

import com.example.liftjournal.domain.format.roundToOneDecimal
import com.example.liftjournal.domain.stats.AllTimeStats
import com.example.liftjournal.domain.stats.ExerciseTotals
import com.example.liftjournal.domain.stats.YearStats
import java.time.Month

private const val DAYS_TO_THURSDAY = 3L

/**
 * The exercise the exercise chips should show as selected: [requested] while it still has weighted sets
 * in the period, otherwise the one with the most volume (prototype `progressView`).
 */
fun resolveExercise(exercises: List<ExerciseTotals>, requested: String?): ExerciseTotals? =
    exercises.firstOrNull { it.key == requested } ?: exercises.firstOrNull()

/** The chart for one year (SPEC §5.3). [exercise] is ignored for total volume. */
fun yearChart(stats: YearStats, view: ProgressView, exercise: ExerciseTotals?): ProgressChart {
    if (view == ProgressView.TOTAL_VOLUME || view == ProgressView.GOAL) {
        val bars = stats.months.map { MonthBar(Month.of(it.monthIndex + 1), it.totalKg, it.workouts) }
        return ProgressChart.Months(stats.year, stats.isCurrentYear, exerciseName = null, bars = bars)
    }
    if (exercise == null) return ProgressChart.NoExercise(view)
    if (view == ProgressView.EXERCISE_VOLUME) {
        val bars = stats.months.map { month ->
            val volume = month.byExercise[exercise.key]
            MonthBar(Month.of(month.monthIndex + 1), volume?.volumeKg ?: 0.0, volume?.sets ?: 0)
        }
        return ProgressChart.Months(stats.year, stats.isCurrentYear, exercise.name, bars)
    }
    val bars = stats.weeks.map { week ->
        WeekBar(week.monday, week.isoWeek, week.monday.plusDays(DAYS_TO_THURSDAY).month, week.topSets[exercise.key])
    }
    return ProgressChart.Weeks(stats.year, stats.isCurrentYear, exercise.name, bars, exercise.averageSetWeight())
}

/** The all-time chart: one bar per year, oldest first (SPEC §5.4). [exercise] is ignored for total volume. */
fun allTimeChart(stats: AllTimeStats, view: ProgressView, exercise: ExerciseTotals?, currentYear: Int): ProgressChart {
    val averagePerMonth = { volumeKg: Double -> AllTimeSummary.AveragePerMonth(volumeKg / maxOf(1, stats.spanMonths)) }
    if (view == ProgressView.TOTAL_VOLUME || view == ProgressView.GOAL) {
        val bars = stats.years.map { YearBar(it.year, it.averageKgPerMonth, it.months, it.workouts) }
        return ProgressChart.Years(
            YearChartKind.TOTAL_VOLUME,
            exerciseName = null,
            currentYear = currentYear,
            bars = bars,
            summary = averagePerMonth(stats.totalKg),
            averageSetWeight = null,
        )
    }
    if (exercise == null) return ProgressChart.NoExercise(view)
    if (view == ProgressView.EXERCISE_VOLUME) {
        val bars = stats.years.map { year ->
            YearBar(
                year.year,
                year.exerciseAverageKgPerMonth(exercise.key),
                year.months,
                year.byExercise[exercise.key]?.sets ?: 0,
            )
        }
        return ProgressChart.Years(
            YearChartKind.EXERCISE_VOLUME,
            exercise.name,
            currentYear,
            bars,
            averagePerMonth(exercise.volumeKg),
            averageSetWeight = null,
        )
    }
    val bars = stats.years.map { year ->
        val exerciseYear = year.byExercise[exercise.key]
        val average = exerciseYear?.averageWeeklyTopKg ?: 0.0
        YearBar(year.year, roundToOneDecimal(average), year.months, exerciseYear?.weeklyTopsKg?.size ?: 0)
    }
    val allWeeklyTops = stats.years.flatMap { it.byExercise[exercise.key]?.weeklyTopsKg?.values.orEmpty() }
    val averageTop = allWeeklyTops.takeIf { it.isNotEmpty() }?.average()?.let(::roundToOneDecimal)
    val summary = AllTimeSummary.AverageTopSet(averageTop, allWeeklyTops.size)
    return ProgressChart.Years(
        YearChartKind.TOP_SET,
        exercise.name,
        currentYear,
        bars,
        summary,
        exercise.averageSetWeight(),
    )
}

private fun ExerciseTotals.averageSetWeight(): AverageSetWeight? =
    if (weightedSetCount == 0) null else AverageSetWeight(roundToOneDecimal(averageSetWeightKg), weightedSetCount)
