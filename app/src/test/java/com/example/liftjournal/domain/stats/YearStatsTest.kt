package com.example.liftjournal.domain.stats

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YearStatsTest {
    private val today = date("2026-11-11")

    @Test
    fun `empty current year has months up to now and no totals`() {
        val stats = yearStats(emptyList(), 2026, today, null)
        assertEquals(11, stats.months.size)
        assertEquals(0.0, stats.totalKg, 0.0)
        assertEquals(0, stats.workouts)
        assertTrue(stats.exercises.isEmpty())
    }

    @Test
    fun `past year has twelve months and weeks to the end of December`() {
        val stats = yearStats(emptyList(), 2025, today, null)
        assertEquals(12, stats.months.size)
        assertEquals(date("2024-12-30"), stats.weeks.first().monday)
        assertEquals(1, stats.weeks.first().isoWeek)
        assertEquals(date("2025-12-29"), stats.weeks.last().monday)
    }

    @Test
    fun `weeks start with the ISO week containing 1 January and end with the current week`() {
        val stats = yearStats(emptyList(), 2026, today, null)
        assertEquals(date("2025-12-29"), stats.weeks.first().monday)
        assertEquals(date("2026-11-09"), stats.weeks.last().monday)
        assertEquals(46, stats.weeks.last().isoWeek)
    }

    @Test
    fun `a single week of training fills its month and week`() {
        val entries =
            listOf(entry("2026-11-09", "squat 100 x 5, 5"), entry("2026-11-11", "squat 110 x 3\nbench 60 x 5"))
        val stats = yearStats(entries, 2026, today, null)

        assertEquals(1630.0, stats.totalKg, 1e-9)
        assertEquals(2, stats.workouts)
        assertEquals(1630.0, stats.months[10].totalKg, 1e-9)
        assertEquals(TopSet(110.0, 3, date("2026-11-11")), stats.weeks.last().topSets["squat"])
        assertEquals(listOf("squat", "bench"), stats.exercises.map { it.key })
    }

    @Test
    fun `weekly top set tie is broken by more reps`() {
        val entries = listOf(entry("2026-11-09", "squat 100 x 3"), entry("2026-11-10", "squat 100 x 5"))
        val top = yearStats(entries, 2026, today, null).weeks.last().topSets.getValue("squat")
        assertEquals(5, top.reps)
    }

    @Test
    fun `only bodyweight sets count nothing without a bodyweight`() {
        val entries = listOf(entry("2026-11-09", "pull-up bw x 8, 8"))
        val stats = yearStats(entries, 2026, today, null)

        assertEquals(0.0, stats.totalKg, 0.0)
        assertEquals(1, stats.workouts)
        assertTrue(stats.exercises.isEmpty())
        assertNull(stats.weeks.last().topSets["pull-up"])
    }

    @Test
    fun `bodyweight sets count with a bodyweight`() {
        val entries = listOf(entry("2026-11-09", "pull-up bw x 8, 8"))
        val stats = yearStats(entries, 2026, today, 80.0)
        assertEquals(1280.0, stats.totalKg, 0.0)
        assertEquals(80.0, stats.exercises.single().heaviestSetKg, 0.0)
    }

    @Test
    fun `future entries and other years are ignored`() {
        val entries = listOf(entry("2026-11-20", "squat 100 x 5"), entry("2025-11-09", "squat 100 x 5"))
        assertEquals(0.0, yearStats(entries, 2026, today, null).totalKg, 0.0)
    }

    @Test
    fun `average set weight is the mean of counted set weights`() {
        val entries = listOf(entry("2026-11-09", "squat 100 x 5, 110 x 3"))
        val squat = yearStats(entries, 2026, today, null).exercises.single()
        assertEquals(105.0, squat.averageSetWeightKg, 1e-9)
        assertEquals(2, squat.weightedSetCount)
    }

    @Test
    fun `available years include the current year and are newest first`() {
        val entries = listOf(entry("2024-03-01", "squat 100 x 5"), entry("2027-01-01", "squat 100 x 5"))
        assertEquals(listOf(2026, 2024), availableYears(entries, today))
    }
}
