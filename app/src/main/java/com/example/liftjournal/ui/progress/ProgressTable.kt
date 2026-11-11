package com.example.liftjournal.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.liftjournal.R
import com.example.liftjournal.domain.format.formatWeekRange
import com.example.liftjournal.domain.format.formatWeight
import com.example.liftjournal.domain.format.formatWholeKg
import com.example.liftjournal.domain.format.longName
import com.example.liftjournal.domain.progress.ProgressChart
import com.example.liftjournal.domain.progress.YearChartKind
import com.example.liftjournal.ui.theme.AppText
import com.example.liftjournal.ui.theme.LocalPalette

/** A simple table: a header row and text rows. The first column is left-aligned, the rest right-aligned. */
class TableData(val headers: List<String>, val rows: List<List<String>>)

private val HeaderPadding = 6.dp
private val RowPadding = 5.dp

/** The chart's numbers as a table, newest first (prototype `tableHtml`). */
@Composable
fun DataTable(table: TableData, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    LazyColumn(modifier) {
        stickyHeader {
            Column(Modifier.background(palette.background)) {
                val headers = table.headers.map { it.uppercase() }
                TableRow(headers, AppText.statLabel.copy(color = palette.foreground), HeaderPadding)
                HorizontalDivider(color = palette.foreground)
            }
        }
        items(table.rows) { row ->
            TableRow(row, AppText.small.copy(color = palette.foreground), RowPadding)
            HorizontalDivider(color = palette.line)
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, style: TextStyle, verticalPadding: Dp) {
    Row(Modifier.fillMaxWidth().padding(vertical = verticalPadding)) {
        cells.forEachIndexed { index, cell ->
            Text(
                text = cell,
                style = style,
                textAlign = if (index == 0) TextAlign.Start else TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The table form of [chart], or null when there is nothing to list. */
@Composable
@ReadOnlyComposable
fun chartTable(chart: ProgressChart): TableData? = when (chart) {
    is ProgressChart.NoExercise -> null
    is ProgressChart.Months -> TableData(
        headers = listOf(
            stringResource(R.string.progress_table_month),
            stringResource(R.string.progress_table_volume),
            stringResource(
                if (chart.exerciseName ==
                    null
                ) {
                    R.string.progress_table_workouts
                } else {
                    R.string.progress_table_sets
                },
            ),
        ),
        rows = chart.bars.map {
            listOf(it.month.longName(), formatWholeKg(it.volumeKg), it.count.toString())
        }.reversed(),
    )
    is ProgressChart.Weeks -> TableData(
        headers = listOf(
            stringResource(R.string.progress_table_week),
            stringResource(R.string.progress_table_dates),
            stringResource(R.string.progress_table_top_set),
        ),
        rows = chart.bars.map { week ->
            val top = week.top?.let {
                stringResource(R.string.progress_weight_reps, formatWeight(it.weightKg), it.reps)
            }
            listOf(
                week.isoWeek.toString(),
                formatWeekRange(week.monday),
                top ?: stringResource(R.string.progress_table_empty),
            )
        }.reversed(),
    )
    is ProgressChart.Years -> yearsTable(chart)
}

@Composable
@ReadOnlyComposable
private fun yearsTable(chart: ProgressChart.Years): TableData {
    val isTopSet = chart.kind == YearChartKind.TOP_SET
    val rows = chart.bars.map { bar ->
        val value = if (isTopSet) formatWeight(bar.valueKg) else formatWholeKg(bar.valueKg)
        listOf(bar.year.toString(), value, yearBarDetails(chart.kind, bar))
    }
    val valueHeader = if (isTopSet) R.string.progress_table_avg_top else R.string.progress_table_avg_month
    return TableData(
        headers = listOf(stringResource(R.string.progress_table_year), stringResource(valueHeader), ""),
        rows = rows.reversed(),
    )
}
