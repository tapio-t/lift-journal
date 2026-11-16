package com.example.liftjournal.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.liftjournal.R
import com.example.liftjournal.domain.export.SpreadsheetFormat
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

/** The prototype's settings column is at most 640 px wide with 18 px side padding (`.wrap`). */
private val MaxContentWidth = 640.dp
private val PageGutter = 18.dp
private val PageTopGap = 6.dp
private val PageTitleText = AppText.pageTitle.copy(fontSize = 28.sp)

/** Backups are JSON; some file providers report them as plain text or generic binary. */
private val BACKUP_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

/** Goals & Settings tab (SPEC §6). Follows the appearance setting, like Progress. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    SettingsEvents(viewModel, snackbarHostState)

    val exportXlsx = rememberLauncherForActivityResult(CreateDocument(SpreadsheetFormat.XLSX.mimeType)) {
        viewModel.onExportDestinationPicked(SpreadsheetFormat.XLSX, it)
    }
    val exportOds = rememberLauncherForActivityResult(CreateDocument(SpreadsheetFormat.ODS.mimeType)) {
        viewModel.onExportDestinationPicked(SpreadsheetFormat.ODS, it)
    }
    val pickBackup = rememberLauncherForActivityResult(OpenDocument(), viewModel::onRestoreFilePicked)
    val onExport: (SpreadsheetFormat) -> Unit = { format ->
        val launcher = if (format == SpreadsheetFormat.XLSX) exportXlsx else exportOds
        launcher.launch(format.fileName(state.today))
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = PageGutter, vertical = PageTopGap),
            ) {
                SettingsContent(state, viewModel, onExport, onRestore = { pickBackup.launch(BACKUP_MIME_TYPES) })
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
    SettingsDialogs(state.dialog, viewModel)
}

/** The sections in the order of SPEC §6. Backup folder (phase 6) and text import (phase 7) slot in later. */
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onExport: (SpreadsheetFormat) -> Unit,
    onRestore: () -> Unit,
) {
    PageHeader(state)
    SectionHeader(stringResource(R.string.settings_section_goals))
    GoalsSection(
        goals = state.goals,
        form = state.goalForm,
        onRemove = viewModel::onRemoveGoalClicked,
        actions = GoalFormActions(
            onTypeSelected = viewModel::onGoalTypeSelected,
            onExerciseSelected = viewModel::onGoalExerciseSelected,
            onPeriodSelected = viewModel::onGoalPeriodSelected,
            onTargetChanged = viewModel::onGoalTargetChanged,
            onSubmit = viewModel::onSetGoalClicked,
        ),
    )
    BodyweightSection(state.bodyweightKg, viewModel::onBodyweightEntered)
    SpreadsheetSection(state.hasEntries, onExport)
    RestoreSection(onRestore)
    GhostTextSection(state.ghostText, viewModel::onGhostTextChanged)
    AppearanceSection(state.theme, viewModel::onThemeChanged)
    EntryFormatSection()
    ResetSection(state.hasUserData, viewModel::onEraseClicked)
}

/** The title and `12 journal entries · 9 logged workouts`. */
@Composable
private fun PageHeader(state: SettingsUiState) {
    val palette = LocalPalette.current
    Text(
        stringResource(R.string.settings_title),
        style = PageTitleText,
        color = palette.foreground,
        modifier = Modifier.padding(top = PageTopGap).semantics { heading() },
    )
    Text(
        stringResource(
            R.string.settings_summary,
            pluralStringResource(R.plurals.settings_entries, state.entryCount, state.entryCount),
            pluralStringResource(R.plurals.settings_logged_workouts, state.workoutCount, state.workoutCount),
        ),
        style = AppText.hint,
        color = palette.muted,
    )
}
