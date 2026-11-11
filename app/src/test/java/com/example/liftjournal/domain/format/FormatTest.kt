package com.example.liftjournal.domain.format

import com.example.liftjournal.domain.date
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class FormatTest {
    @Test
    fun `weights drop trailing zeros and keep at most two decimals`() {
        assertEquals("100", formatWeight(100.0))
        assertEquals("82.5", formatWeight(82.5))
        assertEquals("33.33", formatWeight(100.0 / 3))
        assertEquals("bw", formatWeight(0.0))
    }

    @Test
    fun `whole kilograms are rounded and grouped`() {
        assertEquals("41,230", formatWholeKg(41_229.6))
        assertEquals("0", formatWholeKg(0.2))
    }

    @Test
    fun `compact numbers match the prototype`() {
        assertEquals("950", formatCompact(950.0))
        assertEquals("1.5k", formatCompact(1_500.0))
        assertEquals("2k", formatCompact(2_000.0))
        assertEquals("41k", formatCompact(41_230.0))
        assertEquals("1.2M", formatCompact(1_234_567.0))
    }

    @Test
    fun `journal date header is day dot month dot year`() {
        assertEquals("09.11.2026", formatDayMonthYear(date("2026-11-09")))
    }

    @Test
    fun `long date adds the year only for other years`() {
        assertEquals("Thu 22 Oct", formatLongDate(date("2026-10-22"), today = date("2026-11-11")))
        assertEquals("Wed 22 Oct 2025", formatLongDate(date("2025-10-22"), today = date("2026-11-11")))
    }

    @Test
    fun `week range spans months when needed`() {
        assertEquals("19–25 Oct", formatWeekRange(date("2026-10-19")))
        assertEquals("26 Oct – 1 Nov", formatWeekRange(date("2026-10-26")))
    }

    @Test
    fun `month tag is short month and year`() {
        assertEquals("Nov 2026", formatMonthTag(YearMonth.of(2026, 11)))
        assertEquals("Sep 2026", formatMonthTag(YearMonth.of(2026, 9)))
    }
}
