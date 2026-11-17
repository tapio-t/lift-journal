package com.example.liftjournal.domain.backup

/** The only backup file in the backup folder, always current (PLAN phase 6). */
const val LATEST_BACKUP_FILE_NAME = "lift-journal-backup.json"

/** The latest backup is written here first and then renamed, so a crash never leaves a half-written file. */
const val PENDING_BACKUP_FILE_NAME = "lift-journal-backup-writing.json"
