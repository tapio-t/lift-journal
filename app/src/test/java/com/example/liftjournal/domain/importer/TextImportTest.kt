package com.example.liftjournal.domain.importer

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.parser.EntryParser
import com.example.liftjournal.readTestResource
import com.example.liftjournal.readTestResourceBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextImportTest {
    private val sample = parseImportText(readTestResource("import-sample.txt"))

    @Test
    fun `sample file gives 4 entries with the right dates`() {
        assertEquals(
            listOf(date("2026-11-02"), date("2026-11-04"), date("2026-11-06"), date("2026-11-09")),
            sample.entries.map { it.date },
        )
        assertEquals(listOf("Training log 2026 (exported from Google Docs as plain text)"), sample.ignoredLines)
    }

    @Test
    fun `sample entries keep their text and parse into sets`() {
        val wednesday = sample.entries[1]
        assertEquals(
            "deadlift 150 x 5, (160 x 3)\noverhead press 52.5 x 5, 5, 5\npull-up bw x 8, 8, 6\nFelt strong today",
            wednesday.text,
        )
        val parsed = EntryParser.parse(wednesday.text)
        assertEquals(listOf("Deadlift", "Overhead press", "Pull-up"), parsed.exercises.map { it.name })
        assertEquals(listOf("Felt strong today"), parsed.notes)
    }

    @Test
    fun `every accepted date format is read`() {
        val expected = date("2026-11-09")
        listOf(
            "9.11.2026", "09.11.2026", "9.11.26", "2026-11-09", "Mon 9.11.2026", "ma 9.11.2026",
            "  maanantai 09.11.26  ", "Monday, 9.11.2026", "MA. 2026-11-09",
        ).forEach { assertEquals(it, expected, parseDateLine(it)) }
    }

    @Test
    fun `lines that are not only a date are entry text`() {
        listOf("squat 9.11.2026", "9.11.2026 squat", "31.2.2026", "9.11", "foo 9.11.2026", "")
            .forEach { assertNull(it, parseDateLine(it)) }
    }

    @Test
    fun `a date repeated in the file is merged and an empty date is dropped`() {
        val result = parseImportText("1.11.2026\nsquat 100 x 5\n2.11.2026\n\n1.11.2026\nbench 60 x 5\n")
        assertEquals(listOf(StoredEntry(date("2026-11-01"), "squat 100 x 5\nbench 60 x 5")), result.entries)
    }

    @Test
    fun `utf-8 with a byte order mark and windows-1252 are both decoded`() {
        val byteOrderMark = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val utf8 = byteOrderMark + "ma 9.11.2026\nkyykky 100 x 5".toByteArray()
        assertEquals(date("2026-11-09"), parseImportText(decodeImportBytes(utf8)).entries.single().date)

        val windows = "9.11.2026\npenkki ä 60 x 5".toByteArray(charset("windows-1252"))
        assertEquals("penkki ä 60 x 5", parseImportText(decodeImportBytes(windows)).entries.single().text)
    }

    @Test
    fun `sample bytes decode as utf-8`() {
        assertEquals(4, parseImportText(decodeImportBytes(readTestResourceBytes("import-sample.txt"))).entries.size)
    }
}
