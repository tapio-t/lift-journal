package com.example.liftjournal.domain.backup

import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.settings.UserSettings

/**
 * Everything a backup holds (SPEC §7.1): entries, goals and the user's settings.
 * Goal ids are not part of backups; restored goals get new ids in their backup order.
 */
data class BackupData(val entries: List<StoredEntry>, val goals: List<Goal>, val settings: UserSettings)
