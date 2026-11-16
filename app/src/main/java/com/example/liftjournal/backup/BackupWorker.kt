package com.example.liftjournal.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.liftjournal.appContainer

/** A failed write is retried with WorkManager's backoff a few times, then left for the next change. */
private const val MAX_ATTEMPTS = 3

/** Runs a scheduled real-time backup, also after the app has been closed. */
class BackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val outcome = applicationContext.appContainer.backupManager.backUpNow()
        return when {
            outcome != BackupOutcome.WRITE_FAILED -> Result.success()
            runAttemptCount + 1 < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }
}
