package com.example.liftjournal.domain.progress

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.entry
import com.example.liftjournal.domain.stats.TopSet
import com.example.liftjournal.domain.stats.allTimeStats
import com.example.liftjournal.domain.stats.yearStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Month

class ProgressChartsTest {
    private val today = date("2026-11-11")

    private val entries = listOf(
        entry("2025-03-03", "squat 100 x 5"),
        entry("2026-10-20", "squat 120 x 5\nbench 60 x 5, 5"),
        entry("2026-10-22", "squat 120 x 6"),
        entry("2026-11-09", "squat 100 x 5"),
        entry("2026-11-11", "squat 125 x 2"),
    )

    @Test
    fun `requested exercise is kept while it has sets, otherwise the biggest by volume`() {
        val exercises = yearStats(entries, 2026, today, null).exercises
        assertEquals("bench", resolveExercise(exercises, "bench")?.key)
        assertEquals("squat", resolveExercise(exercises, "deadlift")?.key)
        assertNull(resolveExercise(emptyList(), "squat"))
    }

    @Test
    fun `total volume per month counts workouts and marks the current month`() {
        val chart = yearChart(yearStats(entries, 2026, today, null), ProgressView.TOTAL_VOLUME, null)
            as ProgressChart.Months

        assertEquals(11, chart.bars.size)
        assertEquals(Month.OCTOBER, chart.bars[9].month)
        assertEquals(600.0 + 600.0 + 720.0, chart.bars[9].volumeKg, 1e-9)
        assertEquals(2, chart.bars[9].count)
        assertTrue(chart.isCurrent(10))
        assertFalse(chart.isCurrent(9))
        assertEquals(2670.0, chart.totalKg, 1e-9)
    }

    @Test
    fun `exercise volume per month counts sets`() {
        val stats = yearStats(entries, 2026, today, null)
        val chart = yearChart(stats, ProgressView.EXERCISE_VOLUME, resolveExercise(stats.exercises, "bench"))
            as ProgressChart.Months

        assertEquals("Bench", chart.exerciseName)
        assertEquals(600.0, chart.bars[9].volumeKg, 1e-9)
        assertEquals(2, chart.bars[9].count)
        assertEquals(0, chart.bars[10].count)
    }

    @Test
    fun `top set per week finds best and latest and month starts`() {
        val stats = yearStats(entries, 2026, today, null)
        val chart = yearChart(stats, ProgressView.TOP_SET, resolveExercise(stats.exercises, "squat"))
            as ProgressChart.Weeks

        val october = chart.bars.indexOfFirst { it.monday == date("2026-10-19") }
        assertEquals(TopSet(120.0, 6, date("2026-10-22")), chart.bars[october].top)
        assertEquals(chart.bars.lastIndex, chart.best)
        assertEquals(chart.bars.lastIndex, chart.latest)
        // The week of 29 Dec 2025 belongs to January (its Thursday is 1 Jan), so it starts the list.
        assertEquals(Month.JANUARY, chart.bars.first().month)
        assertEquals(11, chart.monthStarts.size)
        assertEquals(AverageSetWeight(116.3, 4), chart.averageSetWeight)
    }

    @Test
    fun `an exercise view without weighted sets has no chart`() {
        val stats = yearStats(emptyList(), 2026, today, null)
        val chart = yearChart(stats, ProgressView.TOP_SET, resolveExercise(stats.exercises, null))
        assertEquals(ProgressChart.NoExercise(ProgressView.TOP_SET), chart)
        assertFalse(chart.hasData)
    }

    @Test
    fun `all-time total volume averages per month from the first logged month`() {
        val chart = allTimeChart(allTimeStats(entries, today, null), ProgressView.TOTAL_VOLUME, null, 2026)
            as ProgressChart.Years

        assertEquals(listOf(2025, 2026), chart.bars.map { it.year })
        assertEquals(10, chart.bars[0].months)
        assertEquals(50.0, chart.bars[0].valueKg, 1e-9)
        assertEquals(11, chart.bars[1].months)
        assertEquals(AllTimeSummary.AveragePerMonth(3170.0 / 21), chart.summary)
    }

    @Test
    fun `all-time top set averages weekly tops per year and overall`() {
        val stats = allTimeStats(entries, today, null)
        val chart = allTimeChart(stats, ProgressView.TOP_SET, resolveExercise(stats.exercises, "squat"), 2026)
            as ProgressChart.Years

        assertEquals(100.0, chart.bars[0].valueKg, 1e-9)
        // 2026 weeks: 120 (week 43) and 125 (week 46).
        assertEquals(122.5, chart.bars[1].valueKg, 1e-9)
        assertEquals(2, chart.bars[1].count)
        assertEquals(AllTimeSummary.AverageTopSet(115.0, 3), chart.summary)
    }

    @Test
    fun `header stats for the current year show this month so far`() {
        val stats = headerStats(yearStats(entries, 2026, today, null))
        assertEquals(4, stats.workouts)
        assertEquals(ThirdStat.MonthSoFar(Month.NOVEMBER, 750.0), stats.third)
    }

    @Test
    fun `header stats for a past year average over twelve months`() {
        val stats = headerStats(yearStats(entries, 2025, today, null))
        assertEquals(ThirdStat.AveragePerMonth(500.0 / 12), stats.third)
    }
}
