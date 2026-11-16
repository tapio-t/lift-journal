package com.example.liftjournal.domain.goals

import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.parser.effectiveWeightKg
import com.example.liftjournal.domain.parser.uncountedBodyweightSets
import com.example.liftjournal.domain.parser.volumeKg
import com.example.liftjournal.domain.time.isoWeek
import com.example.liftjournal.domain.time.mondayOfWeek
import java.time.LocalDate
import java.time.Month
import kotlin.math.ceil
import kotlin.math.roundToInt

/** What "Now" was measured over, for the line under the goal title (SPEC §5.6). */
sealed interface GoalBasis {
    data object HeaviestLastFourWeeks : GoalBasis

    data class AverageLastFourWeeks(val sets: Int) : GoalBasis

    data class WeekSoFar(val isoWeek: Int) : GoalBasis

    data class MonthSoFar(val month: Month) : GoalBasis
}

/** A goal's current level. [uncountedBodyweightSets] > 0 means bodyweight sets were skipped. */
data class GoalProgress(val currentKg: Double, val uncountedBodyweightSets: Int, val basis: GoalBasis)

/** Set goals look at the last 28 days, today included. */
const val GOAL_LOOKBACK_DAYS = 27L

private const val PERCENT = 100
private const val SET_GOAL_SUGGESTION_STEP_KG = 2.5
private const val VOLUME_GOAL_SUGGESTION_STEP_KG = 500.0
private const val SUGGESTION_RAISE = 1.1

/**
 * Current level of a goal (SPEC §6, prototype `goalCurrent`): top / average set over the last 4 weeks;
 * volume goals count the current week (Monday start) or the current month so far.
 */
fun goalProgress(goal: Goal, entries: List<JournalEntry>, today: LocalDate, bodyweightKg: Double?): GoalProgress =
    if (goal.type.needsPeriod) {
        volumeProgress(
            goal,
            entries,
            today,
            bodyweightKg,
        )
    } else {
        setProgress(goal, entries, today, bodyweightKg)
    }

/** Now ÷ target × 100, rounded. Can exceed 100. */
fun goalPercent(goal: Goal, progress: GoalProgress): Int =
    if (goal.targetKg > 0) (progress.currentKg / goal.targetKg * PERCENT).roundToInt() else 0

/** About 10% above the current level, rounded up to 2.5 kg (set goals) or 500 kg (volume); null when there's no level yet. */
fun suggestedTarget(type: GoalType, currentKg: Double): Double? {
    if (currentKg <= 0) return null
    val step = if (type.needsPeriod) VOLUME_GOAL_SUGGESTION_STEP_KG else SET_GOAL_SUGGESTION_STEP_KG
    return ceil(currentKg * SUGGESTION_RAISE / step) * step
}

private fun setProgress(
    goal: Goal,
    entries: List<JournalEntry>,
    today: LocalDate,
    bodyweightKg: Double?,
): GoalProgress {
    val from = today.minusDays(GOAL_LOOKBACK_DAYS)
    val sets = entries.filter { it.date in from..today }
        .flatMap { it.exercises }
        .filter { it.key == goal.exerciseKey }
        .flatMap { it.sets }
    val weights = sets.mapNotNull { it.effectiveWeightKg(bodyweightKg) }.filter { it > 0 }
    val uncounted = sets.count { it.effectiveWeightKg(bodyweightKg) == null }
    return if (goal.type == GoalType.TOP_SET) {
        GoalProgress(weights.maxOrNull() ?: 0.0, uncounted, GoalBasis.HeaviestLastFourWeeks)
    } else {
        GoalProgress(
            if (weights.isEmpty()) 0.0 else weights.average(),
            uncounted,
            GoalBasis.AverageLastFourWeeks(weights.size),
        )
    }
}

private fun volumeProgress(
    goal: Goal,
    entries: List<JournalEntry>,
    today: LocalDate,
    bodyweightKg: Double?,
): GoalProgress {
    val isWeekly = goal.period == GoalPeriod.WEEK
    val start = if (isWeekly) today.mondayOfWeek() else today.withDayOfMonth(1)
    val exercises = entries.filter { it.date in start..today }
        .flatMap { it.exercises }
        .filter { goal.type == GoalType.TOTAL_VOLUME || it.key == goal.exerciseKey }
    val basis = if (isWeekly) GoalBasis.WeekSoFar(today.isoWeek) else GoalBasis.MonthSoFar(today.month)
    return GoalProgress(
        currentKg = exercises.sumOf { it.volumeKg(bodyweightKg) },
        uncountedBodyweightSets = exercises.sumOf { it.uncountedBodyweightSets(bodyweightKg) },
        basis = basis,
    )
}
