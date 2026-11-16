package com.example.liftjournal.ui.progress

import com.example.liftjournal.domain.goals.GoalProgress
import com.example.liftjournal.domain.progress.HeaderStats
import com.example.liftjournal.domain.progress.ProgressChart
import com.example.liftjournal.domain.progress.ProgressPeriod
import com.example.liftjournal.domain.progress.ProgressView
import com.example.liftjournal.ui.common.NamedGoal
import java.time.LocalDate

/** An exercise chip: weighted exercises in the period, biggest volume first (SPEC §5.1). */
data class ExerciseChip(val key: String, val name: String)

/** The goal view (SPEC §5.6). [percent] can exceed 100. */
data class GoalPanel(
    val goal: NamedGoal,
    val progress: GoalProgress,
    val percent: Int,
    val showsBodyweightWarning: Boolean,
)

/** Everything the Progress tab shows. [chart] is null in the goal view, [goal] outside it. */
data class ProgressUiState(
    val today: LocalDate,
    val isLoaded: Boolean = false,
    val period: ProgressPeriod = ProgressPeriod.OneYear(today.year),
    val years: List<Int> = emptyList(),
    val isYearChipsOpen: Boolean = false,
    val header: HeaderStats? = null,
    val views: List<ProgressView> = emptyList(),
    val view: ProgressView = ProgressView.TOTAL_VOLUME,
    val exercises: List<ExerciseChip> = emptyList(),
    val selectedExerciseKey: String? = null,
    val chart: ProgressChart? = null,
    val goal: GoalPanel? = null,
    val goals: List<NamedGoal> = emptyList(),
    val selectedGoalIndex: Int = 0,
    val selectedBar: Int? = null,
    val showsTable: Boolean = false,
    val isBodyweightMissing: Boolean = false,
) {
    /** The period control is hidden in the goal view (SPEC §5.2). */
    val showsPeriodControl: Boolean get() = view != ProgressView.GOAL

    val showsExerciseChips: Boolean get() = view.needsExercise && exercises.isNotEmpty()

    /** Goal chips appear only when there is more than one goal to choose from. */
    val showsGoalChips: Boolean get() = view == ProgressView.GOAL && goals.size > 1
}
