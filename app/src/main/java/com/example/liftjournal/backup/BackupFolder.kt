package com.example.liftjournal.backup

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import com.example.liftjournal.domain.backup.LATEST_BACKUP_FILE_NAME
import com.example.liftjournal.domain.backup.PENDING_BACKUP_FILE_NAME
import java.io.IOException

private const val JSON_MIME_TYPE = "application/json"

/**
 * Writes backup files into the folder the user chose (PLAN phase 6). Blocking file I/O: call it off the main
 * thread. Throws [IOException] when a file can't be written.
 */
class BackupFolder(private val root: DocumentFile, private val contentResolver: ContentResolver) {
    /**
     * Replaces `lift-journal-backup.json`. The new content is written to a separate file first and then renamed,
     * so a crash never leaves a half-written backup. Providers that can't rename get the file rewritten in place.
     */
    fun writeLatest(json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val pending = writeFile(PENDING_BACKUP_FILE_NAME, bytes)
        root.findFile(LATEST_BACKUP_FILE_NAME)?.delete()
        if (pending.renameTo(LATEST_BACKUP_FILE_NAME)) return
        writeFile(LATEST_BACKUP_FILE_NAME, bytes)
        pending.delete()
    }

    private fun writeFile(name: String, bytes: ByteArray): DocumentFile {
        val file = root.findFile(name) ?: createFile(name)
        val stream = contentResolver.openOutputStream(file.uri, "wt") ?: throw IOException("Cannot write $name")
        stream.use { it.write(bytes) }
        return file
    }

    private fun createFile(name: String): DocumentFile {
        val file = root.createFile(JSON_MIME_TYPE, name) ?: throw IOException("Cannot create $name")
        // Some providers add their own extension (`.json.json`); put the intended name back where possible.
        if (file.name != name) file.renameTo(name)
        return file
    }
}
