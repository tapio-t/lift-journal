package com.example.liftjournal.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatCompact
import com.example.liftjournal.domain.format.formatLongDate
import com.example.liftjournal.domain.format.formatWeekRange
import com.example.liftjournal.domain.format.formatWeight
import com.example.liftjournal.domain.format.formatWholeKg
import com.example.liftjournal.domain.format.longName
import com.example.liftjournal.domain.format.shortName
import com.example.liftjournal.domain.progress.AllTimeSummary
import com.example.liftjournal.domain.progress.AverageSetWeight
import com.example.liftjournal.domain.progress.ProgressChart
import com.example.liftjournal.domain.progress.ProgressView
import com.example.liftjournal.domain.progress.YearBar
import com.example.liftjournal.domain.progress.YearChartKind
import com.example.liftjournal.domain.progress.hasData
import com.example.liftjournal.domain.stats.TopSet
import com.example.liftjournal.ui.common.styledStringResource
import java.time.LocalDate

/** Volume axes switch to short numbers once any value reaches this (prototype `kgAxis`). */
private const val COMPACT_AXIS_FROM_KG = 10_000.0

/** The chart title (SPEC §5.1). */
@Composable
@ReadOnlyComposable
fun chartTitle(chart: ProgressChart): String = when (chart) {
    is ProgressChart.NoExercise -> stringResource(
        when (chart.view) {
            ProgressView.TOP_SET -> R.string.progress_title_none_top
            ProgressView.EXERCISE_VOLUME -> R.string.progress_title_none_exercise
            else -> R.string.progress_title_none_total
        },
    )
    is ProgressChart.Months -> chart.exerciseName?.let { stringResource(R.string.progress_title_exercise_year, it) }
        ?: stringResource(R.string.progress_title_total_year)
    is ProgressChart.Weeks -> stringResource(R.string.progress_title_top_year, chart.exerciseName)
    is ProgressChart.Years -> when (chart.kind) {
        YearChartKind.TOTAL_VOLUME -> stringResource(R.string.progress_title_total_all)
        YearChartKind.EXERCISE_VOLUME -> stringResource(
            R.string.progress_title_exercise_all,
            chart.exerciseName.orEmpty(),
        )
        YearChartKind.TOP_SET -> stringResource(R.string.progress_title_top_all, chart.exerciseName.orEmpty())
    }
}

/** "Average set weight 94.2 kg · 202 sets in 2026", shown in top-set views only (SPEC §5.3). */
@Composable
@ReadOnlyComposable
fun averageSetWeightLine(chart: ProgressChart): AnnotatedString? {
    val (average, year) = when (chart) {
        is ProgressChart.Weeks -> chart.averageSetWeight to chart.year
        is ProgressChart.Years -> chart.averageSetWeight to null
        else -> null to null
    }
    if (average == null) return null
    return styledStringResource(R.string.progress_average_set_weight, formatWeight(average.kg), setsText(average, year))
}

@Composable
@ReadOnlyComposable
private fun setsText(average: AverageSetWeight, year: Int?): String {
    val sets = pluralStringResource(R.plurals.progress_sets, average.sets, average.sets)
    return if (year == null) {
        stringResource(R.string.progress_sets_all_time, sets)
    } else {
        stringResource(R.string.progress_sets_in_year, sets, year)
    }
}

/** The message shown instead of an empty chart, or null when there is something to draw. */
@Composable
@ReadOnlyComposable
fun noDataMessage(chart: ProgressChart): String? = when {
    chart is ProgressChart.NoExercise -> stringResource(R.string.progress_no_exercise)
    chart.hasData -> null
    chart is ProgressChart.Months -> stringResource(R.string.progress_no_sets_in_year, chart.year)
    chart is ProgressChart.Weeks -> stringResource(R.string.progress_no_sets_in_year, chart.year)
    else -> stringResource(R.string.progress_no_sets_yet)
}

/** The line under the title: details of the selected bar, or a summary of the chart (SPEC §5.5). */
@Composable
@ReadOnlyComposable
fun readoutText(chart: ProgressChart, selectedBar: Int?, today: LocalDate): AnnotatedString? = when (chart) {
    is ProgressChart.NoExercise -> null
    is ProgressChart.Months -> monthsReadout(chart, selectedBar)
    is ProgressChart.Weeks -> weeksReadout(chart, selectedBar, today)
    is ProgressChart.Years -> yearsReadout(chart, selectedBar)
}

