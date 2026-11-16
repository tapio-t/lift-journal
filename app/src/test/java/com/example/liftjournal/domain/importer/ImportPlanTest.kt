package com.example.liftjournal.domain.importer

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.readTestResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportPlanTest {
    private val imported = listOf(
        StoredEntry(date("2026-11-02"), "squat 120 x 5"),
        StoredEntry(date("2026-11-04"), "deadlift 150 x 5"),
        StoredEntry(date("2026-11-06"), "bench 80 x 5"),
    )
    private val existing = mapOf(
        date("2026-11-04") to "felt tired",
        date("2026-11-06") to "bench 80 x 5",
    )

    @Test
    fun `new dates are added and existing dates get the text appended below`() {
        val plan = planImport(imported, existing, ImportConflictMode.APPEND)
        assertEquals(listOf(date("2026-11-02")), plan.newDates)
        assertEquals(listOf(date("2026-11-04")), plan.appendedDates)
        assertEquals(listOf(date("2026-11-06")), plan.unchangedDates)
        assertEquals(
            listOf(
                StoredEntry(date("2026-11-02"), "squat 120 x 5"),
                StoredEntry(date("2026-11-04"), "felt tired\ndeadlift 150 x 5"),
            ),
            plan.entriesToWrite,
        )
    }

    @Test
    fun `skip existing dates leaves them untouched`() {
        val plan = planImport(imported, existing, ImportConflictMode.SKIP_EXISTING)
        assertEquals(listOf(StoredEntry(date("2026-11-02"), "squat 120 x 5")), plan.entriesToWrite)
        assertEquals(listOf(date("2026-11-04"), date("2026-11-06")), plan.unchangedDates)
    }

    @Test
    fun `importing the same file twice changes nothing the second time`() {
        val first = planImport(imported, existing, ImportConflictMode.APPEND)
        val afterFirst = existing + first.entriesToWrite.associate { it.date to it.text }

        val second = planImport(imported, afterFirst, ImportConflictMode.APPEND)
        assertTrue(second.entriesToWrite.isEmpty())
        assertEquals(imported.map { it.date }, second.unchangedDates)
    }

    @Test
    fun `preview of the sample counts entries, dates with entries and notes`() {
        val sample = parseImportText(readTestResource("import-sample.txt"))
        val preview = previewImport(sample, mapOf(date("2026-11-04") to "rest"), ImportConflictMode.APPEND)

        assertEquals(4, preview.entryCount)
        assertEquals(date("2026-11-02"), preview.firstDate)
        assertEquals(date("2026-11-09"), preview.lastDate)
        assertEquals(listOf(date("2026-11-04")), preview.datesWithEntries)
        assertEquals(1, preview.unreadableParts)
        assertEquals(1, preview.ignoredLines.size)
        assertEquals(3, preview.plan.newDates.size)
    }
}
