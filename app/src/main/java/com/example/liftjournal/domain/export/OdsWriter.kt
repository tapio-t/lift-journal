package com.example.liftjournal.domain.export

const val ODS_MIME_TYPE = "application/vnd.oasis.opendocument.spreadsheet"

private const val XML_DECLARATION = """<?xml version="1.0" encoding="UTF-8"?>"""
private const val ODF_VERSION = "1.2"
private const val OFFICE_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
private const val CONFIG_NS = "urn:oasis:names:tc:opendocument:xmlns:config:1.0"
private const val MANIFEST_NS = "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0"
private const val CONTENT_NAMESPACES =
    """xmlns:office="$OFFICE_NS" """ +
        """xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" """ +
        """xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" """ +
        """xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" """ +
        """xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0""""
private const val HEADER_STYLE_NAME = "hdr"

/** In LibreOffice's view settings, split mode 2 freezes rows above the split position. */
private const val FROZEN_SPLIT_MODE = 2
private const val FROZEN_ROWS = 1
private const val LOWER_PANE = 2

/**
 * Writes an OpenDocument .ods spreadsheet (port of the prototype's `buildOds`): a bold header row, and
 * multi-line text as one paragraph per line. It also freezes the header row through settings.xml, which the
 * prototype didn't do, so the export matches SPEC §6.1.
 */
fun buildOds(sheets: List<Sheet>): ByteArray = zipArchive(
    listOf(
        // The mimetype file must come first and uncompressed, so apps can recognise the format.
        ArchiveFile("mimetype", ODS_MIME_TYPE, isStored = true),
        ArchiveFile("content.xml", contentXml(sheets)),
        ArchiveFile("settings.xml", settingsXml(sheets)),
        ArchiveFile("META-INF/manifest.xml", manifestXml()),
    ),
)

private fun contentXml(sheets: List<Sheet>): String = buildString {
    append(XML_DECLARATION)
    append("""<office:document-content $CONTENT_NAMESPACES office:version="$ODF_VERSION">""")
    append("""<office:automatic-styles><style:style style:name="$HEADER_STYLE_NAME" style:family="table-cell">""")
    append("""<style:text-properties fo:font-weight="bold"/></style:style></office:automatic-styles>""")
    append("<office:body><office:spreadsheet>")
    sheets.forEach { appendTable(it) }
    append("</office:spreadsheet></office:body></office:document-content>")
}

private fun StringBuilder.appendTable(sheet: Sheet) {
    val columnCount = sheet.rows.maxOfOrNull { it.size } ?: 0
    append("""<table:table table:name="${escapeXml(sheet.name)}">""")
    append("""<table:table-column table:number-columns-repeated="$columnCount"/>""")
    sheet.rows.forEachIndexed { rowIndex, cells ->
        val style = if (rowIndex == 0) """ table:style-name="$HEADER_STYLE_NAME"""" else ""
        append("<table:table-row>")
        for (column in 0 until columnCount) appendCell(cells.getOrElse(column) { Cell.Empty }, style)
        append("</table:table-row>")
    }
    append("</table:table>")
}

private fun StringBuilder.appendCell(cell: Cell, style: String) {
    when (cell) {
        Cell.Empty -> append("<table:table-cell/>")
        is Cell.Number -> {
            val value = cell.xmlValue()
            append("""<table:table-cell$style office:value-type="float" office:value="$value">""")
            append("<text:p>$value</text:p></table:table-cell>")
        }
        is Cell.Text -> {
            append("""<table:table-cell$style office:value-type="string">""")
            cell.value.split('\n').forEach { line -> append("<text:p>${escapeXml(line)}</text:p>") }
            append("</table:table-cell>")
        }
    }
}

/** Freezes the first row of every table. */
private fun settingsXml(sheets: List<Sheet>): String = buildString {
    append(XML_DECLARATION)
    append("""<office:document-settings xmlns:office="$OFFICE_NS" xmlns:config="$CONFIG_NS" """)
    append("""office:version="$ODF_VERSION"><office:settings>""")
    append("""<config:config-item-set config:name="ooo:view-settings">""")
    append("""<config:config-item-map-indexed config:name="Views"><config:config-item-map-entry>""")
    append("""<config:config-item config:name="ViewId" config:type="string">view1</config:config-item>""")
    append("""<config:config-item-map-named config:name="Tables">""")
    sheets.forEach { sheet ->
        append("""<config:config-item-map-entry config:name="${escapeXml(sheet.name)}">""")
        appendConfigItem("VerticalSplitMode", "short", FROZEN_SPLIT_MODE)
        appendConfigItem("VerticalSplitPosition", "int", FROZEN_ROWS)
        appendConfigItem("ActiveSplitRange", "short", LOWER_PANE)
        appendConfigItem("PositionTop", "int", 0)
        appendConfigItem("PositionBottom", "int", FROZEN_ROWS)
        append("</config:config-item-map-entry>")
    }
    append("</config:config-item-map-named>")
    append("</config:config-item-map-entry></config:config-item-map-indexed>")
    append("</config:config-item-set></office:settings></office:document-settings>")
}

private fun StringBuilder.appendConfigItem(name: String, type: String, value: Int) {
    append("""<config:config-item config:name="$name" config:type="$type">$value</config:config-item>""")
}

private fun manifestXml(): String = XML_DECLARATION +
    """<manifest:manifest xmlns:manifest="$MANIFEST_NS" manifest:version="$ODF_VERSION">""" +
    """<manifest:file-entry manifest:full-path="/" manifest:version="$ODF_VERSION" """ +
    """manifest:media-type="$ODS_MIME_TYPE"/>""" +
    """<manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>""" +
    """<manifest:file-entry manifest:full-path="settings.xml" manifest:media-type="text/xml"/>""" +
    "</manifest:manifest>"
