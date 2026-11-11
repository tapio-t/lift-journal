package com.example.liftjournal.ui.journals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatMonthTag
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.JournalsSearchColors
import com.example.liftjournal.ui.theme.LocalPalette
import java.time.YearMonth

private val FieldHeight = 38.dp
private val FieldShape = RoundedCornerShape(11.dp)
private val IconSize = 16.dp
private val ClearButtonSize = 26.dp
private val TouchTarget = 48.dp
private val TagHeight = 30.dp
private val TagMinTouchHeight = 48.dp

/** The search field and, while searching, the month tags (SPEC §3.2). */
@Composable
fun JournalSearchBar(
    state: JournalsUiState,
    onOpened: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onMonthTagClick: (YearMonth) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)) {
        SearchField(state.query, state.showsClearButton, onOpened, onQueryChange, onClear)
        if (state.isSearchOpen || state.monthFilter != null) {
            MonthTags(state.monthTags, state.monthFilter, onMonthTagClick)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    showClear: Boolean,
    onOpened: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val label = stringResource(R.string.journals_search_label)
    // Tags open when the user taps the field, not when Android hands it focus on launch.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) onOpened() }
    }
    Row(
        Modifier.fillMaxWidth().height(FieldHeight).background(JournalsSearchColors.field, FieldShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = JournalsSearchColors.placeholder,
            modifier = Modifier.padding(start = 11.dp, end = 7.dp).size(IconSize),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = AppText.body.copy(color = Color.White),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            interactionSource = interactionSource,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.journals_search_placeholder),
                            style = AppText.body,
                            color = JournalsSearchColors.placeholder,
                        )
                    }
                    inner()
                }
            },
        )
        if (showClear) ClearSearchButton(onClear)
    }
}

@Composable
private fun ClearSearchButton(onClear: () -> Unit) {
    val description = stringResource(R.string.journals_clear_search)
    Box(
        Modifier
            .size(TouchTarget)
            .clickable(role = Role.Button, onClick = onClear)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(ClearButtonSize).background(JournalsSearchColors.clearButton, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.Black, modifier = Modifier.size(IconSize))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthTags(months: List<YearMonth>, selected: YearMonth?, onClick: (YearMonth) -> Unit) {
    val palette = LocalPalette.current
    if (months.isEmpty()) {
        Text(
            stringResource(R.string.journals_no_months),
            style = AppText.small,
            color = palette.muted,
            modifier = Modifier.padding(top = 10.dp),
        )
        return
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        months.forEach { month -> MonthTag(month, isSelected = month == selected, onClick = { onClick(month) }) }
    }
}

@Composable
private fun MonthTag(month: YearMonth, isSelected: Boolean, onClick: () -> Unit) {
    val background = if (isSelected) Color.White else Color.Black
    val content = if (isSelected) Color.Black else Color.White
    Box(
        Modifier.heightIn(min = TagMinTouchHeight).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(TagHeight / 2),
            color = background,
            border = BorderStroke(1.dp, if (isSelected) Color.White else JournalsSearchColors.tagBorder),
            modifier = Modifier.height(TagHeight),
        ) {
            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text(formatMonthTag(month), style = AppText.chip, color = content)
            }
        }
    }
}
