package com.example.liftjournal.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

private val ChipHeight = 32.dp
private val SmallChipHeight = 28.dp
private val ChipPadding = 12.dp
private val SmallChipPadding = 10.dp
private val ChipGap = 6.dp
private val SmallChipGap = 4.dp
private val SegmentPadding = 12.dp
private val SegmentShape = RoundedCornerShape(15.dp)

/** Small chips and the period control use 12 sp bold text (prototype `.chip.sm`, `.pseg button`). */
private val SmallChipText = AppText.chip.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)

/** How a chip looks: chart tags are large with a dark border; exercise, goal and year chips are small and faint. */
enum class ChipSize { LARGE, SMALL }

/** A pill-shaped choice; the selected one is filled (prototype `.chip`). */
@Composable
fun ProgressChip(text: String, isSelected: Boolean, size: ChipSize, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val height = if (size == ChipSize.LARGE) ChipHeight else SmallChipHeight
    val border = if (size == ChipSize.LARGE || isSelected) palette.foreground else palette.faint
    val shape = RoundedCornerShape(height / 2)
    Box(
        Modifier
            // The chip stays small but its touch target is 48 dp (PLAN phase 8).
            .minimumInteractiveComponentSize()
            .height(height)
            .clip(shape)
            .background(if (isSelected) palette.foreground else palette.background)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = if (size == ChipSize.LARGE) ChipPadding else SmallChipPadding),
        contentAlignment = Alignment.Center,
    ) {
        val style = if (size == ChipSize.LARGE) AppText.chip else SmallChipText
        Text(text, style = style, color = if (isSelected) palette.background else palette.foreground, maxLines = 1)
    }
}

/**
 * A single line of chips that scrolls sideways, so a long exercise list never takes chart height.
 * [label] is the small caption in front, e.g. "EXERCISE".
 */
@Composable
fun ChipRow(
    modifier: Modifier = Modifier,
    label: String? = null,
    gap: Dp = ChipGap,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) {
            Text(label.uppercase(), style = AppText.statLabel, color = LocalPalette.current.muted)
        }
        content()
    }
}

/** Year chips sit closer together than tag chips. */
val YearChipGap: Dp = SmallChipGap

/** The `All-time | 2026 ▾` control (SPEC §5.2). */
@Composable
fun PeriodSegments(
    allTimeLabel: String,
    yearLabel: String,
    isAllTime: Boolean,
    onAllTime: () -> Unit,
    onYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .height(SmallChipHeight)
            .clip(SegmentShape)
            .border(BorderStroke(1.dp, palette.foreground), SegmentShape),
    ) {
        Segment(allTimeLabel, isSelected = isAllTime, onClick = onAllTime)
        VerticalDivider(color = palette.foreground)
        Segment(yearLabel, isSelected = !isAllTime, onClick = onYear)
    }
}

@Composable
private fun Segment(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .fillMaxHeight()
            .background(if (isSelected) palette.foreground else palette.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(horizontal = SegmentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SmallChipText, color = if (isSelected) palette.background else palette.foreground)
    }
}
