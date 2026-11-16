package com.example.liftjournal.backup

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.liftjournal.domain.backup.LATEST_BACKUP_FILE_NAME
import com.example.liftjournal.domain.backup.SNAPSHOTS_TO_KEEP
import com.example.liftjournal.domain.backup.snapshotFileName
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/** Uses a plain directory as the folder; the file operations are the same ones SAF folders get. */
@RunWith(AndroidJUnit4::class)
class BackupFolderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var folder: BackupFolder

    @Before
    fun setUp() {
        directory = File(context.cacheDir, "backup-test").apply {
            deleteRecursively()
            mkdirs()
        }
        folder = BackupFolder(DocumentFile.fromFile(directory), context.contentResolver)
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun latestBackupIsReplacedAndNoPendingFileIsLeft() {
        folder.writeLatest("""{"v":2,"entries":[]}""")
        folder.writeLatest("""{"v":2}""")

        assertEquals(listOf(LATEST_BACKUP_FILE_NAME), directory.list()!!.toList())
        assertEquals("""{"v":2}""", File(directory, LATEST_BACKUP_FILE_NAME).readText())
    }

    @Test
    fun snapshotOfTheSameDayIsOverwritten() {
        val today = LocalDate.of(2026, 11, 16)
        folder.writeSnapshot(today, "first")
        folder.writeSnapshot(today, "second")

        assertEquals("second", File(directory, snapshotFileName(today)).readText())
    }

    @Test
    fun onlyTheNewestSnapshotsAreKept() {
        val firstDay = LocalDate.of(2026, 11, 1)
        repeat(SNAPSHOTS_TO_KEEP + 2) { day -> folder.writeSnapshot(firstDay.plusDays(day.toLong()), "{}") }
        File(directory, "notes.txt").writeText("keep me")

        folder.deleteOldSnapshots()

        val names = directory.list()!!.toSet()
        assertEquals(SNAPSHOTS_TO_KEEP + 1, names.size)
        assertFalse(snapshotFileName(firstDay) in names)
        assertFalse(snapshotFileName(firstDay.plusDays(1)) in names)
    }
}
