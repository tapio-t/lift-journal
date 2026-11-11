package com.example.liftjournal.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.liftjournal.domain.stats.AxisScale
import com.example.liftjournal.domain.stats.linearScale
import com.example.liftjournal.domain.stats.zoomedScale
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette
import com.example.liftjournal.ui.theme.Palette

/** One bar as drawn. [axisLabel] is null for weeks that don't start a month. */
data class ChartBar(
    val value: Double,
    val valueLabel: String,
    val axisLabel: String?,
    val narrowAxisLabel: String?,
    val description: String,
    val isCurrent: Boolean,
)

/**
 * A bar chart ready to draw. [isZoomed] charts (top sets) use the zoomed weight scale and wide bars;
 * [isWeekly] charts have month separators and left-aligned month labels (SPEC §5.3).
 */
class BarChartModel(
    val title: String,
    val bars: List<ChartBar>,
    val isZoomed: Boolean,
    val isWeekly: Boolean,
    val separators: List<Int>,
    val axisLabel: (Double) -> String,
)

private val ChartPaddingLeft = 42.dp

/** The bottom stub that stands for 0 → low on a zoomed scale. */
private val ZoomStubHeight = 18.dp

/** Under this slot width, month labels shrink to one letter. */
private val NarrowSlotWidth = 30.dp
private val MaxZoomedBarWidth = 44.dp
private val ZoomedBarGap = 2.dp
private val MaxBarWidth = 24.dp
private val MinBarWidth = 2.dp
private val WideSlotWidth = 14.dp
private const val WIDE_SLOT_BAR_SHARE = 0.62f
private const val NARROW_SLOT_BAR_SHARE = 0.7f
private val WeekBarRadius = 2.dp
private val BarRadius = 4.dp
private val SeparatorOverhang = 6.dp
private val WeekLabelInset = 3.dp
private val ValueLabelEdgeMargin = 12.dp
private val ScaleBreakInset = 6.dp
private val ScaleBreakWidth = 8.dp
private val ScaleBreakRise = 5.dp
private val ScaleBreakStroke = 1.2.dp
private val ScaleBreakOffsets = listOf(3.dp, 7.dp)

/**
 * A bar chart drawn on a Canvas. Each bar has an invisible tap target over its whole slot, which also
 * carries the bar's description for screen readers (prototype `barSvg` hit rectangles).
 */
@Composable
fun BarChart(model: BarChartModel, selectedBar: Int?, onBarTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val palette = LocalPalette.current
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = model.title }) {
            drawBarChart(model, selectedBar, measurer, palette)
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = ChartPaddingLeft, end = ChartPaddingRight, top = ChartPaddingTop),
        ) {
            model.bars.forEachIndexed { index, bar ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = null, indication = null, role = Role.Button) { onBarTap(index) }
                        .semantics {
                            contentDescription = bar.description
                            selected = index == selectedBar
                        },
                )
            }
        }
    }
}

/** Positions of the plot and its bars, in pixels. */
private class BarGeometry(size: Size, density: Density, model: BarChartModel) {
    val plotLeft = with(density) { ChartPaddingLeft.toPx() }
    val plotTop = with(density) { ChartPaddingTop.toPx() }
    val baseY = size.height - with(density) { ChartPaddingBottom.toPx() }
    val slot = (size.width - plotLeft - with(density) { ChartPaddingRight.toPx() }) / model.bars.size
    private val stub = with(density) { ZoomStubHeight.toPx() }

    // The zoomed scale counts gridlines per CSS pixel in the prototype, which is a dp here.
    val scale: AxisScale = if (model.isZoomed) {
        zoomedScale(model.bars.map { it.value }, (baseY - stub - plotTop) / density.density)
    } else {
        linearScale(model.bars.map { it.value })
    }
    val zoomBaseY = if (scale.isZoomed) baseY - stub else baseY
    val stubTopY = baseY - stub
    val isNarrow = slot < with(density) { NarrowSlotWidth.toPx() }
    val barWidth = with(density) {
        if (model.isZoomed) {
            (slot - ZoomedBarGap.toPx()).coerceIn(MinBarWidth.toPx(), MaxZoomedBarWidth.toPx())
        } else {
            val share = if (slot > WideSlotWidth.toPx()) WIDE_SLOT_BAR_SHARE else NARROW_SLOT_BAR_SHARE
            (slot * share).coerceIn(MinBarWidth.toPx(), MaxBarWidth.toPx())
        }
    }

    fun y(value: Double): Float {
        val share = ((value - scale.low) / (scale.high - scale.low)).toFloat()
        return zoomBaseY - share * (zoomBaseY - plotTop)
    }

    /** Load-off weeks below the zoomed range reach only the top of the stub (SPEC §5.3). */
    fun barTop(value: Double): Float = if (scale.isZoomed && value <= scale.low) stubTopY else y(value)

