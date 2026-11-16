package com.example.liftjournal.domain.stats

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.entry
import org.junit.Assert.assertEquals
import org.junit.Test

class AllTimeStatsTest {
    private val today = date("2026-11-11")

    @Test
    fun `first year counts from its first logged month and current year up to now`() {
        val entries = listOf(entry("2025-10-15", "squat 100 x 5"), entry("2026-02-01", "squat 100 x 5"))
        val stats = allTimeStats(entries, today, null)

        assertEquals(listOf(2025, 2026), stats.years.map { it.year })
        assertEquals(listOf(3, 11), stats.years.map { it.months })
        assertEquals(14, stats.spanMonths)
    }

    @Test
    fun `yearly averages divide by counted months`() {
        val entries = listOf(entry("2025-10-15", "squat 100 x 6"))
        val year2025 = allTimeStats(entries, today, null).years.first()
        assertEquals(200.0, year2025.averageKgPerMonth, 1e-9)
        assertEquals(200.0, year2025.exerciseAverageKgPerMonth("squat"), 1e-9)
    }

    @Test
    fun `average weekly top set uses only trained weeks`() {
        val entries = listOf(
            entry("2026-11-02", "squat 100 x 5, 90 x 5"),
            entry("2026-11-09", "squat 110 x 3"),
            entry("2026-11-10", "squat 105 x 3"),
        )
        val squat = allTimeStats(entries, today, null).years.single().byExercise.getValue("squat")
        assertEquals(105.0, squat.averageWeeklyTopKg, 1e-9)
        assertEquals(2, squat.weeklyTopsKg.size)
    }

    @Test
    fun `empty data still shows the current year`() {
        val stats = allTimeStats(emptyList(), today, null)
        assertEquals(listOf(2026), stats.years.map { it.year })
        assertEquals(0.0, stats.averageKgPerMonth, 0.0)
    }
}
