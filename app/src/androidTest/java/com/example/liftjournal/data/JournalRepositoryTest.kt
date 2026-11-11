package com.example.liftjournal.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.liftjournal.data.db.AppDatabase
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class JournalRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: JournalRepository
    private val notifier = DataChangeNotifier()
    private val monday = LocalDate.of(2026, 11, 9)
    private val tuesday = LocalDate.of(2026, 11, 10)

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repository = JournalRepository(database, notifier) { 1_000L }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun savedEntryIsReadBackParsedAndNewestFirst() = runTest {
        repository.saveEntry(monday, "squat 100 x 5")
        repository.saveEntry(tuesday, "bench 60 x 5")

        val entries = repository.entries.first()
        assertEquals(listOf(tuesday, monday), entries.map { it.date })
        assertEquals("Squat", entries[1].exercises.single().name)
    }

    @Test
    fun savingBlankTextDeletesTheEntry() = runTest {
        repository.saveEntry(monday, "squat 100 x 5")
        repository.saveEntry(monday, "   ")
        assertFalse(repository.hasEntry(monday))
    }

    @Test
    fun deleteReturnsEntryAndRestoreBringsItBack() = runTest {
        repository.saveEntry(monday, "squat 100 x 5")
        val deleted = repository.deleteEntry(monday)
        assertFalse(repository.hasEntry(monday))

        repository.restoreEntry(requireNotNull(deleted))
        assertEquals("squat 100 x 5", repository.entries.first().single().text)
    }

    @Test
    fun movingOntoAnExistingEntryAppendsBelowIt() = runTest {
        repository.saveEntry(monday, "squat 100 x 5")
        repository.saveEntry(tuesday, "bench 60 x 5")

        repository.moveEntry(from = monday, to = tuesday)

        val entries = repository.entries.first()
        assertEquals(listOf(tuesday), entries.map { it.date })
        assertEquals("bench 60 x 5\nsquat 100 x 5", entries.single().text)
    }

    @Test
    fun movingToAnEmptyDateKeepsTheText() = runTest {
        repository.saveEntry(monday, "squat 100 x 5")
        repository.moveEntry(from = monday, to = tuesday)
        assertNull(repository.deleteEntry(monday))
        assertTrue(repository.hasEntry(tuesday))
    }

    @Test
    fun goalsKeepTheirOrderAndCanBeRemoved() = runTest {
        val first = repository.addGoal(Goal(0, GoalType.TOP_SET, "squat", null, 160.0))
        repository.addGoal(Goal(0, GoalType.TOTAL_VOLUME, null, GoalPeriod.WEEK, 20_000.0))

        assertEquals(listOf(GoalType.TOP_SET, GoalType.TOTAL_VOLUME), repository.goals.first().map { it.type })
        repository.removeGoal(first)
        assertEquals(listOf(GoalType.TOTAL_VOLUME), repository.goals.first().map { it.type })
    }

    @Test
    fun snapshotReturnsEntriesOldestFirst() = runTest {
        repository.saveEntry(tuesday, "b 1 x 1")
        repository.saveEntry(monday, "a 1 x 1")
        assertEquals(listOf(monday, tuesday), repository.snapshot().first.map { it.date })
    }
}
