package com.example.liftjournal.domain.journal

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class JournalRulesTest {
    private val wednesday = date("2026-11-11")

    @Test
    fun `ghost text is last week's same weekday entry without note lines`() {
        val entries = listOf(entry("2026-11-04", "squat 100 x 5\nfelt good\nbench 60 x 5"))
        assertEquals(listOf("squat 100 x 5", "bench 60 x 5"), ghostLinesFor(wednesday, entries))
    }

    @Test
    fun `ghost text skips same weekday entries with only notes`() {
        val entries = listOf(entry("2026-10-28", "squat 90 x 5"), entry("2026-11-04", "rest day"))
        assertEquals(listOf("squat 90 x 5"), ghostLinesFor(wednesday, entries))
    }

    @Test
    fun `ghost text looks back at most 8 weeks`() {
        assertEquals(listOf("squat 90 x 5"), ghostLinesFor(wednesday, listOf(entry("2026-09-16", "squat 90 x 5"))))
        assertNull(ghostLinesFor(wednesday, listOf(entry("2026-09-09", "squat 90 x 5"))))
    }

    @Test
    fun `typed characters cover ghost characters line by line`() {
        val overlay = ghostOverlay("squ\n", listOf("squat 100 x 5", "bench 60 x 5"))
        assertEquals(GhostLine("squ", "at 100 x 5"), overlay[0])
        assertEquals(GhostLine("", "bench 60 x 5"), overlay[1])
    }

    @Test
    fun `tab completes the current line when typed text is a prefix`() {
        val accepted = acceptGhostLine("SQU", 3, listOf("squat 100 x 5"))
        assertEquals(TextWithCursor("SQUat 100 x 5", 13), accepted)
    }

    @Test
    fun `tab does nothing when typed text differs or cursor is mid-line`() {
        assertNull(acceptGhostLine("dead", 4, listOf("squat 100 x 5")))
        assertNull(acceptGhostLine("squ", 1, listOf("squat 100 x 5")))
        assertNull(acceptGhostLine("squat 100 x 5", 13, listOf("squat 100 x 5")))
    }

    @Test
    fun `tab works on the second line`() {
        val accepted = acceptGhostLine("squat 100 x 5\nbe", 16, listOf("squat 100 x 5", "bench 60 x 5"))
        assertEquals("squat 100 x 5\nbench 60 x 5", accepted?.text)
    }

    @Test
    fun `search needs every word in text or date`() {
        val entries =
            listOf(
                entry("2026-11-09", "Squat 100 x 5"),
                entry("2026-10-09", "bench 60 x 5"),
                entry("2026-11-10", "squat 90 x 5"),
            )
        assertEquals(
            listOf(date("2026-11-10"), date("2026-11-09")),
            searchEntries(entries, "squat", null).map {
                it.date
            },
        )
        assertEquals(listOf(date("2026-11-09")), searchEntries(entries, "SQUAT 09.11", null).map { it.date })
        assertEquals(listOf(date("2026-10-09")), searchEntries(entries, "", YearMonth.of(2026, 10)).map { it.date })
    }

    @Test
    fun `month tags list months with entries newest first`() {
        val entries = listOf(entry("2026-10-09", "a"), entry("2026-11-09", "b"), entry("2026-11-10", "c"))
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), monthsWithEntries(entries))
    }
}
