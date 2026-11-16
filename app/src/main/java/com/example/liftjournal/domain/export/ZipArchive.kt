package com.example.liftjournal.domain.export

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A file inside a zip archive. [isStored] writes it uncompressed, which OpenDocument requires for its
 * `mimetype` file so apps can recognise the format from the first bytes.
 */
class ArchiveFile(val path: String, val content: String, val isStored: Boolean = false)

/** Zips [files] in order. Both .xlsx and .ods are zip archives of XML files. */
fun zipArchive(files: List<ArchiveFile>): ByteArray {
    val buffer = ByteArrayOutputStream()
    ZipOutputStream(buffer).use { zip ->
        files.forEach { file ->
            val bytes = file.content.toByteArray(Charsets.UTF_8)
            zip.putNextEntry(zipEntry(file, bytes))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return buffer.toByteArray()
}

private fun zipEntry(file: ArchiveFile, bytes: ByteArray): ZipEntry {
    val entry = ZipEntry(file.path)
    if (!file.isStored) return entry
    // An uncompressed entry must declare its size and checksum up front.
    entry.method = ZipEntry.STORED
    entry.size = bytes.size.toLong()
    entry.compressedSize = bytes.size.toLong()
    entry.crc = CRC32().apply { update(bytes) }.value
    return entry
}
