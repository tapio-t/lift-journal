package com.example.liftjournal.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatCompact
import com.example.liftjournal.domain.stats.goalScale
import com.example.liftjournal.ui.common.goalBasisText
import com.example.liftjournal.ui.common.goalFullName
import com.example.liftjournal.ui.common.goalValueText
import com.example.liftjournal.ui.common.styledStringResource
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette
import com.example.liftjournal.ui.theme.Palette

/** The percentage takes 40% of the width; the Now vs Goal chart the rest (prototype `.gpct`). */
private const val PERCENT_SHARE = 0.4f
private val GoalViewGap = 12.dp
private val PercentLabelGap = 8.dp
private val GoalChartPaddingLeft = 44.dp
private const val GOAL_BAR_SLOT_SHARE = 0.5f
private val MaxGoalBarWidth = 64.dp
private val GoalBarRadius = 4.dp
private val GoalOutlineWidth = 2.dp
private val TargetDash = 4.dp

/** "Goal · Squat top set 160 kg". */
@Composable
@ReadOnlyComposable
fun goalTitle(panel: GoalPanel): String = stringResource(R.string.goal_title, goalFullName(panel.goal))

/** "Now 145 kg · heaviest set in the last 4 weeks". */
@Composable
@ReadOnlyComposable
fun goalNowLine(panel: GoalPanel): AnnotatedString = styledStringResource(
    R.string.goal_now_line,
    goalValueText(panel.goal.goal, panel.progress.currentKg),
    goalBasisText(panel.progress.basis),
)

/** "15 kg to go" or "Goal reached", plus a note when bodyweight sets were skipped. */
@Composable
@ReadOnlyComposable
fun goalRemainingLine(panel: GoalPanel): AnnotatedString {
    val goal = panel.goal.goal
    val remainingKg = goal.targetKg - panel.progress.currentKg
    val line = if (remainingKg > 0) {
        styledStringResource(R.string.goal_to_go, goalValueText(goal, remainingKg))
    } else {
        styledStringResource(R.string.goal_reached)
    }
    if (!panel.showsBodyweightWarning) return line
    return line + AnnotatedString(stringResource(R.string.goal_bodyweight_not_counted))
}

/** Large percentage on the left, Now vs Goal bars on the right (SPEC §5.6). */
@Composable
fun GoalView(panel: GoalPanel, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(GoalViewGap)) {
        GoalPercent(panel.percent, Modifier.weight(PERCENT_SHARE).fillMaxHeight())
        GoalChart(panel, Modifier.weight(1 - PERCENT_SHARE).fillMaxHeight())
    }
}

@Composable
private fun GoalPercent(percent: Int, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$percent%", style = AppText.goalPercent, color = palette.foreground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(PercentLabelGap))
        Text(stringResource(R.string.goal_of_goal), style = AppText.section, color = palette.muted)
    }
}

@Composable
private fun GoalChart(panel: GoalPanel, modifier: Modifier) {
    val goal = panel.goal.goal
    val nowText = goalValueText(goal, panel.progress.currentKg)
    val goalText = goalValueText(goal, goal.targetKg)
    val labels = GoalChartLabels(
        now = stringResource(R.string.goal_now),
        goal = stringResource(R.string.goal_goal),
        nowValue = nowText,
        goalValue = goalText,
    )
    val description = stringResource(R.string.goal_chart_description, nowText, goalText)
    val measurer = rememberTextMeasurer()
    val palette = LocalPalette.current
    Canvas(modifier.fillMaxSize().semantics { contentDescription = description }) {
        drawGoalChart(panel.progress.currentKg, goal.targetKg, labels, measurer, palette)
    }
}

private class GoalChartLabels(val now: String, val goal: String, val nowValue: String, val goalValue: String)

/** Zero-based: a solid Now bar, an outlined Goal bar and a dashed line at the target (prototype `goalSvg`). */
private fun DrawScope.drawGoalChart(
    currentKg: Double,
    targetKg: Double,
    labels: GoalChartLabels,
    measurer: TextMeasurer,
    palette: Palette,
) {
    val plotLeft = GoalChartPaddingLeft.toPx()
    val plotRight = size.width - ChartPaddingRight.toPx()
    val plotTop = ChartPaddingTop.toPx()
    val baseY = size.height - ChartPaddingBottom.toPx()
    val scale = goalScale(currentKg, targetKg)
    val y = { kg: Double -> baseY - (kg / scale.high).toFloat() * (baseY - plotTop) }
    val axisStyle = AppText.chartAxis.copy(color = palette.muted)
    drawGridlines(measurer, scale, y, plotLeft, axisStyle, palette.line, ::formatCompact)

    val slot = (plotRight - plotLeft) / 2
    val barWidth = minOf(MaxGoalBarWidth.toPx(), slot * GOAL_BAR_SLOT_SHARE)
    val bars = listOf(
        Triple(currentKg, labels.now, labels.nowValue),
        Triple(targetKg, labels.goal, labels.goalValue),
    )
    bars.forEachIndexed { index, (kg, name, value) ->
        val centerX = plotLeft + slot * index + slot / 2
        val top = y(kg)
        val isGoal = index == 1
        if (kg > 0) {
            val style = if (isGoal) Stroke(width = GoalOutlineWidth.toPx()) else Fill
            drawBar(centerX - barWidth / 2, barWidth, top, baseY, GoalBarRadius.toPx(), palette.foreground, style)
        }
        drawValueLabel(measurer, value, AppText.chartValue.copy(color = palette.foreground), centerX, top)
        val nameStyle = AppText.chartAxis.copy(color = palette.foreground, fontWeight = FontWeight.ExtraBold)
        drawBottomLabel(measurer, name, nameStyle, centerX, baseY, TextAnchor.CENTER)
    }

    val dash = TargetDash.toPx()
    drawLine(
        palette.foreground,
        Offset(plotLeft, y(targetKg)),
        Offset(plotRight, y(targetKg)),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
    )
    drawBaseLine(plotLeft, baseY, palette.foreground)
}

/** Now and Goal as a table. */
@Composable
@ReadOnlyComposable
fun goalTable(panel: GoalPanel): TableData {
    val goal = panel.goal.goal
    return TableData(
        headers = listOf(
            "",
            stringResource(R.string.progress_table_kg),
            stringResource(R.string.progress_table_percent),
        ),
        rows = listOf(
            listOf(
                stringResource(R.string.goal_now),
                goalValueText(goal, panel.progress.currentKg),
                "${panel.percent}%",
            ),
            listOf(stringResource(R.string.goal_goal), goalValueText(goal, goal.targetKg), "100%"),
        ),
    )
}