    fun slotLeft(index: Int): Float = plotLeft + slot * index

    fun centerX(index: Int): Float = slotLeft(index) + slot / 2
}

private fun DrawScope.drawBarChart(model: BarChartModel, selectedBar: Int?, measurer: TextMeasurer, palette: Palette) {
    if (model.bars.isEmpty()) return
    val geometry = BarGeometry(size, this, model)
    val axisStyle = AppText.chartAxis.copy(color = palette.muted)
    drawGridlines(measurer, geometry.scale, geometry::y, geometry.plotLeft, axisStyle, palette.line, model.axisLabel)
    if (geometry.scale.isZoomed) {
        drawLabel(measurer, "0", axisStyle, geometry.plotLeft - AxisLabelGap.toPx(), geometry.baseY, TextAnchor.END)
    }
    drawSeparators(model.separators, geometry, palette)
    drawBars(model, selectedBar, geometry, measurer, palette)
    drawBaseLine(geometry.plotLeft, geometry.baseY, palette.foreground)
    if (geometry.scale.isZoomed) drawScaleBreak(geometry, palette)
}

/** Thin vertical lines between months in the weekly chart. */
private fun DrawScope.drawSeparators(separators: List<Int>, geometry: BarGeometry, palette: Palette) {
    val overhang = SeparatorOverhang.toPx()
    separators.forEach { index ->
        val x = geometry.slotLeft(index)
        drawLine(
            palette.faint,
            Offset(x, geometry.plotTop - overhang),
            Offset(x, geometry.baseY + overhang),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun DrawScope.drawBars(
    model: BarChartModel,
    selectedBar: Int?,
    geometry: BarGeometry,
    measurer: TextMeasurer,
    palette: Palette,
) {
    // With nothing selected, the tallest bar carries the value label (SPEC §5.5).
    val labelled = selectedBar ?: model.bars.indices.maxByOrNull { model.bars[it].value }
    val radius = (if (model.isWeekly) WeekBarRadius else BarRadius).toPx()
    val valueStyle = AppText.chartValue.copy(color = palette.foreground)
    val edge = ValueLabelEdgeMargin.toPx()
    model.bars.forEachIndexed { index, bar ->
        val centerX = geometry.centerX(index)
        if (bar.value > 0) {
            val top = geometry.barTop(bar.value)
            val color = if (selectedBar != null && selectedBar != index) palette.dim else palette.foreground
            drawBar(centerX - geometry.barWidth / 2, geometry.barWidth, top, geometry.baseY, radius, color)
            if (index == labelled) {
                val labelX = centerX.coerceIn(geometry.plotLeft + edge, size.width - ChartPaddingRight.toPx() - edge)
                drawValueLabel(measurer, bar.valueLabel, valueStyle, labelX, top)
            }
        }
        drawAxisLabel(model, bar, index, selectedBar, geometry, measurer, palette)
    }
}

/** Month and year labels are centred, and bold when selected or current; week labels start at their slot. */
private fun DrawScope.drawAxisLabel(
    model: BarChartModel,
    bar: ChartBar,
    index: Int,
    selectedBar: Int?,
    geometry: BarGeometry,
    measurer: TextMeasurer,
    palette: Palette,
) {
    val label = (if (geometry.isNarrow) bar.narrowAxisLabel else bar.axisLabel) ?: return
    if (model.isWeekly) {
        val style = AppText.chartAxis.copy(color = palette.muted)
        val x = geometry.slotLeft(index) + WeekLabelInset.toPx()
        drawBottomLabel(measurer, label, style, x, geometry.baseY, TextAnchor.START)
        return
    }
    val isEmphasised = index == selectedBar || (selectedBar == null && bar.isCurrent)
    val style = if (isEmphasised) {
        AppText.chartAxis.copy(color = palette.foreground, fontWeight = FontWeight.ExtraBold)
    } else {
        AppText.chartAxis.copy(color = palette.muted)
    }
    drawBottomLabel(measurer, label, style, geometry.centerX(index), geometry.baseY, TextAnchor.CENTER)
}

/** The small `//` on the axis where the zoomed range starts: two short slanted strokes. */
private fun DrawScope.drawScaleBreak(geometry: BarGeometry, palette: Palette) {
    val x = geometry.plotLeft - ScaleBreakInset.toPx()
    val stroke = ScaleBreakStroke.toPx()
    ScaleBreakOffsets.forEach { offset ->
        val y = geometry.stubTopY + offset.toPx()
        val end = Offset(x + ScaleBreakWidth.toPx(), y - ScaleBreakRise.toPx())
        drawLine(palette.foreground, Offset(x, y), end, strokeWidth = stroke)
    }
}
