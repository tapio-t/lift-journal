package com.example.liftjournal.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.liftjournal.appContainer
import com.example.liftjournal.backup.BackupOutcome
import com.example.liftjournal.backup.RealtimeBackupManager
import com.example.liftjournal.data.DocumentFiles
import com.example.liftjournal.data.JournalRepository
import com.example.liftjournal.data.SettingsRepository
import com.example.liftjournal.data.SpreadsheetExporter
import com.example.liftjournal.data.UserDataRepository
import com.example.liftjournal.domain.backup.BackupData
import com.example.liftjournal.domain.backup.readBackup
import com.example.liftjournal.domain.export.SpreadsheetFormat
import com.example.liftjournal.domain.format.parseKilograms
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.domain.time.TodayProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

private const val STOP_TIMEOUT_MS = 5_000L

/** What the user has typed or opened on the screen, apart from the stored data. */
internal data class SettingsInputs(
    val goalType: GoalType = GoalType.TOP_SET,
    val exerciseKey: String? = null,
    val period: GoalPeriod = GoalPeriod.WEEK,
    val targetText: String = "",
    val dialog: SettingsDialog? = null,
)

/** Goals & Settings: goals, bodyweight, export, restore, ghost text, appearance and erase (SPEC §6). */
class SettingsViewModel(
    private val journalRepository: JournalRepository,
    private val settingsRepository: SettingsRepository,
    private val userDataRepository: UserDataRepository,
    private val spreadsheetExporter: SpreadsheetExporter,
    private val documentFiles: DocumentFiles,
    private val backupManager: RealtimeBackupManager,
    private val todayProvider: TodayProvider,
) : ViewModel() {
    private val inputs = MutableStateFlow(SettingsInputs())
    private val events = Channel<SettingsEvent>(Channel.BUFFERED)

    /** The backup folder's display name is looked up from its provider only when the folder changes. */
    private val backupFolderName: Flow<String?> = settingsRepository.settings
        .map { it.backup.folderUri }
        .distinctUntilChanged()
        .map { uri -> uri?.let { backupManager.folderName(it) } }

    val uiState: StateFlow<SettingsUiState> =
        combine(
            journalRepository.entries,
            journalRepository.goals,
            settingsRepository.settings,
            backupFolderName,
            inputs,
        ) { entries, goals, settings, folderName, inputs ->
            buildSettingsState(entries, goals, settings, inputs, todayProvider.today(), folderName)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                SettingsUiState(todayProvider.today()),
            )

    val oneOffEvents: Flow<SettingsEvent> = events.receiveAsFlow()

    fun onGoalTypeSelected(type: GoalType) = inputs.update { it.copy(goalType = type) }

    fun onGoalExerciseSelected(key: String) = inputs.update { it.copy(exerciseKey = key) }

    fun onGoalPeriodSelected(period: GoalPeriod) = inputs.update { it.copy(period = period) }

    fun onGoalTargetChanged(text: String) = inputs.update { it.copy(targetText = text) }

    fun onSetGoalClicked() {
        val form = uiState.value.goalForm
        val target = parseKilograms(form.targetText)
        if (target == null) {
            events.trySend(SettingsEvent.TargetMissing)
            return
        }
        if (!form.canSubmit) return
        val goal = Goal(
            id = 0,
            type = form.type,
            exerciseKey = form.exerciseKey.takeIf { form.type.needsExercise },
            period = form.period.takeIf { form.type.needsPeriod },
            targetKg = target,
        )
        inputs.update { it.copy(targetText = "") }
        viewModelScope.launch {
            journalRepository.addGoal(goal)
            events.send(SettingsEvent.GoalAdded)
        }
    }

    fun onRemoveGoalClicked(goal: Goal) {
        viewModelScope.launch {
            journalRepository.removeGoal(goal.id)
            events.send(SettingsEvent.GoalRemoved(goal))
        }
    }

    fun onUndoRemoveGoal(goal: Goal) {
        viewModelScope.launch { journalRepository.restoreGoal(goal) }
    }

    /** Saves the typed bodyweight; anything that isn't a positive number clears it, as in the prototype. */
    fun onBodyweightEntered(text: String) {
        val kg = parseKilograms(text)
        if (kg == uiState.value.bodyweightKg) return
        viewModelScope.launch {
            settingsRepository.setBodyweight(kg)
            events.send(if (kg != null) SettingsEvent.BodyweightSaved(kg) else SettingsEvent.BodyweightCleared)
        }
    }

    fun onGhostTextChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGhostText(enabled) }
    }

    fun onThemeChanged(theme: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    /** The user picked where to save the spreadsheet; a null [destination] means they cancelled. */
    fun onExportDestinationPicked(format: SpreadsheetFormat, destination: Uri?) {
        if (destination == null) return
        viewModelScope.launch {
            val event = try {
                spreadsheetExporter.export(format, destination)
                SettingsEvent.Exported(format)
            } catch (_: IOException) {
                SettingsEvent.ExportFailed
            } catch (_: SecurityException) {
                SettingsEvent.ExportFailed
            }
            events.send(event)
        }
    }

    /** The user picked a backup file; it is read and previewed before anything is replaced. */
    fun onRestoreFilePicked(file: Uri?) {
        if (file == null) return
        viewModelScope.launch {
            val text = try {
                documentFiles.readText(file)
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
            if (text == null) {
                events.send(SettingsEvent.FileUnreadable)
                return@launch
            }
            val backup = readBackup(text)
            if (backup == null) {
                events.send(SettingsEvent.NotABackup)
                return@launch
            }
            inputs.update { it.copy(dialog = SettingsDialog.ConfirmRestore(backup, backup.preview())) }
        }
    }

    fun onRestoreConfirmed(backup: BackupData) {
        inputs.update { it.copy(dialog = null) }
        viewModelScope.launch {
            userDataRepository.restore(backup)
            events.send(SettingsEvent.BackupRestored)
        }
    }

    /** The user picked a backup folder; a null [folder] means they cancelled. */
    fun onBackupFolderPicked(folder: Uri?) {
        if (folder == null) return
        viewModelScope.launch {
            val outcome = try {
                backupManager.useFolder(folder)
            } catch (_: SecurityException) {
                BackupOutcome.FOLDER_UNAVAILABLE
            }
            events.send(SettingsEvent.BackupFinished(outcome))
        }
    }

    fun onBackUpNowClicked() {
        viewModelScope.launch { events.send(SettingsEvent.BackupFinished(backupManager.backUpNow())) }
    }

    /** Coming back to the screen re-checks the folder, e.g. after the user deleted it in a file manager. */
    fun onScreenResumed() {
        viewModelScope.launch { backupManager.checkFolderAccess() }
    }

    fun onEraseClicked() = inputs.update { it.copy(dialog = SettingsDialog.ConfirmErase) }

    fun onEraseConfirmed() {
        inputs.update { it.copy(dialog = null) }
        viewModelScope.launch {
            userDataRepository.eraseAll()
            events.send(SettingsEvent.DataErased)
        }
    }

    fun onDialogDismissed() = inputs.update { it.copy(dialog = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                SettingsViewModel(
                    journalRepository = container.journalRepository,
                    settingsRepository = container.settingsRepository,
                    userDataRepository = container.userDataRepository,
                    spreadsheetExporter = container.spreadsheetExporter,
                    documentFiles = container.documentFiles,
                    backupManager = container.backupManager,
                    todayProvider = container.todayProvider,
                )
            }
        }
    }
}

private fun BackupData.preview() = RestorePreview(
    entryCount = entries.size,
    firstDate = entries.minOfOrNull { it.date },
    lastDate = entries.maxOfOrNull { it.date },
    goalCount = goals.size,
    bodyweightKg = settings.bodyweightKg,
)
