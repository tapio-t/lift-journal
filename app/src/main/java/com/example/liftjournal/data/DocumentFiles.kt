package com.example.liftjournal.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads and writes documents the user picked through the Storage Access Framework (no storage permissions). */
class DocumentFiles(private val contentResolver: ContentResolver) {
    /** Reads a whole document. Callers decode it, since imported text files may not be UTF-8. */
    suspend fun readBytes(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val stream = contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        stream.use { it.readBytes() }
    }

    /** Replaces a document's content with [bytes]. Mode "wt" truncates, so a shorter file leaves no old tail. */
    suspend fun write(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
        stream.use { it.write(bytes) }
    }
}
