package com.example.liftjournal.domain.export

private const val XML_DECLARATION = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
private const val OPENXML = "http://schemas.openxmlformats.org"
private const val SPREADSHEET_NS = "$OPENXML/spreadsheetml/2006/main"
private const val RELATIONSHIPS_NS = "$OPENXML/package/2006/relationships"
private const val DOCUMENT_RELATIONSHIPS = "$OPENXML/officeDocument/2006/relationships"
private const val MAIN_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml"

/** Style index 1 in styles.xml is the bold header font. */
private const val HEADER_STYLE = 1

/**
 * Writes an Excel .xlsx workbook (port of the prototype's `buildXlsx`): one worksheet per sheet, inline strings,
 * a bold header row and the header frozen.
 */
fun buildXlsx(sheets: List<Sheet>): ByteArray {
    val worksheets = sheets.mapIndexed { index, sheet ->
        ArchiveFile("xl/worksheets/sheet${index + 1}.xml", worksheetXml(sheet))
    }
    return zipArchive(
        listOf(
            ArchiveFile("[Content_Types].xml", contentTypesXml(sheets.size)),
            ArchiveFile("_rels/.rels", rootRelationshipsXml()),
            ArchiveFile("xl/workbook.xml", workbookXml(sheets)),
            ArchiveFile("xl/_rels/workbook.xml.rels", workbookRelationshipsXml(sheets.size)),
            ArchiveFile("xl/styles.xml", stylesXml()),
        ) + worksheets,
    )
}

private fun contentTypesXml(sheetCount: Int): String = buildString {
    append(XML_DECLARATION)
    append("""<Types xmlns="$OPENXML/package/2006/content-types">""")
    append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
    append("""<Default Extension="xml" ContentType="application/xml"/>""")
    append("""<Override PartName="/xl/workbook.xml" ContentType="$MAIN_CONTENT_TYPE.sheet.main+xml"/>""")
    append("""<Override PartName="/xl/styles.xml" ContentType="$MAIN_CONTENT_TYPE.styles+xml"/>""")
    for (number in 1..sheetCount) {
        append(
            """<Override PartName="/xl/worksheets/sheet$number.xml" ContentType="$MAIN_CONTENT_TYPE.worksheet+xml"/>""",
        )
    }
    append("</Types>")
}

private fun rootRelationshipsXml(): String = XML_DECLARATION +
    """<Relationships xmlns="$RELATIONSHIPS_NS">""" +
    """<Relationship Id="rId1" Type="$DOCUMENT_RELATIONSHIPS/officeDocument" Target="xl/workbook.xml"/>""" +
    "</Relationships>"

private fun workbookXml(sheets: List<Sheet>): String = buildString {
    append(XML_DECLARATION)
    append("""<workbook xmlns="$SPREADSHEET_NS" xmlns:r="$DOCUMENT_RELATIONSHIPS"><sheets>""")
    sheets.forEachIndexed { index, sheet ->
        val number = index + 1
        append("""<sheet name="${escapeXml(sheet.name)}" sheetId="$number" r:id="rId$number"/>""")
    }
    append("</sheets></workbook>")
}

/** Worksheets are rId1…rIdN; the styles part comes after them. */
private fun workbookRelationshipsXml(sheetCount: Int): String = buildString {
    append(XML_DECLARATION)
    append("""<Relationships xmlns="$RELATIONSHIPS_NS">""")
    for (number in 1..sheetCount) {
        append(
            """<Relationship Id="rId$number" Type="$DOCUMENT_RELATIONSHIPS/worksheet" """ +
                """Target="worksheets/sheet$number.xml"/>""",
        )
    }
    append("""<Relationship Id="rId${sheetCount + 1}" Type="$DOCUMENT_RELATIONSHIPS/styles" Target="styles.xml"/>""")
    append("</Relationships>")
}

/** Two fonts and two cell formats: normal (0) and bold header ([HEADER_STYLE]). */
private fun stylesXml(): String = XML_DECLARATION +
    """<styleSheet xmlns="$SPREADSHEET_NS">""" +
    """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font>""" +
    """<font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
    """<fills count="2"><fill><patternFill patternType="none"/></fill>""" +
    """<fill><patternFill patternType="gray125"/></fill></fills>""" +
    """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
    """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
    """<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
    """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>""" +
    """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
    "</styleSheet>"

private fun worksheetXml(sheet: Sheet): String = buildString {
    append(XML_DECLARATION)
    append("""<worksheet xmlns="$SPREADSHEET_NS">""")
    // Freezes the header row.
    append("""<sheetViews><sheetView workbookViewId="0">""")
    append("""<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""")
    append("</sheetView></sheetViews>")
    append("<cols>")
    columnWidths(sheet.rows).forEachIndexed { index, width ->
        append("""<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>""")
    }
    append("</cols><sheetData>")
    sheet.rows.forEachIndexed { rowIndex, cells -> appendRow(rowIndex, cells) }
    append("</sheetData></worksheet>")
}

private fun StringBuilder.appendRow(rowIndex: Int, cells: List<Cell>) {
    val rowNumber = rowIndex + 1
    val style = if (rowIndex == 0) """ s="$HEADER_STYLE"""" else ""
    append("""<row r="$rowNumber">""")
    cells.forEachIndexed { column, cell ->
        val reference = columnName(column) + rowNumber
        when (cell) {
            Cell.Empty -> Unit
            is Cell.Number -> append("""<c r="$reference"$style><v>${cell.xmlValue()}</v></c>""")
            is Cell.Text -> append(
                """<c r="$reference" t="inlineStr"$style><is><t xml:space="preserve">""" +
                    escapeXml(cell.value) + "</t></is></c>",
            )
        }
    }
    append("</row>")
}
