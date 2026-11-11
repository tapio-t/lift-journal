package com.example.liftjournal.ui.journals

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.liftjournal.data.DataChangeNotifier
import com.example.liftjournal.data.JournalRepository
import com.example.liftjournal.data.SettingsRepository
import com.example.liftjournal.data.db.AppDatabase
import com.example.liftjournal.ui.theme.JournalsPalette
import com.example.liftjournal.ui.theme.LiftJournalTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class JournalsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 11)
    private lateinit var database: AppDatabase
    private lateinit var repository: JournalRepository
    private lateinit var settingsFile: File
    private lateinit var viewModel: JournalsViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val notifier = DataChangeNotifier()
        repository = JournalRepository(database, notifier)
        settingsFile = File(context.cacheDir, "test-${UUID.randomUUID()}.preferences_pb")
        val settings = SettingsRepository(PreferenceDataStoreFactory.create { settingsFile }, notifier)
        runBlocking {
            repository.saveEntry(LocalDate.of(2026, 11, 9), "squat 100 x 5")
            repository.saveEntry(LocalDate.of(2026, 10, 20), "bench 60 x 5")
        }
        viewModel = JournalsViewModel(repository, settings, { today }, SavedStateHandle())
        compose.setContent { LiftJournalTheme(JournalsPalette) { JournalsScreen(viewModel) } }
    }

    @After
    fun tearDown() {
        database.close()
        settingsFile.delete()
    }

    @Test
    fun deleteAsksForConfirmationAndUndoBringsTheEntryBack() {
        compose.onNodeWithContentDescription("Edit entry 09.11.2026").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Do you really want to delete journal entry?").assertExists()
        compose.onNodeWithText("Delete").performClick()

        compose.waitUntilDoesNotExist(hasText("squat 100 x 5"))
        compose.onNodeWithText("Undo").performClick()

        compose.waitUntilExactlyOneExists(hasText("squat 100 x 5"))
    }

    @Test
    fun cancellingTheDeleteQuestionKeepsTheEntry() {
        compose.onNodeWithContentDescription("Edit entry 09.11.2026").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("squat 100 x 5").assertExists()
    }

    @Test
    fun searchShowsOnlyMatchingEntries() {
        compose.onNodeWithContentDescription("Search journals").performClick()
        compose.onNodeWithContentDescription("Search journals").performTextInput("bench")

        compose.waitUntilDoesNotExist(hasText("squat 100 x 5"))
        compose.onNodeWithText("bench 60 x 5").assertExists()
        compose.onNodeWithText("11.11.2026").assertDoesNotExist()
    }

    @Test
    fun monthTagFiltersAndClearRestoresTheList() {
        compose.onNodeWithContentDescription("Search journals").performClick()
        compose.onNodeWithText("Oct 2026").performClick()
        compose.waitUntilDoesNotExist(hasText("squat 100 x 5"))

        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.waitUntilExactlyOneExists(hasText("squat 100 x 5"))
        compose.onNodeWithText("11.11.2026").assertExists()
    }

    @Test
    fun changeDateOntoAnExistingEntryAsksAndMerges() {
        compose.onNodeWithContentDescription("Edit entry 20.10.2026").performClick()
        compose.onNodeWithText("Change date").performClick()
        compose.onNodeWithText("OK").assertExists()

        // The Material date picker is driven by the view model here; picking a day by touch is flaky in tests.
        compose.runOnIdle { viewModel.onMoveDatePicked(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 9)) }
        compose.waitUntilExactlyOneExists(hasText("Merge"))
        compose.onNodeWithText("Merge").performClick()

        compose.waitUntilExactlyOneExists(hasText("squat 100 x 5\nbench 60 x 5"))
        compose.onNodeWithText("20.10.2026").assertDoesNotExist()
    }

    @Test
    fun typingInTodaysEntrySavesIt() {
        compose.onNodeWithContentDescription("Journal 11.11.2026").performTextInput("deadlift 150 x 5")
        compose.waitUntil(timeoutMillis = 3_000) {
            runBlocking { repository.entries.first().any { it.date == today } }
        }
        val saved = runBlocking { repository.entries.first().first { it.date == today } }
        assertEquals("deadlift 150 x 5", saved.text)
        compose.waitUntilExactlyOneExists(hasContentDescription("Edit entry 11.11.2026"))
    }
}
