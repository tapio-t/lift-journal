package com.example.liftjournal.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads and writes documents the user picked through the Storage Access Framework (no storage permissions). */
class DocumentFiles(private val contentResolver: ContentResolver) {
    /** Reads a whole document as UTF-8 text. */
    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** Replaces a document's content with [bytes]. Mode "wt" truncates, so a shorter file leaves no old tail. */
    suspend fun write(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
        stream.use { it.write(bytes) }
    }
}
