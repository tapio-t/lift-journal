package com.example.liftjournal.domain.progress

import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.stats.ExerciseTotals
import com.example.liftjournal.domain.stats.allTimeStats
import com.example.liftjournal.domain.stats.yearStats
import java.time.LocalDate

/** The year or all-time numbers behind the Progress header, exercise chips and chart. */
class PeriodStats(
    val header: HeaderStats,
    /** Weighted exercises in the period, biggest volume first. */
    val exercises: List<ExerciseTotals>,
    private val chartFor: (ProgressView, ExerciseTotals?) -> ProgressChart,
) {
    /** The chart for [view]; [exerciseKey] falls back to the biggest exercise when it has no sets here. */
    fun chart(view: ProgressView, exerciseKey: String?): ProgressChart = chartFor(view, exercise(exerciseKey))

    fun exercise(requestedKey: String?): ExerciseTotals? = resolveExercise(exercises, requestedKey)
}

/** Calculates [period] from all [entries], counting up to [today]. */
fun periodStats(
    period: ProgressPeriod,
    entries: List<JournalEntry>,
    today: LocalDate,
    bodyweightKg: Double?,
): PeriodStats = when (period) {
    is ProgressPeriod.OneYear -> {
        val stats = yearStats(entries, period.year, today, bodyweightKg)
        PeriodStats(headerStats(stats), stats.exercises) { view, exercise -> yearChart(stats, view, exercise) }
    }
    ProgressPeriod.AllTime -> {
        val stats = allTimeStats(entries, today, bodyweightKg)
        PeriodStats(headerStats(stats), stats.exercises) { view, exercise ->
            allTimeChart(stats, view, exercise, today.year)
        }
    }
}
