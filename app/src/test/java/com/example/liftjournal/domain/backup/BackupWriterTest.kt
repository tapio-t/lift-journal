package com.example.liftjournal.domain.backup

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.domain.settings.UserSettings
import com.example.liftjournal.readTestResource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupWriterTest {
    private val backup = BackupData(
        entries = listOf(
            StoredEntry(date("2026-11-04"), "deadlift 150 x 5\n\"quoted\" note"),
            StoredEntry(date("2026-11-02"), "squat 120 x 5, 5, 5"),
        ),
        goals = listOf(
            Goal(0, GoalType.TOP_SET, "squat", null, 140.0),
            Goal(0, GoalType.TOTAL_VOLUME, null, GoalPeriod.WEEK, 25_000.0),
        ),
        settings = UserSettings(ThemeMode.DARK, ghostText = false, bodyweightKg = 80.5),
    )

    @Test
    fun `a written backup reads back the same, entries oldest first`() {
        val restored = readBackup(writeBackup(backup))
        assertEquals(backup.copy(entries = backup.entries.sortedBy { it.date }), restored)
    }

    @Test
    fun `the prototype sample survives a write and read`() {
        val sample = requireNotNull(readBackup(readTestResource("prototype-backup-sample.json")))
        assertEquals(sample, readBackup(writeBackup(sample)))
    }

    @Test
    fun `backups use the prototype version 2 layout`() {
        val root = Json.parseToJsonElement(writeBackup(backup)).jsonObject
        assertEquals("2", root.getValue("v").jsonPrimitive.content)
        val settings = root.getValue("settings").jsonObject
        assertEquals(setOf("theme", "ghost", "bodyweight", "goals"), settings.keys)
    }

    @Test
    fun `a missing bodyweight is written as null`() {
        val text = writeBackup(backup.copy(settings = UserSettings()))
        val settings = Json.parseToJsonElement(text).jsonObject.getValue("settings").jsonObject
        assertEquals("null", settings.getValue("bodyweight").toString())
    }

    @Test
    fun `snapshot names carry the date`() {
        assertEquals("lift-journal-2026-11-16.json", snapshotFileName(date("2026-11-16")))
    }

    @Test
    fun `only the 14 newest snapshots are kept and other files are left alone`() {
        val snapshots = (1..16).map { day -> snapshotFileName(date("2026-11-01").plusDays(day - 1L)) }
        val others = listOf(LATEST_BACKUP_FILE_NAME, "notes.txt", "lift-journal-2026-11-xx.json")

        assertEquals(
            listOf("lift-journal-2026-11-02.json", "lift-journal-2026-11-01.json"),
            snapshotsToDelete(others + snapshots.shuffled()),
        )
    }
}
