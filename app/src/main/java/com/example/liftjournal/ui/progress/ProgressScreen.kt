package com.example.liftjournal.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatCompact
import com.example.liftjournal.domain.format.formatWholeKg
import com.example.liftjournal.domain.format.shortName
import com.example.liftjournal.domain.progress.HeaderStats
import com.example.liftjournal.domain.progress.ProgressChart
import com.example.liftjournal.domain.progress.ProgressPeriod
import com.example.liftjournal.domain.progress.ProgressView
import com.example.liftjournal.domain.progress.ThirdStat
import com.example.liftjournal.ui.common.goalShortName
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

private val ScreenPaddingHorizontal = 14.dp
private val ScreenPaddingTop = 10.dp
private val ScreenPaddingBottom = 8.dp
private val SectionGap = 6.dp
private val HeaderGap = 10.dp
private val StatGap = 20.dp
private val HeaderBottomPadding = 7.dp
private val ReadoutMinHeight = 19.dp
private val NoDataCorner = 12.dp
private val NoDataDash = 4.dp
private val NoDataPadding = 16.dp

/** Progress tab, always in landscape: header, readout, chart and the chart choices (SPEC §5). */
@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)) {
    LockLandscapeWhileShown()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.isLoaded) return
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                start = ScreenPaddingHorizontal,
                end = ScreenPaddingHorizontal,
                top = ScreenPaddingTop,
                bottom = ScreenPaddingBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        ProgressHeader(state, viewModel)
        ReadoutRow(state, viewModel::onTableToggled)
        ChartArea(state, viewModel::onBarTapped, Modifier.weight(1f).fillMaxWidth())
        ViewTags(state, viewModel::onViewSelected)
        SecondaryChips(state, viewModel)
        if (state.isBodyweightMissing) {
            Text(
                stringResource(R.string.progress_bodyweight_hint),
                style = AppText.hint,
                color = LocalPalette.current.muted,
            )
        }
    }
}

@Composable
private fun ProgressHeader(state: ProgressUiState, viewModel: ProgressViewModel) {
    val palette = LocalPalette.current
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Row(
                Modifier.weight(1f).padding(bottom = HeaderBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(HeaderGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.progress_title).uppercase(),
                    style = AppText.section,
                    color = palette.foreground,
                )
                if (state.showsPeriodControl) PeriodControl(state, viewModel)
            }
            state.header?.let { HeaderStatsRow(it, Modifier.padding(bottom = HeaderBottomPadding)) }
        }
        HorizontalDivider(color = palette.foreground)
    }
}

@Composable
private fun PeriodControl(state: ProgressUiState, viewModel: ProgressViewModel) {
    val period = state.period
    val yearText = if (period is ProgressPeriod.OneYear) {
        period.year.toString()
    } else {
        stringResource(
            R.string.progress_year,
        )
    }
    PeriodSegments(
        allTimeLabel = stringResource(R.string.progress_all_time),
        yearLabel = stringResource(R.string.progress_year_button, yearText),
        isAllTime = period == ProgressPeriod.AllTime,
        onAllTime = viewModel::onAllTimeClicked,
        onYear = viewModel::onYearButtonClicked,
    )
    if (state.isYearChipsOpen) {
        ChipRow(gap = YearChipGap) {
            state.years.forEach { year ->
                ProgressChip(
                    text = year.toString(),
                    isSelected = period == ProgressPeriod.OneYear(year),
                    size = ChipSize.SMALL,
                    onClick = { viewModel.onYearPicked(year) },
                )
            }
        }
    }
}

