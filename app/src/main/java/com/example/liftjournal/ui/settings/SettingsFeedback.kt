package com.example.liftjournal.ui.settings

import android.content.res.Resources
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.liftjournal.R
import com.example.liftjournal.backup.BackupOutcome
import com.example.liftjournal.domain.format.formatDayMonthYear
import com.example.liftjournal.domain.format.formatWeight
import com.example.liftjournal.ui.common.ConfirmDialog
import com.example.liftjournal.ui.common.showForAction

/** Shows the view model's one-off messages as snackbars; removing a goal offers Undo. */
@Composable
fun SettingsEvents(viewModel: SettingsViewModel, snackbarHostState: SnackbarHostState) {
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.oneOffEvents.collect { event ->
            if (event is SettingsEvent.GoalRemoved) {
                val undo = snackbarHostState.showForAction(
                    resources.getString(R.string.settings_goal_removed),
                    resources.getString(R.string.action_undo),
                )
                if (undo) viewModel.onUndoRemoveGoal(event.goal)
                return@collect
            }
            snackbarHostState.showSnackbar(resources.message(event))
        }
    }
}

private fun Resources.message(event: SettingsEvent): String = when (event) {
    SettingsEvent.GoalAdded -> getString(R.string.settings_goal_added)
    is SettingsEvent.GoalRemoved -> getString(R.string.settings_goal_removed)
    SettingsEvent.TargetMissing -> getString(R.string.settings_goal_enter_target)
    is SettingsEvent.BodyweightSaved -> getString(R.string.settings_bodyweight_saved, formatWeight(event.kg))
    SettingsEvent.BodyweightCleared -> getString(R.string.settings_bodyweight_cleared)
    is SettingsEvent.Exported -> getString(R.string.settings_exported, event.format.extension)
    SettingsEvent.ExportFailed -> getString(R.string.settings_export_failed)
    SettingsEvent.BackupRestored -> getString(R.string.settings_restored)
    SettingsEvent.NotABackup -> getString(R.string.settings_not_a_backup)
    SettingsEvent.FileUnreadable -> getString(R.string.settings_file_unreadable)
    SettingsEvent.DataErased -> getString(R.string.settings_erased)
    is SettingsEvent.BackupFinished -> getString(
        when (event.outcome) {
            BackupOutcome.SUCCESS -> R.string.settings_backed_up
            BackupOutcome.NO_FOLDER -> R.string.settings_backup_no_folder
            BackupOutcome.FOLDER_UNAVAILABLE -> R.string.settings_backup_unavailable
            BackupOutcome.WRITE_FAILED -> R.string.settings_backup_failed
        },
    )
}

@Composable
fun SettingsDialogs(dialog: SettingsDialog?, viewModel: SettingsViewModel) {
    when (dialog) {
        null -> Unit
        is SettingsDialog.ConfirmRestore -> ConfirmDialog(
            title = stringResource(R.string.settings_restore_title),
            message = restorePreviewText(dialog.preview),
            confirmLabel = stringResource(R.string.settings_restore_confirm),
            onConfirm = { viewModel.onRestoreConfirmed(dialog.backup) },
            onDismiss = viewModel::onDialogDismissed,
        )
        SettingsDialog.ConfirmErase -> ConfirmDialog(
            message = stringResource(R.string.settings_erase_question),
            confirmLabel = stringResource(R.string.settings_erase_confirm),
            onConfirm = viewModel::onEraseConfirmed,
            onDismiss = viewModel::onDialogDismissed,
        )
    }
}

/** `2 journal entries`, `02.11.2026 – 04.11.2026`, `2 goals · bodyweight 80 kg`, then the warning. */
@Composable
private fun restorePreviewText(preview: RestorePreview): String {
    val entries = pluralStringResource(R.plurals.settings_entries, preview.entryCount, preview.entryCount)
    val first = preview.firstDate
    val last = preview.lastDate
    val dates = if (first != null && last != null) {
        stringResource(R.string.settings_restore_dates, formatDayMonthYear(first), formatDayMonthYear(last))
    } else {
        stringResource(R.string.settings_restore_no_dates)
    }
    val goals = pluralStringResource(R.plurals.settings_restore_goals, preview.goalCount, preview.goalCount)
    val bodyweight = preview.bodyweightKg
    val goalsAndBodyweight = if (bodyweight != null) {
        stringResource(R.string.settings_restore_bodyweight, goals, formatWeight(bodyweight))
    } else {
        stringResource(R.string.settings_restore_no_bodyweight, goals)
    }
    return stringResource(R.string.settings_restore_preview, entries, dates, goalsAndBodyweight)
}
