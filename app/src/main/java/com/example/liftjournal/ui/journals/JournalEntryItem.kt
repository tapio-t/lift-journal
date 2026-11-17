package com.example.liftjournal.ui.journals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatDayMonthYear
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

private val EntryTopPadding = 20.dp
private val EntryBottomPadding = 16.dp
private val PenIconSize = 18.dp
private val HeaderMinHeight = 48.dp

/** What a journal entry row can do. Only saved entries have the pen. */
class EntryActions(
    val onTextChange: (String) -> Unit,
    val onFocusLost: () -> Unit,
    val onChangeDate: (() -> Unit)?,
    val onDelete: (() -> Unit)?,
)

/**
 * One entry: the `dd.mm.yyyy` header with a pen icon at the right edge, then the editable text.
 * The pen opens "Change date" and "Delete" (owner decision, replaces the long-press menu in SPEC §3.3);
 * it is the only way to change an entry's date.
 */
@Composable
fun JournalEntryItem(
    item: EntryItem,
    actions: EntryActions,
    modifier: Modifier = Modifier,
    ghostLines: List<String> = emptyList(),
) {
    val palette = LocalPalette.current
    val dateText = formatDayMonthYear(item.date)
    Column(modifier.fillMaxWidth().padding(top = EntryTopPadding)) {
        // The header keeps the pen button's height even without the pen, so the text doesn't jump when it appears.
        Row(Modifier.fillMaxWidth().heightIn(min = HeaderMinHeight), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dateText,
                style = AppText.entryDate,
                color = palette.foreground,
                modifier = Modifier.weight(1f),
            )
            if (actions.onChangeDate != null && actions.onDelete != null) {
                EntryEditMenu(dateText, actions.onChangeDate, actions.onDelete)
            }
        }
        EntryEditor(
            text = item.text,
            accessibilityLabel = stringResource(R.string.journals_entry_label, dateText),
            onTextChange = actions.onTextChange,
            onFocusLost = actions.onFocusLost,
            ghostLines = ghostLines,
            modifier = Modifier.padding(bottom = EntryBottomPadding),
        )
        HorizontalDivider(color = palette.line)
    }
}

@Composable
private fun EntryEditMenu(dateText: String, onChangeDate: () -> Unit, onDelete: () -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    val palette = LocalPalette.current
    Box {
        IconButton(onClick = { isOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.journals_edit_entry, dateText),
                tint = palette.muted,
                modifier = Modifier.size(PenIconSize),
            )
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.journals_menu_change_date)) },
                onClick = {
                    isOpen = false
                    onChangeDate()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.journals_menu_delete)) },
                onClick = {
                    isOpen = false
                    onDelete()
                },
            )
        }
    }
}
