package com.example.liftjournal.data

import android.net.Uri
import com.example.liftjournal.domain.export.SpreadsheetFormat
import com.example.liftjournal.domain.export.buildExportSheets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Writes the journal as a spreadsheet to a document the user created (SPEC §6.1). */
class SpreadsheetExporter(
    private val journalRepository: JournalRepository,
    private val settingsRepository: SettingsRepository,
    private val documentFiles: DocumentFiles,
) {
    suspend fun export(format: SpreadsheetFormat, destination: Uri) {
        val entries = journalRepository.entries.first()
        val bodyweightKg = settingsRepository.current().user.bodyweightKg
        val bytes = withContext(Dispatchers.Default) { format.write(buildExportSheets(entries, bodyweightKg)) }
        documentFiles.write(destination, bytes)
    }
}
