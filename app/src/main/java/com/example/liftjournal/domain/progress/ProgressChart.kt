package com.example.liftjournal.domain.progress

import com.example.liftjournal.domain.stats.TopSet
import java.time.LocalDate
import java.time.Month

/** The charts in the Progress tag row (SPEC §5.1). */
enum class ProgressView {
    GOAL,
    TOTAL_VOLUME,
    TOP_SET,
    EXERCISE_VOLUME,
    ;

    /** Top set and exercise volume are about one exercise and show exercise chips. */
    val needsExercise: Boolean get() = this == TOP_SET || this == EXERCISE_VOLUME
}

/** One year, or all-time (SPEC §5.2). */
sealed interface ProgressPeriod {
    data object AllTime : ProgressPeriod

    data class OneYear(val year: Int) : ProgressPeriod
}

/** Mean weight of all counted sets of an exercise in the period (SPEC §5.3 "Average set weight"). */
data class AverageSetWeight(val kg: Double, val sets: Int)

/** What a Progress chart shows. Every variant except [NoExercise] has one value per bar. */
sealed interface ProgressChart {
    /** The view needs an exercise, but none has a weighted set in the period. */
    data class NoExercise(val view: ProgressView) : ProgressChart

    /**
     * Volume per month of one year. [exerciseName] is null for total volume.
     * [count] per bar is workouts (total volume) or sets (exercise volume).
     */
    data class Months(val year: Int, val isCurrentYear: Boolean, val exerciseName: String?, val bars: List<MonthBar>) :
        ProgressChart {
        val totalKg: Double get() = bars.sumOf { it.volumeKg }

        /** The current month's bar is highlighted in the current year. */
        fun isCurrent(index: Int): Boolean = isCurrentYear && index == bars.lastIndex
    }

    /** The heaviest set of each ISO week of one year, for one exercise. */
    data class Weeks(
        val year: Int,
        val isCurrentYear: Boolean,
        val exerciseName: String,
        val bars: List<WeekBar>,
        val averageSetWeight: AverageSetWeight?,
    ) : ProgressChart {
        /** Indices of weeks whose Thursday starts a new month; these get a label and, after the first, a separator. */
        val monthStarts: List<Int> get() = bars.indices.filter { it == 0 || bars[it].month != bars[it - 1].month }

        /** The heaviest week; ties go to the earliest (prototype readout). */
        val best: Int? get() = trainedWeeks.maxByOrNull { bars[it].top?.weightKg ?: 0.0 }

        val latest: Int? get() = trainedWeeks.lastOrNull()

        private val trainedWeeks: List<Int> get() = bars.indices.filter { bars[it].top != null }
    }

    /** One bar per year: averages only (SPEC §5.4). */
    data class Years(
        val kind: YearChartKind,
        val exerciseName: String?,
        val currentYear: Int,
        val bars: List<YearBar>,
        val summary: AllTimeSummary,
        val averageSetWeight: AverageSetWeight?,
    ) : ProgressChart
}

data class MonthBar(val month: Month, val volumeKg: Double, val count: Int)

/** A week; [month] is the month of its Thursday, which is the month the week mostly lies in. */
data class WeekBar(val monday: LocalDate, val isoWeek: Int, val month: Month, val top: TopSet?)

/**
 * A year in the all-time view. [valueKg] is the average volume per month, or the average weekly top set.
 * [count] is workouts (total volume), sets (exercise volume) or weeks trained (top set).
 */
data class YearBar(val year: Int, val valueKg: Double, val months: Int, val count: Int)

enum class YearChartKind { TOTAL_VOLUME, EXERCISE_VOLUME, TOP_SET }

/** The readout when no year is selected. */
sealed interface AllTimeSummary {
    data class AveragePerMonth(val kg: Double) : AllTimeSummary

    /** Average of every weekly top set over all years; null [kg] means no weighted sets yet. */
    data class AverageTopSet(val kg: Double?, val weeks: Int) : AllTimeSummary
}

/** The bar values of a chart, in order. */
val ProgressChart.values: List<Double>
    get() = when (this) {
        is ProgressChart.NoExercise -> emptyList()
        is ProgressChart.Months -> bars.map { it.volumeKg }
        is ProgressChart.Weeks -> bars.map { it.top?.weightKg ?: 0.0 }
        is ProgressChart.Years -> bars.map { it.valueKg }
    }

/** False when every bar is empty, so the chart shows a "No sets logged" message instead. */
val ProgressChart.hasData: Boolean get() = values.any { it > 0 }
