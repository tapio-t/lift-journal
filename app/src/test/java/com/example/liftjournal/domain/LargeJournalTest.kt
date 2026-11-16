package com.example.liftjournal.domain

import com.example.liftjournal.domain.export.SpreadsheetFormat
import com.example.liftjournal.domain.export.buildExportSheets
import com.example.liftjournal.domain.journal.ghostLinesFor
import com.example.liftjournal.domain.journal.searchEntries
import com.example.liftjournal.domain.progress.ProgressPeriod
import com.example.liftjournal.domain.progress.ProgressView
import com.example.liftjournal.domain.progress.periodStats
import com.example.liftjournal.domain.stats.availableYears
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Five years of training (about 800 entries) must stay quick to calculate (PLAN phase 8). The budget is generous
 * so the test isn't flaky on a slow CI machine; on a phone the same work runs off the main thread.
 */
class LargeJournalTest {
    private val today = date("2026-11-16")
    private val entries = generateTrainingJournal(today.minusYears(5), today).sortedByDescending { it.date }

    @Test
    fun `five years of data is about 800 entries`() {
        assertTrue("got ${entries.size}", entries.size in 650..850)
    }

    @Test
    fun `every progress view for every period calculates within budget`() {
        val periods = availableYears(entries, today).map { ProgressPeriod.OneYear(it) } + ProgressPeriod.AllTime
        val chartViews = ProgressView.entries - ProgressView.GOAL
        val elapsed = measureTime {
            periods.forEach { period ->
                val stats = periodStats(period, entries, today, bodyweightKg = 80.0)
                chartViews.forEach { view -> stats.chart(view, exerciseKey = null) }
            }
        }
        assertTrue("took $elapsed", elapsed < BUDGET)
    }

    @Test
    fun `export, search and ghost text stay within budget`() {
        val elapsed = measureTime {
            SpreadsheetFormat.XLSX.write(buildExportSheets(entries, bodyweightKg = 80.0))
            searchEntries(entries, "squat 2024", month = null)
            ghostLinesFor(today, entries)
        }
        assertTrue("took $elapsed", elapsed < BUDGET)
    }

    private companion object {
        val BUDGET = 5.seconds
    }
}