@Composable
@ReadOnlyComposable
private fun monthsReadout(chart: ProgressChart.Months, selectedBar: Int?): AnnotatedString {
    val bar = selectedBar?.let(chart.bars::getOrNull)
    if (bar == null) {
        val period = if (chart.isCurrentYear) {
            stringResource(R.string.progress_year_so_far, chart.year)
        } else {
            chart.year.toString()
        }
        return styledStringResource(R.string.progress_readout_months_summary, period, formatWholeKg(chart.totalKg))
    }
    val monthName = bar.month.longName()
    val month = if (chart.isCurrent(
            selectedBar,
        )
    ) {
        stringResource(R.string.progress_month_so_far, monthName)
    } else {
        monthName
    }
    val count = monthCountText(chart, bar.count)
    return styledStringResource(R.string.progress_readout_month, month, formatWholeKg(bar.volumeKg), count)
}

/** Total volume counts workouts; exercise volume counts sets. */
@Composable
@ReadOnlyComposable
private fun monthCountText(chart: ProgressChart.Months, count: Int): String {
    val plural = if (chart.exerciseName == null) R.plurals.progress_workouts else R.plurals.progress_sets
    return pluralStringResource(plural, count, count)
}

@Composable
@ReadOnlyComposable
private fun weeksReadout(chart: ProgressChart.Weeks, selectedBar: Int?, today: LocalDate): AnnotatedString {
    val selected = selectedBar?.let(chart.bars::getOrNull)
    val top = selected?.top
    if (selected != null && top != null) {
        return styledStringResource(
            R.string.progress_readout_week,
            selected.isoWeek,
            formatWeekRange(selected.monday),
            weightRepsText(top),
            formatLongDate(top.date, today),
        )
    }
    val best = chart.best?.let(chart.bars::get)
    val latest = chart.latest?.let(chart.bars::get)
    val bestTop = best?.top
    val latestTop = latest?.top
    if (bestTop == null || latestTop == null) {
        return AnnotatedString(stringResource(R.string.progress_readout_weeks_none, chart.year))
    }
    return styledStringResource(
        R.string.progress_readout_weeks_summary,
        weightRepsText(bestTop),
        best.isoWeek,
        weightRepsText(latestTop),
        latest.isoWeek,
    )
}

@Composable
@ReadOnlyComposable
private fun yearsReadout(chart: ProgressChart.Years, selectedBar: Int?): AnnotatedString {
    val bar = selectedBar?.let(chart.bars::getOrNull) ?: return yearsSummary(chart.summary)
    val details = yearBarDetails(chart.kind, bar)
    if (chart.kind == YearChartKind.TOP_SET) {
        return styledStringResource(R.string.progress_readout_year_top, bar.year, formatWeight(bar.valueKg), details)
    }
    return styledStringResource(R.string.progress_readout_year_volume, bar.year, formatWholeKg(bar.valueKg), details)
}

/** `12 workouts in 10 months`, `40 sets in 12 months` or `30 weeks trained`. */
@Composable
@ReadOnlyComposable
fun yearBarDetails(kind: YearChartKind, bar: YearBar): String {
    if (kind ==
        YearChartKind.TOP_SET
    ) {
        return pluralStringResource(R.plurals.progress_weeks_trained, bar.count, bar.count)
    }
    val countPlural = if (kind == YearChartKind.TOTAL_VOLUME) R.plurals.progress_workouts else R.plurals.progress_sets
    return stringResource(
        R.string.progress_count_in_months,
        pluralStringResource(countPlural, bar.count, bar.count),
        pluralStringResource(R.plurals.progress_months, bar.months, bar.months),
    )
}

@Composable
@ReadOnlyComposable
private fun yearsSummary(summary: AllTimeSummary): AnnotatedString = when (summary) {
    is AllTimeSummary.AveragePerMonth ->
        styledStringResource(R.string.progress_readout_years_summary, formatWholeKg(summary.kg))
    is AllTimeSummary.AverageTopSet -> summary.kg?.let { kg ->
        val weeks = pluralStringResource(R.plurals.progress_weeks, summary.weeks, summary.weeks)
        styledStringResource(R.string.progress_readout_years_top_summary, formatWeight(kg), weeks)
    } ?: AnnotatedString(stringResource(R.string.progress_readout_years_top_none))
}

