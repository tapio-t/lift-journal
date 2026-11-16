package com.example.liftjournal.domain.backup

import com.example.liftjournal.domain.time.toStorageString
import java.time.LocalDate

/** The always-current backup in the backup folder (PLAN phase 6). */
const val LATEST_BACKUP_FILE_NAME = "lift-journal-backup.json"

/** The latest backup is written here first and then renamed, so a crash never leaves a half-written file. */
const val PENDING_BACKUP_FILE_NAME = "lift-journal-backup-writing.json"

/** How many daily snapshots to keep. */
const val SNAPSHOTS_TO_KEEP = 14

private val SNAPSHOT_NAME = Regex("""lift-journal-\d{4}-\d{2}-\d{2}\.json""")

/** `lift-journal-2026-11-16.json`: one snapshot per day. */
fun snapshotFileName(date: LocalDate): String = "lift-journal-${date.toStorageString()}.json"

/**
 * The snapshot files to delete so only the [SNAPSHOTS_TO_KEEP] newest remain. Other files in the folder are
 * never touched. The ISO date in the name makes alphabetical order the same as date order.
 */
fun snapshotsToDelete(fileNames: List<String>): List<String> =
    fileNames.filter { SNAPSHOT_NAME.matches(it) }.sortedDescending().drop(SNAPSHOTS_TO_KEEP)
