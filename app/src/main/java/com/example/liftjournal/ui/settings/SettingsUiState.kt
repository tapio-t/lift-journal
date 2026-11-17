package com.example.liftjournal.ui.settings

import com.example.liftjournal.backup.BackupOutcome
import com.example.liftjournal.domain.backup.BackupData
import com.example.liftjournal.domain.export.SpreadsheetFormat
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.domain.importer.ImportConflictMode
import com.example.liftjournal.domain.importer.ImportPreview
import com.example.liftjournal.domain.importer.TextImport
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.domain.stats.KnownExercise
import com.example.liftjournal.ui.common.NamedGoal
import java.time.LocalDate

/** Everything the Goals & Settings tab shows (SPEC §6). */
data class SettingsUiState(
    val today: LocalDate,
    val entryCount: Int = 0,
    val workoutCount: Int = 0,
    val goals: List<GoalRow> = emptyList(),
    val goalForm: GoalForm = GoalForm(),
    val bodyweightKg: Double? = null,
    val ghostText: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val backup: BackupFolderState = BackupFolderState(),
    val dialog: SettingsDialog? = null,
    /** Shown while [dialog] is [SettingsDialog.ConfirmImport], recalculated against the current entries. */
    val importPreview: ImportPreview? = null,
) {
    val hasEntries: Boolean get() = entryCount > 0

    /** Erasing makes sense only when there is something to erase. */
    val hasUserData: Boolean get() = hasEntries || goals.isNotEmpty() || bodyweightKg != null
}

/** A goal in the list, with its current level and percentage. */
data class GoalRow(val goal: NamedGoal, val currentKg: Double, val percent: Int)

/** The new-goal form. [suggestedTargetKg] is about 10% above the current level, shown as the placeholder. */
data class GoalForm(
    val type: GoalType = GoalType.TOP_SET,
    val exercises: List<KnownExercise> = emptyList(),
    val exerciseKey: String? = null,
    val period: GoalPeriod = GoalPeriod.WEEK,
    val targetText: String = "",
    val suggestedTargetKg: Double? = null,
) {
    /** A goal about one exercise can't be set before any exercise is logged. */
    val canSubmit: Boolean get() = !type.needsExercise || exerciseKey != null
}

/** The real-time backup folder (PLAN phase 6). [folderName] is null when the provider can't tell. */
data class BackupFolderState(
    val isChosen: Boolean = false,
    val folderName: String? = null,
    val lastBackupAtMillis: Long? = null,
    val hasProblem: Boolean = false,
)

/** What a picked backup contains, shown before it replaces anything. */
data class RestorePreview(
    val entryCount: Int,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val goalCount: Int,
    val bodyweightKg: Double?,
)

sealed interface SettingsDialog {
    data class ConfirmRestore(val backup: BackupData, val preview: RestorePreview) : SettingsDialog

    data object ConfirmErase : SettingsDialog

    /** "Do you really want to delete goal …?" */
    data class ConfirmRemoveGoal(val goal: NamedGoal) : SettingsDialog

    /** The import preview; [mode] can be switched before confirming. */
    data class ConfirmImport(val textImport: TextImport, val mode: ImportConflictMode) : SettingsDialog
}

/** One-off messages, shown as snackbars. */
sealed interface SettingsEvent {
    data object GoalAdded : SettingsEvent

    data object TargetMissing : SettingsEvent

    data class BodyweightSaved(val kg: Double) : SettingsEvent

    data object BodyweightCleared : SettingsEvent

    data class Exported(val format: SpreadsheetFormat) : SettingsEvent

    data object ExportFailed : SettingsEvent

    data object BackupRestored : SettingsEvent

    data object NotABackup : SettingsEvent

    data object FileUnreadable : SettingsEvent

    data object DataErased : SettingsEvent

    data class BackupFinished(val outcome: BackupOutcome) : SettingsEvent

    /** [changedEntries] is 0 when everything in the file was already there. */
    data class Imported(val changedEntries: Int) : SettingsEvent

    data object NoDatesFound : SettingsEvent
}