/** Workouts, total and this month (or average per month), right-aligned (SPEC §5.1). */
@Composable
private fun HeaderStatsRow(stats: HeaderStats, modifier: Modifier) {
    val totalLabel = when (val period = stats.period) {
        is ProgressPeriod.OneYear -> stringResource(R.string.progress_stat_kg_in_year, period.year)
        ProgressPeriod.AllTime -> stringResource(R.string.progress_stat_kg_all_time)
    }
    val (thirdValue, thirdLabel) = when (val third = stats.third) {
        is ThirdStat.MonthSoFar ->
            third.kg to
                stringResource(R.string.progress_stat_kg_in_month, third.month.shortName())
        is ThirdStat.AveragePerMonth -> third.kg to stringResource(R.string.progress_stat_avg_per_month)
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(StatGap)) {
        Stat(formatWholeKg(stats.workouts.toDouble()), stringResource(R.string.progress_stat_workouts))
        Stat(formatCompact(stats.totalKg), totalLabel)
        Stat(formatCompact(thirdValue), thirdLabel)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    val palette = LocalPalette.current
    Column(horizontalAlignment = Alignment.End) {
        Text(value, style = AppText.statNumber, color = palette.foreground)
        Text(label.uppercase(), style = AppText.statLabel, color = palette.muted)
    }
}

/** Chart title, average set weight, readout, and the table toggle on the right. */
@Composable
private fun ReadoutRow(state: ProgressUiState, onTableToggled: () -> Unit) {
    val palette = LocalPalette.current
    val lines = readoutLines(state)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(lines.title, style = AppText.chartTitle, color = palette.foreground)
            lines.detail?.let { Text(it, style = AppText.readout, color = palette.foreground) }
            Text(
                text = lines.readout ?: AnnotatedString(""),
                style = AppText.readout,
                color = palette.muted,
                modifier = Modifier
                    .heightIn(min = ReadoutMinHeight)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        val toggleText =
            stringResource(if (state.showsTable) R.string.progress_show_chart else R.string.progress_show_table)
        ProgressChip(toggleText, isSelected = false, size = ChipSize.SMALL, onClick = onTableToggled)
    }
}

/** The three text lines above the chart. [detail] is the average-set-weight or "Now" line. */
private class ReadoutLines(val title: String, val detail: AnnotatedString?, val readout: AnnotatedString?)

@Composable
private fun readoutLines(state: ProgressUiState): ReadoutLines {
    val goal = state.goal
    if (goal != null) return ReadoutLines(goalTitle(goal), goalNowLine(goal), goalRemainingLine(goal))
    val chart = state.chart ?: return ReadoutLines("", null, null)
    return ReadoutLines(
        chartTitle(chart),
        averageSetWeightLine(chart),
        readoutText(chart, state.selectedBar, state.today),
    )
}

@Composable
private fun ChartArea(state: ProgressUiState, onBarTap: (Int) -> Unit, modifier: Modifier) {
    Box(modifier) {
        val goal = state.goal
        val chart = state.chart
        when {
            goal != null && state.showsTable -> DataTable(goalTable(goal), Modifier.fillMaxSize())
            goal != null -> GoalView(goal, Modifier.fillMaxSize())
            chart != null -> ChartOrTable(chart, state, onBarTap)
        }
    }
}

@Composable
private fun ChartOrTable(chart: ProgressChart, state: ProgressUiState, onBarTap: (Int) -> Unit) {
    val message = noDataMessage(chart)
    val model = barChartModel(chart)
    val table = chartTable(chart)
    when {
        message != null -> NoData(message)
        state.showsTable && table != null -> DataTable(table, Modifier.fillMaxSize())
        model != null -> BarChart(model, state.selectedBar, onBarTap, Modifier.fillMaxSize())
    }
}

/** A dashed box with a hint, in place of an empty chart. */
@Composable
private fun NoData(message: String) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val dash = NoDataDash.toPx()
                drawRoundRect(
                    color = palette.faint,
                    cornerRadius = CornerRadius(NoDataCorner.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    ),
                )
            }
            .padding(NoDataPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = AppText.body, color = palette.muted, textAlign = TextAlign.Center)
    }
}

/** `Goal` (when goals exist), `Total volume / month`, `Top set / week`, `Exercise volume / month`. */
@Composable
private fun ViewTags(state: ProgressUiState, onSelect: (ProgressView) -> Unit) {
    ChipRow {
        state.views.forEach { view ->
            ProgressChip(viewLabel(view), isSelected = view == state.view, size = ChipSize.LARGE, onClick = {
                onSelect(view)
            })
        }
    }
}

@Composable
private fun viewLabel(view: ProgressView): String = stringResource(
    when (view) {
        ProgressView.GOAL -> R.string.progress_view_goal
        ProgressView.TOTAL_VOLUME -> R.string.progress_view_total
        ProgressView.TOP_SET -> R.string.progress_view_top
        ProgressView.EXERCISE_VOLUME -> R.string.progress_view_exercise
    },
)

/** Exercise chips in exercise views, goal chips in the goal view when there are several goals. */
@Composable
private fun SecondaryChips(state: ProgressUiState, viewModel: ProgressViewModel) {
    if (state.showsGoalChips) {
        ChipRow(label = stringResource(R.string.progress_chips_goal)) {
            state.goals.forEachIndexed { index, goal ->
                ProgressChip(
                    text = goalShortName(goal),
                    isSelected = index == state.selectedGoalIndex,
                    size = ChipSize.SMALL,
                    onClick = { viewModel.onGoalSelected(index) },
                )
            }
        }
    }
    if (state.showsExerciseChips) {
        ChipRow(label = stringResource(R.string.progress_chips_exercise)) {
            state.exercises.forEach { exercise ->
                ProgressChip(
                    text = exercise.name,
                    isSelected = exercise.key == state.selectedExerciseKey,
                    size = ChipSize.SMALL,
                    onClick = { viewModel.onExerciseSelected(exercise.key) },
                )
            }
        }
    }
}
