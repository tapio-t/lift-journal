package com.example.liftjournal.domain.goals

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Month

class GoalCalculatorTest {
    private val today = date("2026-11-11")
    private val entries = listOf(
        entry("2026-10-13", "squat 150 x 1"),
        entry("2026-10-20", "squat 140 x 3"),
        entry("2026-11-02", "squat 130 x 5\nbench 80 x 5"),
        entry("2026-11-10", "squat 145 x 2\npull-up bw x 10"),
    )

    @Test
    fun `top set goal uses the heaviest set in the last 28 days`() {
        val goal = Goal(1, GoalType.TOP_SET, "squat", null, 160.0)
        val progress = goalProgress(goal, entries, today, null)
        assertEquals(145.0, progress.currentKg, 0.0)
        assertEquals(GoalBasis.HeaviestLastFourWeeks, progress.basis)
        assertEquals(91, goalPercent(goal, progress))
    }

    @Test
    fun `average set goal averages the sets in the last 28 days`() {
        val goal = Goal(1, GoalType.AVERAGE_SET, "squat", null, 150.0)
        val progress = goalProgress(goal, entries, today, null)
        assertEquals(415.0 / 3, progress.currentKg, 1e-9)
        assertEquals(GoalBasis.AverageLastFourWeeks(3), progress.basis)
    }

    @Test
    fun `weekly volume goal counts this week so far from Monday`() {
        val goal = Goal(1, GoalType.TOTAL_VOLUME, null, GoalPeriod.WEEK, 1000.0)
        val progress = goalProgress(goal, entries, today, null)
        assertEquals(290.0, progress.currentKg, 0.0)
        assertEquals(1, progress.uncountedBodyweightSets)
        assertEquals(GoalBasis.WeekSoFar(46), progress.basis)
    }

    @Test
    fun `monthly exercise volume goal counts this month so far`() {
        val goal = Goal(1, GoalType.EXERCISE_VOLUME, "squat", GoalPeriod.MONTH, 1000.0)
        val progress = goalProgress(goal, entries, today, 80.0)
        assertEquals(940.0, progress.currentKg, 0.0)
        assertEquals(GoalBasis.MonthSoFar(Month.NOVEMBER), progress.basis)
        assertEquals(94, goalPercent(goal, progress))
    }

    @Test
    fun `percentage can exceed 100`() {
        val goal = Goal(1, GoalType.TOP_SET, "squat", null, 100.0)
        assertEquals(145, goalPercent(goal, goalProgress(goal, entries, today, null)))
    }

    @Test
    fun `suggested target is about 10 percent above now`() {
        assertEquals(160.0, suggestedTarget(GoalType.TOP_SET, 145.0)!!, 0.0)
        assertEquals(1500.0, suggestedTarget(GoalType.TOTAL_VOLUME, 1300.0)!!, 0.0)
        assertNull(suggestedTarget(GoalType.TOP_SET, 0.0))
    }
}
