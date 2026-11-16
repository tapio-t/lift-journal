package com.example.liftjournal.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.liftjournal.data.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the exported schema. When the database version goes up, add a test here that creates the old
 * version with data, runs the migration and checks nothing was lost.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun version1DatabaseWithDataOpensWithCurrentSchema() = runTest {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO entries (date, text, updated_at) VALUES ('2026-11-09', 'squat 100 x 5', 1)")
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            TEST_DB,
        ).build()
        val entries = database.entryDao().getAll()
        database.close()

        assertEquals("squat 100 x 5", entries.single().text)
    }

    private companion object {
        const val TEST_DB = "schema-test.db"
    }
}
