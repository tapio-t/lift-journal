package com.example.liftjournal.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatWeight
import com.example.liftjournal.domain.format.formatWholeKg
import com.example.liftjournal.domain.format.longName
import com.example.liftjournal.domain.format.roundToOneDecimal
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalBasis
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType

/** A goal together with its exercise's display name, ready to be named on screen. */
data class NamedGoal(val goal: Goal, val exerciseName: String)

/** `145 kg` for set goals (one decimal), `12,500 kg` for volume goals (prototype `gFmt`). */
@Composable
@ReadOnlyComposable
fun goalValueText(goal: Goal, kg: Double): String {
    val number = if (goal.type.needsPeriod) formatWholeKg(kg) else formatWeight(roundToOneDecimal(kg))
    return stringResource(R.string.progress_weight_kg, number)
}

/** `Squat top set 160 kg`, `Total volume 20,000 kg / week` (prototype `goalName`). */
@Composable
@ReadOnlyComposable
fun goalFullName(named: NamedGoal): String {
    val goal = named.goal
    val target = goalValueText(goal, goal.targetKg)
    return when (goal.type) {
        GoalType.TOP_SET -> stringResource(R.string.goal_name_top, named.exerciseName, target)
        GoalType.AVERAGE_SET -> stringResource(R.string.goal_name_average, named.exerciseName, target)
        GoalType.TOTAL_VOLUME -> stringResource(R.string.goal_name_volume, target, periodText(goal.period))
        GoalType.EXERCISE_VOLUME ->
            stringResource(R.string.goal_name_exercise_volume, named.exerciseName, target, periodText(goal.period))
    }
}

/** `Squat top set`, `Volume / week`: the goal chips in Progress (prototype `goalShort`). */
@Composable
@ReadOnlyComposable
fun goalShortName(named: NamedGoal): String = when (named.goal.type) {
    GoalType.TOP_SET -> stringResource(R.string.goal_short_top, named.exerciseName)
    GoalType.AVERAGE_SET -> stringResource(R.string.goal_short_average, named.exerciseName)
    GoalType.TOTAL_VOLUME -> stringResource(R.string.goal_short_volume, periodText(named.goal.period))
    GoalType.EXERCISE_VOLUME ->
        stringResource(R.string.goal_short_exercise_volume, named.exerciseName, periodText(named.goal.period))
}

/** What "Now" was measured over: `heaviest set in the last 4 weeks`, `week 46 so far`. */
@Composable
@ReadOnlyComposable
fun goalBasisText(basis: GoalBasis): String = when (basis) {
    GoalBasis.HeaviestLastFourWeeks -> stringResource(R.string.goal_basis_heaviest)
    is GoalBasis.AverageLastFourWeeks -> pluralStringResource(R.plurals.goal_basis_average, basis.sets, basis.sets)
    is GoalBasis.WeekSoFar -> stringResource(R.string.goal_basis_week, basis.isoWeek)
    is GoalBasis.MonthSoFar -> stringResource(R.string.goal_basis_month, basis.month.longName())
}

@Composable
@ReadOnlyComposable
private fun periodText(period: GoalPeriod?): String =
    stringResource(if (period == GoalPeriod.MONTH) R.string.goal_period_month else R.string.goal_period_week)
