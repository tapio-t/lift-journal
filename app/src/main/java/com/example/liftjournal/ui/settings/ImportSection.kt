package com.example.liftjournal.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatDayMonthYear
import com.example.liftjournal.domain.importer.ImportConflictMode
import com.example.liftjournal.domain.importer.ImportPreview
import com.example.liftjournal.ui.theme.AppText
import java.time.LocalDate

/** At most this many conflicting dates are listed by name in the preview. */
private const val LISTED_DATES = 5
private val LineGap = 8.dp

/** The import button and how the file should look (SPEC §6 item 6, PLAN phase 7). */
@Composable
fun ImportSection(onImport: () -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_import))
    SettingsButton(stringResource(R.string.settings_import), onImport, Modifier.fillMaxWidth())
    Hint(stringResource(R.string.settings_import_hint))
}

/** The preview before importing; the conflict choice only shows when some dates already have entries. */
@Composable
fun ImportPreviewDialog(
    preview: ImportPreview,
    mode: ImportConflictMode,
    onModeChange: (ImportConflictMode) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_import_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ImportSummary(preview)
                if (preview.datesWithEntries.isNotEmpty()) {
                    FieldLabel(stringResource(R.string.settings_import_existing_label))
                    ChoiceSegments(ImportConflictMode.entries, mode, { modeName(it) }, onModeChange)
                }
                val changes = preview.plan.entriesToWrite.size
                Text(
                    pluralStringResource(R.plurals.settings_import_result_preview, changes, changes),
                    style = AppText.body,
                    modifier = Modifier.padding(top = LineGap),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.settings_import_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ImportSummary(preview: ImportPreview) {
    val lines = buildList {
        add(entryRangeText(preview))
        if (preview.datesWithEntries.isNotEmpty()) add(existingDatesText(preview.datesWithEntries))
        val unreadable = preview.unreadableParts
        if (unreadable > 0) add(pluralStringResource(R.plurals.settings_import_unreadable, unreadable, unreadable))
        val ignored = preview.ignoredLines
        if (ignored.isNotEmpty()) {
            add(pluralStringResource(R.plurals.settings_import_ignored, ignored.size, ignored.size, ignored.first()))
        }
    }
    lines.forEach { Text(it, style = AppText.small, modifier = Modifier.padding(bottom = LineGap)) }
}

/** `4 entries · 02.11.2026 – 09.11.2026`. */
@Composable
private fun entryRangeText(preview: ImportPreview): String {
    val entries = pluralStringResource(R.plurals.settings_import_entries, preview.entryCount, preview.entryCount)
    val first = preview.firstDate ?: return entries
    val last = preview.lastDate ?: return entries
    return stringResource(R.string.settings_import_dates, entries, formatDayMonthYear(first), formatDayMonthYear(last))
}

/** `2 dates already have an entry: 04.11.2026, 06.11.2026`, shortened with "and N more". */
@Composable
private fun existingDatesText(dates: List<LocalDate>): String {
    val listed = dates.take(LISTED_DATES).joinToString(", ") { formatDayMonthYear(it) }
    val hidden = dates.size - LISTED_DATES
    val names = if (hidden > 0) stringResource(R.string.settings_import_more_dates, listed, hidden) else listed
    return pluralStringResource(R.plurals.settings_import_existing, dates.size, dates.size, names)
}

@Composable
private fun modeName(mode: ImportConflictMode): String = stringResource(
    when (mode) {
        ImportConflictMode.APPEND -> R.string.settings_import_mode_append
        ImportConflictMode.SKIP_EXISTING -> R.string.settings_import_mode_skip
    },
)
