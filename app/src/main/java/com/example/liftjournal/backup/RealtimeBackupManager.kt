package com.example.liftjournal.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.liftjournal.data.DataChangeNotifier
import com.example.liftjournal.data.SettingsRepository
import com.example.liftjournal.data.UserDataRepository
import com.example.liftjournal.domain.backup.writeBackup
import com.example.liftjournal.domain.time.TodayProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Changes are written about 2 s after the last one, so typing doesn't cause a write per keystroke. */
private const val DEBOUNCE_SECONDS = 2L
private const val UNIQUE_WORK_NAME = "realtime-backup"
private const val FOLDER_PERMISSIONS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

/** How a backup attempt went. */
enum class BackupOutcome {
    SUCCESS,

    /** No folder is chosen, so there is nothing to do. */
    NO_FOLDER,

    /** The folder is gone or the permission was revoked; Settings and Journals warn about it. */
    FOLDER_UNAVAILABLE,

    /** Writing failed, e.g. the storage is full; worth retrying later. */
    WRITE_FAILED,
}

/**
 * Real-time backup to the folder the user chose (PLAN phase 6). Every committed change schedules a backup through
 * WorkManager, so it is written even if the app is closed right after the change.
 */
class RealtimeBackupManager(
    private val context: Context,
    private val userDataRepository: UserDataRepository,
    private val settingsRepository: SettingsRepository,
    private val changeNotifier: DataChangeNotifier,
    private val todayProvider: TodayProvider,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val hasPendingChange = AtomicBoolean(false)

    // A manual "Back up now" and the scheduled worker must not write the same files at once.
    private val writeLock = Mutex()

    /** Starts watching for data changes and checks that the folder is still reachable. Call once at app start. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            changeNotifier.changes.collect {
                hasPendingChange.set(true)
                scheduleBackup(delaySeconds = DEBOUNCE_SECONDS)
            }
        }
        scope.launch { checkFolderAccess() }
    }

    /** The app went to the background: write a pending change now instead of waiting for the delay. */
    fun flushPendingChange() {
        if (hasPendingChange.get()) scheduleBackup(delaySeconds = 0)
    }

    /** Writes the latest backup and today's snapshot now. */
    suspend fun backUpNow(): BackupOutcome = writeLock.withLock {
        hasPendingChange.set(false)
        val folderUri = settingsRepository.current().backup.folderUri ?: return BackupOutcome.NO_FOLDER
        val folder = openFolder(Uri.parse(folderUri))
        if (folder == null) {
            settingsRepository.recordBackupProblem()
            return BackupOutcome.FOLDER_UNAVAILABLE
        }
        val json = writeBackup(userDataRepository.currentBackup())
        val written = writeFiles(folder, json)
        if (written) settingsRepository.recordBackupSuccess(currentTimeMillis())
        return if (written) BackupOutcome.SUCCESS else BackupOutcome.WRITE_FAILED
    }

    /** Keeps access to a newly chosen folder across restarts, forgets the old one, and backs up right away. */
    suspend fun useFolder(folder: Uri): BackupOutcome {
        context.contentResolver.takePersistableUriPermission(folder, FOLDER_PERMISSIONS)
        val previous = settingsRepository.current().backup.folderUri?.let(Uri::parse)
        if (previous != null && previous != folder) releasePermission(previous)
        settingsRepository.setBackupFolder(folder.toString())
        return backUpNow()
    }

    /** Flags a lost folder, or retries a backup once a folder that was unreachable is back. */
    suspend fun checkFolderAccess() {
        val backup = settingsRepository.current().backup
        val folderUri = backup.folderUri ?: return
        val isReachable = openFolder(Uri.parse(folderUri)) != null
        when {
            !isReachable -> settingsRepository.recordBackupProblem()
            backup.hasProblem -> backUpNow()
        }
    }

    /** The folder's name as the file picker shows it, e.g. `LiftJournal`. */
    suspend fun folderName(folderUri: String): String? = withContext(Dispatchers.IO) {
        runCatching { DocumentFile.fromTreeUri(context, Uri.parse(folderUri))?.name }.getOrNull()
    }

    private fun scheduleBackup(delaySeconds: Long) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        // REPLACE restarts the delay on every change, which debounces quick edits into one write.
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** The folder, if the app still holds write permission and the folder still exists. */
    private suspend fun openFolder(folderUri: Uri): BackupFolder? = withContext(Dispatchers.IO) {
        val hasPermission = context.contentResolver.persistedUriPermissions
            .any { it.uri == folderUri && it.isWritePermission }
        if (!hasPermission) return@withContext null
        val root = runCatching { DocumentFile.fromTreeUri(context, folderUri) }.getOrNull()
        root?.takeIf { it.exists() && it.canWrite() }?.let { BackupFolder(it, context.contentResolver) }
    }

    private suspend fun writeFiles(folder: BackupFolder, json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            folder.writeLatest(json)
            folder.writeSnapshot(todayProvider.today(), json)
            folder.deleteOldSnapshots()
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun releasePermission(folder: Uri) {
        runCatching { context.contentResolver.releasePersistableUriPermission(folder, FOLDER_PERMISSIONS) }
    }
}
