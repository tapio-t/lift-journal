package com.example.liftjournal.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatDayMonthYear
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val RowGap = 8.dp
private val StatusText = AppText.body.copy(fontWeight = FontWeight.Bold)
private val WarningText = AppText.small.copy(fontWeight = FontWeight.Bold)
private val CLOCK_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Real-time backup: the chosen folder, the last backup time and the buttons (SPEC §6 item 3, PLAN phase 6). */
@Composable
fun BackupFolderSection(
    backup: BackupFolderState,
    today: LocalDate,
    onChooseFolder: () -> Unit,
    onBackUpNow: () -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_section_backup_folder))
    val palette = LocalPalette.current
    if (backup.isChosen) {
        val name = backup.folderName
        val folderLabel = if (name != null) {
            stringResource(R.string.settings_backup_folder, name)
        } else {
            stringResource(R.string.settings_backup_folder_unnamed)
        }
        Text(folderLabel, style = StatusText, color = palette.foreground)
        Text(lastBackupText(backup.lastBackupAtMillis, today), style = AppText.small, color = palette.muted)
        if (backup.hasProblem) {
            Text(
                stringResource(R.string.settings_backup_problem),
                style = WarningText,
                color = palette.foreground,
                modifier = Modifier.padding(top = RowGap),
            )
        }
    } else {
        Hint(stringResource(R.string.settings_backup_not_set), Modifier.padding(bottom = RowGap))
    }
    BackupButtons(backup.isChosen, onChooseFolder, onBackUpNow)
}

@Composable
private fun BackupButtons(isChosen: Boolean, onChooseFolder: () -> Unit, onBackUpNow: () -> Unit) {
    Row(Modifier.padding(top = RowGap), horizontalArrangement = Arrangement.spacedBy(RowGap)) {
        SettingsButton(
            text = stringResource(R.string.settings_backup_choose),
            onClick = onChooseFolder,
            isPrimary = !isChosen,
            modifier = Modifier.weight(1f),
        )
        if (isChosen) {
            SettingsButton(
                text = stringResource(R.string.settings_backup_now),
                onClick = onBackUpNow,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** `Last backup: today 12:04`, `yesterday 18:30` or `09.11.2026 12:04`, in the device's time zone. */
@Composable
private fun lastBackupText(atMillis: Long?, today: LocalDate): String {
    if (atMillis == null) return stringResource(R.string.settings_backup_none_yet)
    val time = Instant.ofEpochMilli(atMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val clock = time.format(CLOCK_TIME)
    return when (time.toLocalDate()) {
        today -> stringResource(R.string.settings_backup_last_today, clock)
        today.minusDays(1) -> stringResource(R.string.settings_backup_last_yesterday, clock)
        else -> stringResource(R.string.settings_backup_last_date, formatDayMonthYear(time.toLocalDate()), clock)
    }
}
