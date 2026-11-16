package com.example.liftjournal.domain.export

import com.example.liftjournal.domain.entry
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportSheetsTest {
    private val entries = listOf(
        entry("2026-11-04", "Squat 110 x 3\npull-up bw x 8\nfelt strong"),
        entry("2026-11-02", "squat 100 x 5, 5\nbench 60 x 5"),
        entry("2026-10-30", "rest day"),
    )

    private fun sheet(name: String, bodyweightKg: Double? = null) =
        buildExportSheets(entries, bodyweightKg).single { it.name == name }

    @Test
    fun `there are six sheets in the prototype order`() {
        assertEquals(
            listOf("Sets", "Daily volume", "Weekly volume", "Monthly volume", "Monthly total", "Journal text"),
            buildExportSheets(entries, null).map { it.name },
        )
    }

    @Test
    fun `sets sheet has one row per set, oldest first`() {
        val rows = sheet("Sets").rows
        assertEquals(
            row(
                "Date", "Weekday", "ISO year", "ISO week", "Exercise", "Set", "Weight (kg)", "Reps", "Volume (kg)",
                "Bodyweight ×",
            ),
            rows[0],
        )
        assertEquals(row("2026-11-02", "Mon", 2026, 45, "Squat", 1, 100, 5, 500, null), rows[1])
        assertEquals(row("2026-11-02", "Mon", 2026, 45, "Squat", 2, 100, 5, 500, null), rows[2])
        assertEquals(6, rows.size)
    }

    @Test
    fun `bodyweight sets have empty weight and volume until bodyweight is set`() {
        assertEquals(
            row("2026-11-04", "Wed", 2026, 45, "Pull-up", 1, null, 8, null, 1),
            sheet("Sets").rows.last(),
        )
        assertEquals(
            row("2026-11-04", "Wed", 2026, 45, "Pull-up", 1, 80, 8, 640, 1),
            sheet("Sets", bodyweightKg = 80.0).rows.last(),
        )
    }

    @Test
    fun `daily volume leaves the top set empty when nothing counted`() {
        val rows = sheet("Daily volume").rows
        assertEquals(row("2026-11-02", "Mon", 45, "Squat", 2, 10, 1000, 100), rows[1])
        assertEquals(row("2026-11-04", "Wed", 45, "Pull-up", 1, 8, 0, null), rows.last())
    }

    @Test
    fun `weekly volume groups the same exercise across spellings and days`() {
        assertEquals(
            listOf(
                row(
                    "ISO year",
                    "ISO week",
                    "Week starts (Mon)",
                    "Exercise",
                    "Sets",
                    "Reps",
                    "Volume (kg)",
                    "Top set (kg)",
                ),
                row(2026, 45, "2026-11-02", "Bench", 1, 5, 300, 60),
                row(2026, 45, "2026-11-02", "Pull-up", 1, 8, 0, null),
                row(2026, 45, "2026-11-02", "Squat", 3, 13, 1330, 110),
            ),
            sheet("Weekly volume").rows,
        )
    }

    @Test
    fun `monthly volume is sorted by month then exercise`() {
        val rows = sheet("Monthly volume").rows
        assertEquals(row("2026-11", "Squat", 3, 13, 1330, 110), rows.last())
        assertEquals(4, rows.size)
    }

    @Test
    fun `monthly total counts months with only notes too`() {
        assertEquals(
            listOf(
                row("Month", "Workouts", "Sets", "Total volume (kg)"),
                row("2026-10", 0, 0, 0),
                row("2026-11", 2, 5, 1630),
            ),
            sheet("Monthly total").rows,
        )
    }

    @Test
    fun `journal text keeps the full entry text`() {
        assertEquals(
            row("2026-11-04", "Wed", 330, "Squat 110 x 3\npull-up bw x 8\nfelt strong"),
            sheet("Journal text").rows.last(),
        )
    }
}
