package com.example.liftjournal.domain.importer

import com.example.liftjournal.domain.export.ArchiveFile
import com.example.liftjournal.domain.export.zipArchive
import com.example.liftjournal.readTestResource
import com.example.liftjournal.readTestResourceBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentTextTest {
    private val textSample = parseImportText(readTestResource("import-sample.txt"))

    @Test
    fun `a Word file gives the same entries as the text file`() {
        val text = requireNotNull(importFileText(readTestResourceBytes("import-sample.docx")))
        assertEquals(textSample, parseImportText(text))
    }

    @Test
    fun `an OpenDocument file gives the same entries as the text file`() {
        val text = requireNotNull(importFileText(readTestResourceBytes("import-sample.odt")))
        assertEquals(textSample, parseImportText(text))
    }

    @Test
    fun `a plain text file is read as text`() {
        assertEquals("9.11.2026\nsquat 100 x 5", importFileText("9.11.2026\nsquat 100 x 5".toByteArray()))
    }

    @Test
    fun `Word runs, tabs and line breaks become plain text, tab stops don't`() {
        val body = """
            <w:p><w:pPr><w:tabs><w:tab w:val="left" w:pos="720"/></w:tabs></w:pPr>
              <w:r><w:t>9.11.</w:t></w:r><w:r><w:t>2026</w:t></w:r></w:p>
            <w:p><w:r><w:t>squat</w:t><w:tab/><w:t>100 x 5</w:t><w:br/><w:t>bench 60 x 5</w:t></w:r></w:p>
        """.trimIndent()
        val docx = zipArchive(listOf(ArchiveFile("word/document.xml", wordDocument(body))))

        assertEquals("9.11.2026\nsquat\t100 x 5\nbench 60 x 5\n", importFileText(docx))
    }

    @Test
    fun `OpenDocument spans, spaces and headings become plain text`() {
        val body = """
            <text:h>9.11.2026</text:h>
            <text:p>squat<text:s text:c="2"/><text:span>100 x 5</text:span><text:line-break/>bench 60 x 5</text:p>
        """.trimIndent()
        val odt = zipArchive(listOf(ArchiveFile("content.xml", openDocument(body))))

        assertEquals("9.11.2026\nsquat  100 x 5\nbench 60 x 5\n", importFileText(odt))
    }

    @Test
    fun `a zip archive without a document is unreadable`() {
        assertNull(importFileText(zipArchive(listOf(ArchiveFile("notes.txt", "9.11.2026")))))
    }

    @Test
    fun `a damaged document is unreadable`() {
        assertNull(importFileText(zipArchive(listOf(ArchiveFile("word/document.xml", "<w:document>")))))
    }

    private fun wordDocument(body: String) =
        """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""" +
            "<w:body>$body</w:body></w:document>"

    private fun openDocument(body: String) =
        """<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" """ +
            """xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">""" +
            "<office:body><office:text>$body</office:text></office:body></office:document-content>"
}
