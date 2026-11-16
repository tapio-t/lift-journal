package com.example.liftjournal.domain.settings

/** Settings that belong to the user's data and go into backups (SPEC §7, §7.1). */
data class UserSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val ghostText: Boolean = true,
    val bodyweightKg: Double? = null,
)

/** Where real-time backups go and how the last one went. Device-specific, so not part of backups. */
data class BackupStatus(
    val folderUri: String? = null,
    val lastBackupAtMillis: Long? = null,
    val hasProblem: Boolean = false,
)

/** All settings. */
data class AppSettings(val user: UserSettings = UserSettings(), val backup: BackupStatus = BackupStatus())
