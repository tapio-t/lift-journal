package com.example.liftjournal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

private val BottomBarHeight = 52.dp
private val RailWidth = 84.dp
private val RailItemHeight = 58.dp
private val IndicatorThickness = 2.dp
private const val INDICATOR_WIDTH_FRACTION = 0.4f

/** Portrait tab bar: uppercase labels with a short line above the current tab (prototype nav.tabs). */
@Composable
fun BottomTabBar(current: Destination, onSelect: (Destination) -> Unit) {
    val palette = LocalPalette.current
    Column(Modifier.background(palette.background).navigationBarsPadding()) {
        HorizontalDivider(color = palette.line)
        Row(Modifier.fillMaxWidth().height(BottomBarHeight).selectableGroup()) {
            Destination.entries.forEach { destination ->
                TabItem(
                    destination = destination,
                    selected = destination == current,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    indicatorAtTop = true,
                )
            }
        }
    }
}

/** Landscape navigation rail on the left. */
@Composable
fun SideTabRail(current: Destination, onSelect: (Destination) -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.background(palette.background).fillMaxHeight()) {
        Column(
            Modifier.width(RailWidth).fillMaxHeight().selectableGroup(),
            verticalArrangement = Arrangement.Center,
        ) {
            Destination.entries.forEach { destination ->
                TabItem(
                    destination = destination,
                    selected = destination == current,
                    onSelect = onSelect,
                    modifier = Modifier.fillMaxWidth().height(RailItemHeight),
                    indicatorAtTop = false,
                )
            }
        }
        VerticalDivider(color = palette.line)
    }
}

@Composable
private fun TabItem(
    destination: Destination,
    selected: Boolean,
    onSelect: (Destination) -> Unit,
    modifier: Modifier,
    indicatorAtTop: Boolean,
) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = { onSelect(destination) }),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            val indicator = if (indicatorAtTop) {
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth(INDICATOR_WIDTH_FRACTION)
                    .height(IndicatorThickness)
            } else {
                Modifier.align(Alignment.CenterStart)
                    .width(IndicatorThickness)
                    .fillMaxHeight(INDICATOR_WIDTH_FRACTION)
            }
            Box(indicator.background(palette.foreground))
        }
        Text(
            text = stringResource(destination.labelRes).uppercase(),
            style = AppText.tabLabel,
            color = if (selected) palette.foreground else palette.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
