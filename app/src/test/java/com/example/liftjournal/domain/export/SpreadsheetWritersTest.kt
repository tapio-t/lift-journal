package com.example.liftjournal.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Re-opens written files: the zip must be readable and every XML part well-formed (PLAN phase 5). */
class SpreadsheetWritersTest {
    private val sheets = listOf(
        Sheet("Sets", listOf(row("Date", "Weight (kg)", "Note"), row("2026-11-02", 82.5, "a < b & \"c\"\nline 2"))),
        Sheet("Monthly total", listOf(row("Month", "Total"), row("2026-11", 1630), row(null, 0))),
    )

    private data class ZipPart(val entry: ZipEntry, val text: String)

    private fun unzip(bytes: ByteArray): List<ZipPart> = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        generateSequence { zip.nextEntry }.map { ZipPart(it, zip.readBytes().toString(Charsets.UTF_8)) }.toList()
    }

    private fun part(bytes: ByteArray, name: String): String = unzip(bytes).single { it.entry.name == name }.text

    private fun parseXml(text: String): Document = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))

    @Test
    fun `xlsx has the workbook parts and one well-formed worksheet per sheet`() {
        val parts = unzip(buildXlsx(sheets))
        assertEquals(
            listOf(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/styles.xml",
                "xl/worksheets/sheet1.xml",
                "xl/worksheets/sheet2.xml",
            ),
            parts.map { it.entry.name },
        )
        parts.forEach { parseXml(it.text) }
    }

    @Test
    fun `xlsx writes numbers as numbers, text inline and a bold frozen header`() {
        val worksheet = part(buildXlsx(sheets), "xl/worksheets/sheet1.xml")
        assertTrue(worksheet.contains("""<c r="B2"><v>82.5</v></c>"""))
        assertTrue(
            worksheet.contains("""<c r="A1" t="inlineStr" s="1"><is><t xml:space="preserve">Date</t></is></c>"""),
        )
        assertTrue(worksheet.contains("a &lt; b &amp; &quot;c&quot;\nline 2"))
        assertTrue(worksheet.contains("""state="frozen""""))
    }

    @Test
    fun `xlsx skips empty cells but writes zero`() {
        val worksheet = part(buildXlsx(sheets), "xl/worksheets/sheet2.xml")
        assertTrue(worksheet.contains("""<row r="3"><c r="B3"><v>0</v></c></row>"""))
    }

    @Test
    fun `ods starts with an uncompressed mimetype and all xml parts are well-formed`() {
        val parts = unzip(buildOds(sheets))
        val mimetype = parts.first()
        assertEquals("mimetype", mimetype.entry.name)
        assertEquals(ZipEntry.STORED, mimetype.entry.method)
        assertEquals(ODS_MIME_TYPE, mimetype.text)
        assertEquals(
            listOf("content.xml", "settings.xml", "META-INF/manifest.xml"),
            parts.drop(1).map { it.entry.name },
        )
        parts.drop(1).forEach { parseXml(it.text) }
    }

    @Test
    fun `ods writes one table per sheet with float cells and one paragraph per text line`() {
        val content = parseXml(part(buildOds(sheets), "content.xml"))
        val tables = content.getElementsByTagName("table:table")
        assertEquals(2, tables.length)
        assertEquals("Sets", (tables.item(0) as Element).getAttribute("table:name"))

        val cells = content.getElementsByTagName("table:table-cell")
        val weight = cells.item(4) as Element
        assertEquals("float", weight.getAttribute("office:value-type"))
        assertEquals("82.5", weight.getAttribute("office:value"))
        val note = cells.item(5) as Element
        assertEquals(2, note.getElementsByTagName("text:p").length)
    }

    @Test
    fun `column names continue past Z`() {
        assertEquals(listOf("A", "Z", "AA", "AB", "BA"), listOf(0, 25, 26, 27, 52).map(::columnName))
    }

    @Test
    fun `control characters are removed from xml text`() {
        assertEquals("ab", escapeXml("a\u0001b"))
    }
}
