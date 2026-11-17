package com.example.liftjournal.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.liftjournal.appContainer
import com.example.liftjournal.data.JournalRepository
import com.example.liftjournal.data.SettingsRepository
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.goalPercent
import com.example.liftjournal.domain.goals.goalProgress
import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.progress.ProgressPeriod
import com.example.liftjournal.domain.progress.ProgressView
import com.example.liftjournal.domain.progress.periodStats
import com.example.liftjournal.domain.settings.AppSettings
import com.example.liftjournal.domain.stats.availableYears
import com.example.liftjournal.domain.stats.exerciseDisplayName
import com.example.liftjournal.domain.time.TodayProvider
import com.example.liftjournal.ui.common.NamedGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

private const val STOP_TIMEOUT_MS = 5_000L

/** What the user picked on the Progress tab. A null [period] means the current year; a null [view], none picked. */
private data class ProgressInputs(
    val period: ProgressPeriod? = null,
    val isYearChipsOpen: Boolean = false,
    val view: ProgressView? = null,
    val exerciseKey: String? = null,
    val goalIndex: Int = 0,
    val selectedBar: Int? = null,
    val showsTable: Boolean = false,
)

/** Progress: period, chart choice, bar selection and the goal view, all calculated from the journal. */
class ProgressViewModel(
    journalRepository: JournalRepository,
    settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
) : ViewModel() {
    private val inputs = MutableStateFlow(ProgressInputs())

    val uiState: StateFlow<ProgressUiState> =
        combine(journalRepository.entries, journalRepository.goals, settingsRepository.settings, inputs, ::buildState)
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                ProgressUiState(todayProvider.today()),
            )

    fun onAllTimeClicked() = changeChart { it.copy(period = ProgressPeriod.AllTime, isYearChipsOpen = false) }

    /** The year button opens and closes the year chips. */
    fun onYearButtonClicked() = inputs.update { it.copy(isYearChipsOpen = !it.isYearChipsOpen) }

    fun onYearPicked(year: Int) = changeChart {
        it.copy(period = ProgressPeriod.OneYear(year), isYearChipsOpen = false)
    }

    fun onViewSelected(view: ProgressView) = changeChart { it.copy(view = view) }

    fun onExerciseSelected(key: String) = changeChart { it.copy(exerciseKey = key) }

    fun onGoalSelected(index: Int) = inputs.update { it.copy(goalIndex = index) }

    /** Tapping a bar selects it; tapping it again clears the selection (SPEC §5.5). */
    fun onBarTapped(index: Int) = inputs.update { it.copy(selectedBar = if (it.selectedBar == index) null else index) }

    fun onTableToggled() = inputs.update { it.copy(showsTable = !it.showsTable) }

    /** Any change of what the chart shows clears the bar selection. */
    private fun changeChart(change: (ProgressInputs) -> ProgressInputs) =
        inputs.update { change(it).copy(selectedBar = null) }

    private fun buildState(
        entries: List<JournalEntry>,
        goals: List<Goal>,
        settings: AppSettings,
        inputs: ProgressInputs,
    ): ProgressUiState {
        val today = todayProvider.today()
        val bodyweightKg = settings.user.bodyweightKg
        val years = availableYears(entries, today)
        val period =
            inputs.period.takeIf { it == ProgressPeriod.AllTime || (it as? ProgressPeriod.OneYear)?.year in years }
                ?: ProgressPeriod.OneYear(today.year)
        val view = shownView(inputs.view, hasGoals = goals.isNotEmpty())
        val namedGoals = goals.map { NamedGoal(it, exerciseDisplayName(it.exerciseKey.orEmpty(), entries)) }
        val goalIndex = inputs.goalIndex.coerceIn(0, maxOf(0, goals.lastIndex))

        val base = ProgressUiState(
            today = today,
            isLoaded = true,
            period = period,
            years = years,
            isYearChipsOpen = inputs.isYearChipsOpen,
            views = availableViews(hasGoals = goals.isNotEmpty()),
            view = view,
            goals = namedGoals,
            selectedGoalIndex = goalIndex,
            selectedBar = inputs.selectedBar,
            showsTable = inputs.showsTable,
            isBodyweightMissing = bodyweightKg == null && entries.any { it.hasBodyweightSet() },
        )
        val withGoal = if (view == ProgressView.GOAL) {
            base.copy(goal = goalPanel(namedGoals[goalIndex], entries, today, bodyweightKg))
        } else {
            base
        }
        return withPeriodStats(withGoal, entries, inputs.exerciseKey, bodyweightKg)
    }

    /** Adds the header stats, exercise chips and chart of the chosen period. */
    private fun withPeriodStats(
        state: ProgressUiState,
        entries: List<JournalEntry>,
        requestedExercise: String?,
        bodyweightKg: Double?,
    ): ProgressUiState {
        val stats = periodStats(state.period, entries, state.today, bodyweightKg)
        return state.copy(
            header = stats.header,
            exercises = stats.exercises.map { ExerciseChip(it.key, it.name) },
            selectedExerciseKey = stats.exercise(requestedExercise)?.key,
            chart = if (state.view == ProgressView.GOAL) null else stats.chart(state.view, requestedExercise),
        )
    }

    private fun goalPanel(
        named: NamedGoal,
        entries: List<JournalEntry>,
        today: LocalDate,
        bodyweightKg: Double?,
    ): GoalPanel {
        val progress = goalProgress(named.goal, entries, today, bodyweightKg)
        return GoalPanel(
            goal = named,
            progress = progress,
            percent = goalPercent(named.goal, progress),
            showsBodyweightWarning = progress.uncountedBodyweightSets > 0 && bodyweightKg == null,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                ProgressViewModel(container.journalRepository, container.settingsRepository, container.todayProvider)
            }
        }
    }
}

/** `Goal` comes first, and only when at least one goal exists (SPEC §5.1). */
private fun availableViews(hasGoals: Boolean): List<ProgressView> {
    val chartViews = listOf(ProgressView.TOTAL_VOLUME, ProgressView.TOP_SET, ProgressView.EXERCISE_VOLUME)
    return if (hasGoals) listOf(ProgressView.GOAL) + chartViews else chartViews
}

/**
 * The view to show for the user's [chosen] one. Until the user picks a view, the goal view (with the first goal set)
 * is shown when a goal exists (owner decision), otherwise total volume. A chosen goal view falls back to total
 * volume once the last goal is removed.
 */
internal fun shownView(chosen: ProgressView?, hasGoals: Boolean): ProgressView = when {
    chosen == null -> if (hasGoals) ProgressView.GOAL else ProgressView.TOTAL_VOLUME
    chosen == ProgressView.GOAL && !hasGoals -> ProgressView.TOTAL_VOLUME
    else -> chosen
}

private fun JournalEntry.hasBodyweightSet(): Boolean = exercises.any { exercise ->
    exercise.sets.any { it.isBodyweight }
}
