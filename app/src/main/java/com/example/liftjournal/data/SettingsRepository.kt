package com.example.liftjournal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.liftjournal.domain.settings.AppSettings
import com.example.liftjournal.domain.settings.BackupStatus
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.domain.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Settings in DataStore. Changes to [UserSettings] are part of the user's data and trigger a backup;
 * backup bookkeeping (folder, last time, problem flag) does not.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val changeNotifier: DataChangeNotifier,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setTheme(theme: ThemeMode) = editUserSettings { it[Keys.THEME] = theme.storageKey }

    suspend fun setGhostText(enabled: Boolean) = editUserSettings { it[Keys.GHOST_TEXT] = enabled }

    /** Sets the bodyweight in kg, or clears it with null. */
    suspend fun setBodyweight(kg: Double?) {
        val validKg = kg?.takeIf { it > 0 }
        editUserSettings { it.putOrRemove(Keys.BODYWEIGHT_KG, validKg) }
    }

    suspend fun setBackupFolder(uri: String?) {
        dataStore.edit {
            it.putOrRemove(Keys.BACKUP_FOLDER_URI, uri)
            it[Keys.BACKUP_PROBLEM] = false
        }
    }

    suspend fun recordBackupSuccess(atMillis: Long) {
        dataStore.edit {
            it[Keys.LAST_BACKUP_AT] = atMillis
            it[Keys.BACKUP_PROBLEM] = false
        }
    }

    suspend fun recordBackupProblem() {
        dataStore.edit { it[Keys.BACKUP_PROBLEM] = true }
    }

    private suspend fun editUserSettings(change: (MutablePreferences) -> Unit) {
        dataStore.edit(change)
        changeNotifier.notifyChanged()
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val GHOST_TEXT = booleanPreferencesKey("ghost_text")
        val BODYWEIGHT_KG = doublePreferencesKey("bodyweight_kg")
        val BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val BACKUP_PROBLEM = booleanPreferencesKey("backup_problem")
    }

    private fun Preferences.toAppSettings() = AppSettings(
        user = UserSettings(
            theme = ThemeMode.fromStorageKey(this[Keys.THEME]),
            ghostText = this[Keys.GHOST_TEXT] ?: true,
            bodyweightKg = this[Keys.BODYWEIGHT_KG],
        ),
        backup = BackupStatus(
            folderUri = this[Keys.BACKUP_FOLDER_URI],
            lastBackupAtMillis = this[Keys.LAST_BACKUP_AT],
            hasProblem = this[Keys.BACKUP_PROBLEM] ?: false,
        ),
    )
}

private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
    if (value == null) remove(key) else this[key] = value
}