/** `130 kg × 5`. */
@Composable
@ReadOnlyComposable
private fun weightRepsText(top: TopSet): String =
    stringResource(R.string.progress_weight_reps, formatWeight(top.weightKg), top.reps)

/** The drawable form of [chart], with labels and screen-reader descriptions. Null for [ProgressChart.NoExercise]. */
@Composable
@ReadOnlyComposable
fun barChartModel(chart: ProgressChart): BarChartModel? = when (chart) {
    is ProgressChart.NoExercise -> null
    is ProgressChart.Months -> monthsModel(chart)
    is ProgressChart.Weeks -> weeksModel(chart)
    is ProgressChart.Years -> yearsModel(chart)
}

@Composable
@ReadOnlyComposable
private fun monthsModel(chart: ProgressChart.Months): BarChartModel {
    val bars = chart.bars.mapIndexed { index, bar ->
        val name = bar.month.shortName()
        ChartBar(
            value = bar.volumeKg,
            valueLabel = formatCompact(bar.volumeKg),
            axisLabel = name,
            narrowAxisLabel = name.take(1),
            description = stringResource(
                R.string.progress_bar_month,
                bar.month.longName(),
                formatWholeKg(bar.volumeKg),
            ),
            isCurrent = chart.isCurrent(index),
        )
    }
    return BarChartModel(chartTitle(chart), bars, isZoomed = false, isWeekly = false, emptyList(), volumeAxis(bars))
}

@Composable
@ReadOnlyComposable
private fun weeksModel(chart: ProgressChart.Weeks): BarChartModel {
    val monthStarts = chart.monthStarts.toSet()
    val bars = chart.bars.mapIndexed { index, week ->
        val top = week.top
        val monthLabel = if (index in monthStarts) week.month.shortName() else null
        ChartBar(
            value = top?.weightKg ?: 0.0,
            valueLabel = top?.let { formatWeight(it.weightKg) }.orEmpty(),
            axisLabel = monthLabel,
            narrowAxisLabel = monthLabel,
            description = if (top == null) {
                stringResource(R.string.progress_bar_week_empty, week.isoWeek)
            } else {
                stringResource(R.string.progress_bar_week, week.isoWeek, weightRepsText(top))
            },
            isCurrent = chart.isCurrentYear && index == chart.bars.lastIndex,
        )
    }
    val separators = chart.monthStarts.filter { it > 0 }
    return BarChartModel(chartTitle(chart), bars, isZoomed = true, isWeekly = true, separators, ::weightAxisLabel)
}

@Composable
@ReadOnlyComposable
private fun yearsModel(chart: ProgressChart.Years): BarChartModel {
    val isTopSet = chart.kind == YearChartKind.TOP_SET
    val bars = chart.bars.map { bar ->
        ChartBar(
            value = bar.valueKg,
            valueLabel = if (isTopSet) formatWeight(bar.valueKg) else formatCompact(bar.valueKg),
            axisLabel = bar.year.toString(),
            narrowAxisLabel = bar.year.toString(),
            description = if (isTopSet) {
                stringResource(R.string.progress_bar_year_top, bar.year, formatWeight(bar.valueKg))
            } else {
                stringResource(R.string.progress_bar_year_volume, bar.year, formatWholeKg(bar.valueKg))
            },
            isCurrent = bar.year == chart.currentYear,
        )
    }
    val axis = if (isTopSet) ::weightAxisLabel else volumeAxis(bars)
    return BarChartModel(chartTitle(chart), bars, isZoomed = isTopSet, isWeekly = false, emptyList(), axis)
}

/** Whole kilograms, or short numbers once the chart reaches 10,000 kg. */
private fun volumeAxis(bars: List<ChartBar>): (Double) -> String =
    if (bars.any { it.value >= COMPACT_AXIS_FROM_KG }) ::formatCompact else ::formatWholeKg

/** `82.5`; zero is written `0` (the prototype's weight format would print "bw"). */
private fun weightAxisLabel(kg: Double): String = if (kg == 0.0) "0" else formatWeight(kg)
