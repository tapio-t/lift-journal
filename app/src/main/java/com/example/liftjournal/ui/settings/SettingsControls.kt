package com.example.liftjournal.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

/** Buttons, fields and segments are at least 48 dp tall so they are easy to hit (PLAN phase 8). */
private val ControlHeight = 48.dp
private val ControlShape = RoundedCornerShape(10.dp)
private val BoxShape = RoundedCornerShape(12.dp)
private val ControlPadding = 12.dp
private val ButtonPadding = 16.dp
private val SectionTopGap = 22.dp
private val SectionBottomGap = 8.dp
private val HintTopGap = 6.dp
private val LabelBottomGap = 5.dp
private const val DISABLED_ALPHA = 0.35f
private val FieldText = AppText.body.copy(fontSize = 16.sp)
private val SegmentText = AppText.chip.copy(fontWeight = FontWeight.Bold)

/** An uppercase grey section title (prototype `.section`). */
@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = AppText.section,
        color = LocalPalette.current.muted,
        modifier = Modifier.padding(top = SectionTopGap, bottom = SectionBottomGap).semantics { heading() },
    )
}

/** Small grey explanation under a control (prototype `.hint`). */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = AppText.hint, color = LocalPalette.current.muted, modifier = modifier.padding(top = HintTopGap))
}

/** A small uppercase label above a form field (prototype `.gform .lbl`). */
@Composable
fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        style = AppText.statLabel,
        color = LocalPalette.current.muted,
        modifier = Modifier.padding(bottom = LabelBottomGap),
    )
}

/** A thin rounded frame around a group of controls (prototype `.box`). */
@Composable
fun FramedBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, LocalPalette.current.line), BoxShape)
            .padding(ControlPadding),
        content = content,
    )
}

/** An outlined button; [isPrimary] fills it (prototype `.btn`, `.btn.primary`). */
@Composable
fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    Box(
        modifier
            .heightIn(min = ControlHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(ControlShape)
            .background(if (isPrimary) palette.foreground else palette.background)
            .border(BorderStroke(1.dp, palette.foreground), ControlShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.button, color = if (isPrimary) palette.background else palette.foreground)
    }
}

/** A full-width row of equal choices with the selected one filled (prototype `.seg`). */
@Composable
fun <T> ChoiceSegments(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ControlHeight)
            .clip(ControlShape)
            .border(BorderStroke(1.dp, palette.foreground), ControlShape),
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) VerticalDivider(color = palette.foreground)
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = ControlHeight)
                    .background(if (isSelected) palette.foreground else palette.background)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .semantics { this.selected = isSelected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = SegmentText,
                    color = if (isSelected) palette.background else palette.foreground,
                )
            }
        }
    }
}

/** A select box: shows the chosen option and opens a menu of all options (prototype `<select>`). */
@Composable
fun <T> Dropdown(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    accessibilityLabel: String,
) {
    var isOpen by remember { mutableStateOf(false) }
    val palette = LocalPalette.current
    val selectedLabel = optionLabel(selected)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = ControlHeight)
                .clip(ControlShape)
                .border(BorderStroke(1.dp, palette.faint), ControlShape)
                .clickable(role = Role.DropdownList) { isOpen = true }
                .semantics(mergeDescendants = true) { contentDescription = "$accessibilityLabel, $selectedLabel" }
                .padding(horizontal = ControlPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(selectedLabel, style = FieldText, color = palette.foreground)
            Text("▾", style = FieldText, color = palette.foreground)
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = FieldText) },
                    onClick = {
                        isOpen = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * A decimal number field with a placeholder (prototype `input[type=number]`). The keyboard's Done key leaves the
 * field, which closes the keyboard. [onDone] runs when the field loses focus, so a value is saved without an extra
 * button.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val palette = LocalPalette.current
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = FieldText.copy(color = palette.foreground),
        cursorBrush = SolidColor(palette.foreground),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .onFocusChanged { focus ->
                if (isFocused && !focus.isFocused) onDone()
                isFocused = focus.isFocused
            }
            .semantics { contentDescription = accessibilityLabel },
        decorationBox = { field ->
            Box(
                Modifier
                    .heightIn(min = ControlHeight)
                    .border(BorderStroke(1.dp, if (isFocused) palette.foreground else palette.faint), ControlShape)
                    .padding(horizontal = ControlPadding),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = FieldText, color = palette.faint)
                field()
            }
        },
    )
}
