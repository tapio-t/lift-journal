package com.example.liftjournal.domain.importer

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

/** Every .docx and .odt file is a zip archive, which starts with these bytes (`PK\u0003\u0004`). */
private val ZIP_SIGNATURE = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

/** A training log is far smaller; a larger document part is refused so a crafted file can't exhaust memory. */
private const val MAX_DOCUMENT_XML_BYTES = 20 * 1024 * 1024

private const val COPY_BUFFER_BYTES = 8 * 1024

/** Where a word-processor format keeps its text, and which of its XML elements matter for plain text. */
private enum class DocumentFormat(
    val bodyPath: String,
    val namespace: String,
    /** Elements whose character data is document text. */
    val textElements: Set<String>,
    /** Elements that end a line when they close. */
    val paragraphElements: Set<String>,
    val lineBreakElements: Set<String>,
    /** Elements holding settings rather than text, whose children are skipped. */
    val settingsElements: Set<String> = emptySet(),
) {
    /**
     * Word (.docx): text sits in `w:t` runs inside `w:p` paragraphs. Paragraph settings (`w:pPr`) are skipped,
     * because their tab stops are also called `w:tab`.
     */
    DOCX(
        bodyPath = "word/document.xml",
        namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
        textElements = setOf("t"),
        paragraphElements = setOf("p"),
        lineBreakElements = setOf("br", "cr"),
        settingsElements = setOf("pPr"),
    ),

    /** OpenDocument text (.odt): text sits directly in `text:p` paragraphs and `text:h` headings. */
    ODT(
        bodyPath = "content.xml",
        namespace = "urn:oasis:names:tc:opendocument:xmlns:text:1.0",
        textElements = setOf("p", "h"),
        paragraphElements = setOf("p", "h"),
        lineBreakElements = setOf("line-break"),
    ),
}

/**
 * The text of an imported file (PLAN phase 7). Word (.docx) and OpenDocument (.odt) files become one line per
 * paragraph; any other file is read as plain text. The format is recognised from the content, not the file name.
 * Returns null for a zip archive that isn't a readable .docx or .odt.
 */
fun importFileText(bytes: ByteArray): String? {
    if (!bytes.startsWithZipSignature()) return decodeImportBytes(bytes)
    return try {
        val (format, xml) = findDocumentBody(bytes) ?: return null
        paragraphText(format, xml)
    } catch (_: IOException) {
        null
    } catch (_: SAXException) {
        null
    } catch (_: ParserConfigurationException) {
        null
    }
}

private fun ByteArray.startsWithZipSignature(): Boolean =
    size >= ZIP_SIGNATURE.size && ZIP_SIGNATURE.indices.all { this[it] == ZIP_SIGNATURE[it] }

/** The first document part found in the archive, or null when there is none or it is too large. */
private fun findDocumentBody(zipBytes: ByteArray): Pair<DocumentFormat, ByteArray>? {
    ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: return null
            val format = DocumentFormat.entries.firstOrNull { it.bodyPath == entry.name } ?: continue
            val xml = zip.readAtMost(MAX_DOCUMENT_XML_BYTES) ?: return null
            return format to xml
        }
    }
}

/** Reads the current zip entry, or returns null if it is longer than [limit] bytes. */
private fun ZipInputStream.readAtMost(limit: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(COPY_BUFFER_BYTES)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        if (output.size() + count > limit) return null
        output.write(buffer, 0, count)
    }
}

private fun paragraphText(format: DocumentFormat, xml: ByteArray): String {
    val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
    // Document files never need a DTD; refusing one blocks external-entity tricks where the parser supports it.
    runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    val collector = ParagraphCollector(format)
    factory.newSAXParser().parse(ByteArrayInputStream(xml), collector)
    return collector.text()
}

/** Collects a document's text with a line break after every paragraph. */
private class ParagraphCollector(private val format: DocumentFormat) : DefaultHandler() {
    private val text = StringBuilder()

    // Text elements can nest (an .odt note holds its own paragraphs), so this counts how deep we are.
    private var textDepth = 0
    private var settingsDepth = 0

    fun text(): String = text.toString()

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        if (uri != format.namespace) return
        if (localName in format.settingsElements) settingsDepth++
        if (settingsDepth > 0) return
        when (localName) {
            in format.textElements -> textDepth++
            in format.lineBreakElements -> text.append('\n')
            "tab" -> text.append('\t')
            // .odt stores runs of spaces as <text:s text:c="3"/>.
            "s" -> text.append(" ".repeat(attributes.getValue(format.namespace, "c")?.toIntOrNull() ?: 1))
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        if (uri != format.namespace) return
        if (localName in format.settingsElements) settingsDepth--
        if (settingsDepth > 0) return
        if (localName in format.textElements) textDepth--
        if (localName in format.paragraphElements) text.append('\n')
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (textDepth > 0) text.appendRange(ch, start, start + length)
    }
}
