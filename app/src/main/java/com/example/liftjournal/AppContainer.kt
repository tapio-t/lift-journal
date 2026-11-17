package com.example.liftjournal

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.liftjournal.backup.RealtimeBackupManager
import com.example.liftjournal.data.DataChangeNotifier
import com.example.liftjournal.data.DocumentFiles
import com.example.liftjournal.data.JournalRepository
import com.example.liftjournal.data.SettingsRepository
import com.example.liftjournal.data.SpreadsheetExporter
import com.example.liftjournal.data.UserDataRepository
import com.example.liftjournal.data.db.AppDatabase
import com.example.liftjournal.domain.time.SystemToday
import com.example.liftjournal.domain.time.TodayProvider

/** Creates the app's long-lived objects once (manual dependency injection, PLAN technical baseline). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val todayProvider: TodayProvider = SystemToday

    val changeNotifier = DataChangeNotifier()

    private val database = AppDatabase.open(appContext)

    val journalRepository = JournalRepository(database, changeNotifier)

    val settingsRepository = SettingsRepository(
        dataStore = PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile(SETTINGS_FILE) },
        changeNotifier = changeNotifier,
    )

    val userDataRepository = UserDataRepository(journalRepository, settingsRepository)

    val documentFiles = DocumentFiles(appContext.contentResolver)

    val spreadsheetExporter = SpreadsheetExporter(journalRepository, settingsRepository, documentFiles)

    val backupManager = RealtimeBackupManager(
        context = appContext,
        userDataRepository = userDataRepository,
        settingsRepository = settingsRepository,
        changeNotifier = changeNotifier,
    )

    private companion object {
        const val SETTINGS_FILE = "settings"
    }
}
