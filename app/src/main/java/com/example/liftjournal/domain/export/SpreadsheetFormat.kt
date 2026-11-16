package com.example.liftjournal.domain.export

import com.example.liftjournal.domain.time.toStorageString
import java.time.LocalDate

/** The two spreadsheet formats the app exports (SPEC §6.1). */
enum class SpreadsheetFormat(val extension: String, val mimeType: String) {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ODS("ods", ODS_MIME_TYPE),
    ;

    /** Writes [sheets] as a file in this format. */
    fun write(sheets: List<Sheet>): ByteArray = when (this) {
        XLSX -> buildXlsx(sheets)
        ODS -> buildOds(sheets)
    }

    /** `lift-journal-2026-11-16.xlsx`, as the prototype names its downloads. */
    fun fileName(today: LocalDate): String = "lift-journal-${today.toStorageString()}.$extension"
}
