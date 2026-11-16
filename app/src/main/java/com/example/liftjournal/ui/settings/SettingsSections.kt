package com.example.liftjournal.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liftjournal.R
import com.example.liftjournal.domain.export.SpreadsheetFormat
import com.example.liftjournal.domain.format.formatWeight
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

private val BodyweightFieldWidth = 180.dp
private val RowGap = 8.dp
private val EntryFormatText = AppText.small.copy(lineHeight = 23.sp)
private val UnitText = AppText.body.copy(fontWeight = FontWeight.Bold)

/** Bodyweight in kg; saved when the keyboard's Done is pressed or the field loses focus (SPEC §6 item 2). */
@Composable
fun BodyweightSection(bodyweightKg: Double?, onEntered: (String) -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_bodyweight))
    // Re-initialised whenever the stored value changes, e.g. after a restore.
    var text by rememberSaveable(bodyweightKg) { mutableStateOf(bodyweightKg?.let(::formatWeight).orEmpty()) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RowGap)) {
        NumberField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.settings_bodyweight_placeholder),
            accessibilityLabel = stringResource(R.string.settings_bodyweight_label),
            modifier = Modifier.widthIn(max = BodyweightFieldWidth),
            onDone = { onEntered(text) },
        )
        Text(stringResource(R.string.settings_kg), style = UnitText, color = LocalPalette.current.foreground)
    }
    Hint(stringResource(R.string.settings_bodyweight_hint))
}

/** Export buttons, disabled until there is something to export (SPEC §6 item 4). */
@Composable
fun SpreadsheetSection(hasEntries: Boolean, onExport: (SpreadsheetFormat) -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_spreadsheet))
    Row(horizontalArrangement = Arrangement.spacedBy(RowGap)) {
        SettingsButton(
            text = stringResource(R.string.settings_export_xlsx),
            onClick = { onExport(SpreadsheetFormat.XLSX) },
            isPrimary = true,
            enabled = hasEntries,
            modifier = Modifier.weight(1f),
        )
        SettingsButton(
            text = stringResource(R.string.settings_export_ods),
            onClick = { onExport(SpreadsheetFormat.ODS) },
            isPrimary = true,
            enabled = hasEntries,
            modifier = Modifier.weight(1f),
        )
    }
    Hint(stringResource(R.string.settings_export_hint))
}

@Composable
fun RestoreSection(onRestore: () -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_restore))
    SettingsButton(stringResource(R.string.settings_restore), onRestore, Modifier.fillMaxWidth())
    Hint(stringResource(R.string.settings_restore_hint))
}

@Composable
fun GhostTextSection(isOn: Boolean, onChange: (Boolean) -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_ghost))
    ChoiceSegments(
        options = listOf(true, false),
        selected = isOn,
        label = { stringResource(if (it) R.string.settings_on else R.string.settings_off) },
        onSelect = onChange,
    )
    Hint(stringResource(R.string.settings_ghost_hint))
}

@Composable
fun AppearanceSection(theme: ThemeMode, onChange: (ThemeMode) -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_appearance))
    ChoiceSegments(ThemeMode.entries, theme, { themeName(it) }, onChange)
}

/** A short help card with the entry format examples (SPEC §4, §6 item 9). */
@Composable
fun EntryFormatSection() {
    SectionHeader(stringResource(R.string.settings_section_entry_format))
    val palette = LocalPalette.current
    val examples = stringArrayResource(R.array.settings_entry_format_examples)
    val meanings = stringArrayResource(R.array.settings_entry_format_meanings)
    val codeStyle =
        SpanStyle(fontFamily = AppText.mono.fontFamily, fontSize = AppText.mono.fontSize, background = palette.soft)
    FramedBox {
        examples.zip(meanings).forEach { (example, meaning) ->
            val line = buildAnnotatedString {
                withStyle(codeStyle) { append(example) }
                append(" $meaning")
            }
            Text(line, style = EntryFormatText, color = palette.foreground)
        }
        Text(stringResource(R.string.settings_entry_format_note), style = EntryFormatText, color = palette.muted)
    }
}

@Composable
fun ResetSection(isEnabled: Boolean, onErase: () -> Unit) {
    SectionHeader(stringResource(R.string.settings_section_reset))
    SettingsButton(
        text = stringResource(R.string.settings_erase),
        onClick = onErase,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth().padding(bottom = RowGap),
    )
}

@Composable
private fun themeName(theme: ThemeMode): String = stringResource(
    when (theme) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)
