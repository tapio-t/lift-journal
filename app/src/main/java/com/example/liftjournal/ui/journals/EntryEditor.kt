package com.example.liftjournal.ui.journals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import com.example.liftjournal.domain.journal.acceptGhostLine
import com.example.liftjournal.domain.journal.ghostOverlay
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.JournalsSearchColors
import com.example.liftjournal.ui.theme.LocalPalette

/**
 * An entry's text, editable in place. The top entry can show [ghostLines] in grey inside the field
 * itself; typed characters cover ghost characters one by one and Tab accepts the rest of a line (SPEC §3.4).
 */
@Composable
fun EntryEditor(
    text: String,
    accessibilityLabel: String,
    onTextChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
    ghostLines: List<String> = emptyList(),
) {
    var value by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    var isFocused by remember { mutableStateOf(false) }
    // Outside changes (undo, merge, another date) replace the text, but never while the user is typing in it.
    LaunchedEffect(text) {
        if (!isFocused && value.text != text) value = TextFieldValue(text, TextRange(text.length))
    }
    val palette = LocalPalette.current
    val onValueChange: (TextFieldValue) -> Unit = { newValue ->
        val textChanged = newValue.text != value.text
        value = newValue
        if (textChanged) onTextChange(newValue.text)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = AppText.entryText.copy(color = palette.foreground),
        cursorBrush = SolidColor(palette.foreground),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (isFocused && !state.isFocused) onFocusLost()
                isFocused = state.isFocused
            }
            .onPreviewKeyEvent { event ->
                val isTab = event.key == Key.Tab && event.type == KeyEventType.KeyDown && !event.isShiftPressed
                if (!isTab) return@onPreviewKeyEvent false
                val accepted =
                    acceptGhostLine(value.text, value.selection.start, ghostLines) ?: return@onPreviewKeyEvent false
                onValueChange(TextFieldValue(accepted.text, TextRange(accepted.cursor)))
                true
            }
            .semantics { contentDescription = accessibilityLabel },
        decorationBox = { innerTextField ->
            Box {
                if (ghostLines.isNotEmpty()) GhostText(value.text, ghostLines)
                innerTextField()
            }
        },
    )
}

/** Grey ghost text behind the field; the typed part is drawn invisible so the remainder lines up. */
@Composable
private fun GhostText(typedText: String, ghostLines: List<String>) {
    Text(
        text = ghostAnnotatedText(typedText, ghostLines),
        style = AppText.entryText.copy(color = JournalsSearchColors.ghostText),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun ghostAnnotatedText(typedText: String, ghostLines: List<String>): AnnotatedString = buildAnnotatedString {
    ghostOverlay(typedText, ghostLines).forEachIndexed { index, line ->
        if (index > 0) append('\n')
        withStyle(SpanStyle(color = Color.Transparent)) { append(line.typed) }
        append(line.remainder)
    }
}
