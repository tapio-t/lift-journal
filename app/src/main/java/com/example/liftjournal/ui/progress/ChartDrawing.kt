package com.example.liftjournal.ui.progress

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.example.liftjournal.domain.stats.AxisScale

/** Chart margins, in the prototype's CSS pixels (= dp). */
internal val ChartPaddingTop = 20.dp
internal val ChartPaddingBottom = 22.dp
internal val ChartPaddingRight = 8.dp

/** Space between the axis labels' right edge and the plot. */
internal val AxisLabelGap = 8.dp

/** Space between a bar's top and its value label. */
private val ValueLabelGap = 2.dp

/** Space between the base line and the labels under it. */
private val BottomLabelGap = 4.dp

/** Where a text's x coordinate is anchored. */
internal enum class TextAnchor { START, CENTER, END }

/** Draws [text] with its vertical centre at [centerY]. */
internal fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    centerY: Float,
    anchor: TextAnchor,
) {
    val layout = measurer.measure(text, style)
    val left = when (anchor) {
        TextAnchor.START -> x
        TextAnchor.CENTER -> x - layout.size.width / 2f
        TextAnchor.END -> x - layout.size.width
    }
    drawText(layout, topLeft = Offset(left, centerY - layout.size.height / 2f))
}

/** Draws [text] centred on [centerX] with its bottom just above [barTop] (the value over a bar). */
internal fun DrawScope.drawValueLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    centerX: Float,
    barTop: Float,
) {
    val layout = measurer.measure(text, style)
    val topLeft = Offset(centerX - layout.size.width / 2f, barTop - ValueLabelGap.toPx() - layout.size.height)
    drawText(layout, topLeft = topLeft)
}

/** Draws [text] under the base line; [x] is its centre or its start depending on [anchor]. */
internal fun DrawScope.drawBottomLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    baseY: Float,
    anchor: TextAnchor,
) {
    val layout = measurer.measure(text, style)
    val left = if (anchor == TextAnchor.CENTER) x - layout.size.width / 2f else x
    drawText(layout, topLeft = Offset(left, baseY + BottomLabelGap.toPx()))
}

/** Horizontal gridlines with their values right-aligned left of [plotLeft]. */
internal fun DrawScope.drawGridlines(
    measurer: TextMeasurer,
    scale: AxisScale,
    y: (Double) -> Float,
    plotLeft: Float,
    style: TextStyle,
    lineColor: Color,
    label: (Double) -> String,
) {
    val plotRight = size.width - ChartPaddingRight.toPx()
    scale.gridlines().forEach { value ->
        drawLine(lineColor, Offset(plotLeft, y(value)), Offset(plotRight, y(value)), strokeWidth = 1.dp.toPx())
        drawLabel(measurer, label(value), style, plotLeft - AxisLabelGap.toPx(), y(value), TextAnchor.END)
    }
}

/** The solid line along the bottom of the plot. */
internal fun DrawScope.drawBaseLine(plotLeft: Float, baseY: Float, color: Color) {
    val plotRight = size.width - ChartPaddingRight.toPx()
    drawLine(color, Offset(plotLeft, baseY), Offset(plotRight, baseY), strokeWidth = 1.dp.toPx())
}

/** A bar from [bottom] up to [top] with rounded top corners, filled or outlined. */
internal fun DrawScope.drawBar(
    left: Float,
    width: Float,
    top: Float,
    bottom: Float,
    cornerRadius: Float,
    color: Color,
    style: DrawStyle = Fill,
) {
    val radius = CornerRadius(cornerRadius.coerceIn(0f, minOf(width / 2, bottom - top).coerceAtLeast(0f)))
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = bottom,
                topLeftCornerRadius = radius,
                topRightCornerRadius = radius,
            ),
        )
    }
    drawPath(path, color, style = style)
}
