package com.example.liftjournal.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Undo snackbars stay about 5 seconds (SPEC §3.3); Material only offers 4 s or 10 s. */
const val UNDO_SNACKBAR_MS = 5_000L

/** Shows a snackbar with an action for [durationMillis] and returns true if the action was tapped. */
suspend fun SnackbarHostState.showForAction(
    message: String,
    actionLabel: String,
    durationMillis: Long = UNDO_SNACKBAR_MS,
): Boolean = coroutineScope {
    val timeout = launch {
        delay(durationMillis)
        currentSnackbarData?.dismiss()
    }
    val result =
        showSnackbar(message, actionLabel, withDismissAction = false, duration = SnackbarDuration.Indefinite)
    timeout.cancel()
    result == SnackbarResult.ActionPerformed
}
