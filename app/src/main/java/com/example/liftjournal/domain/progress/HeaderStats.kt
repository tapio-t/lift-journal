package com.example.liftjournal.domain.progress

import com.example.liftjournal.domain.stats.AllTimeStats
import com.example.liftjournal.domain.stats.YearStats
import java.time.Month

private const val MONTHS_IN_YEAR = 12

/** The third header stat (SPEC §5.1). */
sealed interface ThirdStat {
    /** Current year: volume so far in the current month. */
    data class MonthSoFar(val month: Month, val kg: Double) : ThirdStat

    data class AveragePerMonth(val kg: Double) : ThirdStat
}

/** The three stats at the right of the Progress header. [period] says whether the total is for a year or all-time. */
data class HeaderStats(val period: ProgressPeriod, val workouts: Int, val totalKg: Double, val third: ThirdStat)

fun headerStats(stats: YearStats): HeaderStats {
    val third = if (stats.isCurrentYear) {
        val current = stats.months.last()
        ThirdStat.MonthSoFar(Month.of(current.monthIndex + 1), current.totalKg)
    } else {
        // A past year is averaged over all twelve months, as in the prototype.
        ThirdStat.AveragePerMonth(stats.totalKg / MONTHS_IN_YEAR)
    }
    return HeaderStats(ProgressPeriod.OneYear(stats.year), stats.workouts, stats.totalKg, third)
}

fun headerStats(stats: AllTimeStats): HeaderStats = HeaderStats(
    ProgressPeriod.AllTime,
    stats.workouts,
    stats.totalKg,
    ThirdStat.AveragePerMonth(stats.averageKgPerMonth),
)
