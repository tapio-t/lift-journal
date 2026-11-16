package com.example.liftjournal.data

import com.example.liftjournal.domain.backup.BackupData
import com.example.liftjournal.domain.settings.UserSettings

/** Actions on all of the user's data at once: entries, goals and settings together. */
class UserDataRepository(
    private val journalRepository: JournalRepository,
    private val settingsRepository: SettingsRepository,
) {
    /** Replaces all current data with a backup's (SPEC §6 "Restore from backup"). */
    suspend fun restore(backup: BackupData) {
        journalRepository.replaceAll(backup.entries, backup.goals)
        settingsRepository.replaceUserSettings(backup.settings)
    }

    /**
     * Erases entries, goals, bodyweight and the ghost text choice. The appearance setting stays, as in the
     * prototype, so the screen doesn't change colour under the user.
     */
    suspend fun eraseAll() {
        val theme = settingsRepository.current().user.theme
        journalRepository.replaceAll(entries = emptyList(), goals = emptyList())
        settingsRepository.replaceUserSettings(UserSettings(theme = theme))
    }
}
