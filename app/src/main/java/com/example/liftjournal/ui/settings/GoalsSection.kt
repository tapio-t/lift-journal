package com.example.liftjournal.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatWeight
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.ui.common.goalFullName
import com.example.liftjournal.ui.common.goalValueText
import com.example.liftjournal.ui.common.styledStringResource
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

private val GoalRowPadding = 10.dp
private val RemoveButtonSize = 34.dp
private val FormTopGap = 8.dp
private val FieldGap = 12.dp
private val GoalNameText = AppText.body.copy(fontWeight = FontWeight.Bold)
private val RemoveText = AppText.body.copy(fontSize = 18.sp)

/** The goal list and the new-goal form (SPEC §6 item 1). */
@Composable
fun GoalsSection(goals: List<GoalRow>, form: GoalForm, onRemove: (Goal) -> Unit, actions: GoalFormActions) {
    goals.forEach { GoalListRow(it, onRemove) }
    if (goals.isNotEmpty()) {
        Text(
            styledStringResource(R.string.settings_goals_shown_in_progress),
            style = AppText.hint,
            color = LocalPalette.current.muted,
            modifier = Modifier.padding(vertical = FormTopGap),
        )
    }
    GoalFormBox(form, actions, Modifier.padding(top = FormTopGap))
}

/** What the user can do in the new-goal form. */
class GoalFormActions(
    val onTypeSelected: (GoalType) -> Unit,
    val onExerciseSelected: (String) -> Unit,
    val onPeriodSelected: (GoalPeriod) -> Unit,
    val onTargetChanged: (String) -> Unit,
    val onSubmit: () -> Unit,
)

@Composable
private fun GoalListRow(row: GoalRow, onRemove: (Goal) -> Unit) {
    val palette = LocalPalette.current
    val name = goalFullName(row.goal)
    Row(
        Modifier.fillMaxWidth().padding(vertical = GoalRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GoalRowPadding),
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = GoalNameText, color = palette.foreground)
            Text(
                styledStringResource(
                    R.string.settings_goal_now,
                    goalValueText(row.goal.goal, row.currentKg),
                    row.percent,
                ),
                style = AppText.hint,
                color = palette.muted,
            )
        }
        val removeLabel = stringResource(R.string.settings_goal_remove, name)
        Box(
            Modifier
                .minimumInteractiveComponentSize()
                .size(RemoveButtonSize)
                .clip(CircleShape)
                .border(BorderStroke(1.dp, palette.faint), CircleShape)
                .clickable(role = Role.Button) { onRemove(row.goal.goal) }
                .semantics { contentDescription = removeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text("×", style = RemoveText, color = palette.foreground)
        }
    }
    HorizontalDivider(color = palette.line)
}

@Composable
private fun GoalFormBox(form: GoalForm, actions: GoalFormActions, modifier: Modifier) {
    FramedBox(modifier) {
        val typeLabel = stringResource(R.string.settings_goal_type)
        FieldLabel(typeLabel)
        Dropdown(GoalType.entries, form.type, { goalTypeName(it) }, actions.onTypeSelected, typeLabel)
        Spacer(Modifier.height(FieldGap))
        if (form.type.needsExercise) {
            ExerciseField(form, actions.onExerciseSelected)
            Spacer(Modifier.height(FieldGap))
        }
        if (form.type.needsPeriod) {
            val periodLabel = stringResource(R.string.settings_goal_period)
            FieldLabel(periodLabel)
            Dropdown(GoalPeriod.entries, form.period, { periodName(it) }, actions.onPeriodSelected, periodLabel)
            Spacer(Modifier.height(FieldGap))
        }
        val targetLabel = stringResource(R.string.settings_goal_target)
        FieldLabel(targetLabel)
        NumberField(
            value = form.targetText,
            onValueChange = actions.onTargetChanged,
            placeholder = targetPlaceholder(form.suggestedTargetKg),
            accessibilityLabel = targetLabel,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FieldGap))
        SettingsButton(
            text = stringResource(R.string.settings_goal_set),
            onClick = actions.onSubmit,
            isPrimary = true,
            enabled = form.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExerciseField(form: GoalForm, onSelect: (String) -> Unit) {
    val label = stringResource(R.string.settings_goal_exercise)
    FieldLabel(label)
    val selected = form.exercises.firstOrNull { it.key == form.exerciseKey }
    if (selected == null) {
        Text(
            stringResource(R.string.settings_goal_no_exercises),
            style = AppText.hint,
            color = LocalPalette.current.muted,
        )
        return
    }
    Dropdown(form.exercises, selected, { it.name }, { onSelect(it.key) }, label)
}

@Composable
private fun goalTypeName(type: GoalType): String = stringResource(
    when (type) {
        GoalType.TOP_SET -> R.string.settings_goal_type_top
        GoalType.AVERAGE_SET -> R.string.settings_goal_type_average
        GoalType.TOTAL_VOLUME -> R.string.settings_goal_type_volume
        GoalType.EXERCISE_VOLUME -> R.string.settings_goal_type_exercise_volume
    },
)

@Composable
private fun periodName(period: GoalPeriod): String = stringResource(
    if (period == GoalPeriod.MONTH) R.string.settings_goal_per_month else R.string.settings_goal_per_week,
)

@Composable
private fun targetPlaceholder(suggestedKg: Double?): String = if (suggestedKg != null) {
    stringResource(R.string.settings_goal_target_suggestion, formatWeight(suggestedKg))
} else {
    stringResource(R.string.settings_goal_target_placeholder)
}
