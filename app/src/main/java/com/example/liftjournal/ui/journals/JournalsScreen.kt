package com.example.liftjournal.ui.journals

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatDayMonthYear
import com.example.liftjournal.ui.common.ConfirmDialog
import com.example.liftjournal.ui.common.DatePickerPopup
import com.example.liftjournal.ui.common.showForAction
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

/** Horizontal page margin of Journals. */
private val JournalsGutter = 20.dp

/** Journals tab: a black page with search and dated entries, no calculations (SPEC §3). */
@Composable
fun JournalsScreen(viewModel: JournalsViewModel = viewModel(factory = JournalsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    JournalEvents(viewModel, snackbarHostState)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }
    DisposableEffect(Unit) { onDispose { viewModel.onStop() } }

    // The keyboard only opens when the user taps an entry; tapping anywhere else closes it (SPEC §3.1).
    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = JournalsGutter)) {
            JournalSearchBar(
                state = state,
                onOpened = viewModel::onSearchOpened,
                onQueryChange = viewModel::onQueryChanged,
                onClear = {
                    focusManager.clearFocus()
                    viewModel.onClearSearch()
                },
                onMonthTagClick = viewModel::onMonthTagClicked,
            )
            EntryList(state, viewModel)
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
    JournalDialogs(state.dialog, viewModel)
}

@Composable
private fun EntryList(state: JournalsUiState, viewModel: JournalsViewModel) {
    val top = state.top
    LazyColumn(Modifier.fillMaxSize()) {
        if (!state.isFiltering && top != null) {
            item(key = "top") {
                JournalEntryItem(
                    item = top,
                    actions = entryActions(top, viewModel, canEdit = state.topHasSavedEntry),
                    ghostLines = state.ghostLines,
                )
            }
        }
        if (state.isFiltering && state.entries.isEmpty()) {
            item(key = "none") { NoEntriesFound() }
        }
        items(state.entries, key = { it.date.toEpochDay() }) { entry ->
            JournalEntryItem(item = entry, actions = entryActions(entry, viewModel, canEdit = true))
        }
    }
}

private fun entryActions(item: EntryItem, viewModel: JournalsViewModel, canEdit: Boolean) = EntryActions(
    onTextChange = { viewModel.onTextChanged(item.date, it) },
    onFocusLost = viewModel::onEditorFocusLost,
    onChangeDate = if (canEdit) ({ viewModel.onChangeDateClicked(item.date) }) else null,
    onDelete = if (canEdit) ({ viewModel.onDeleteClicked(item.date) }) else null,
)

@Composable
private fun NoEntriesFound() {
    Text(
        text = stringResource(R.string.journals_none_found),
        style = AppText.small,
        color = LocalPalette.current.muted,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

@Composable
private fun JournalEvents(viewModel: JournalsViewModel, snackbarHostState: SnackbarHostState) {
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.oneOffEvents.collect { event ->
            when (event) {
                is JournalEvent.EntryDeleted -> {
                    val undo = snackbarHostState.showForAction(
                        message = resources.getString(R.string.journals_entry_deleted),
                        actionLabel = resources.getString(R.string.action_undo),
                    )
                    if (undo) viewModel.onUndoDelete(event.entry)
                }
                JournalEvent.BackupProblem -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.journals_backup_problem),
                )
            }
        }
    }
}

@Composable
private fun JournalDialogs(dialog: JournalDialog?, viewModel: JournalsViewModel) {
    when (dialog) {
        null -> Unit
        is JournalDialog.PickMoveDate -> DatePickerPopup(
            initialDate = dialog.from,
            onDatePicked = { viewModel.onMoveDatePicked(dialog.from, it) },
            onDismiss = viewModel::onDialogDismissed,
        )
        is JournalDialog.ConfirmMerge -> ConfirmDialog(
            title = stringResource(R.string.journals_merge_title, formatDayMonthYear(dialog.to)),
            message = stringResource(R.string.journals_merge_message),
            confirmLabel = stringResource(R.string.journals_merge_confirm),
            onConfirm = { viewModel.onMergeConfirmed(dialog.from, dialog.to) },
            onDismiss = viewModel::onDialogDismissed,
        )
        is JournalDialog.ConfirmDelete -> ConfirmDialog(
            message = stringResource(R.string.journals_delete_question),
            confirmLabel = stringResource(R.string.journals_delete_confirm),
            onConfirm = { viewModel.onDeleteConfirmed(dialog.date) },
            onDismiss = viewModel::onDialogDismissed,
        )
    }
}
