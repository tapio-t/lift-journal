package com.example.liftjournal.domain.backup

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.domain.settings.UserSettings
import com.example.liftjournal.readTestResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupReaderTest {
    @Test
    fun `prototype sample backup restores entries goals and settings`() {
        val backup = requireNotNull(readBackup(readTestResource("prototype-backup-sample.json")))

        assertEquals(date("2026-11-02"), backup.entries.first().date)
        assertEquals("squat 120 x 5, 5, 5\nbench press 82.5 x 5, 5, 4", backup.entries.first().text)
        assertEquals(
            listOf(
                Goal(0, GoalType.TOP_SET, "squat", null, 140.0),
                Goal(0, GoalType.TOTAL_VOLUME, null, GoalPeriod.WEEK, 25_000.0),
            ),
            backup.goals,
        )
        assertEquals(UserSettings(ThemeMode.SYSTEM, ghostText = true, bodyweightKg = 80.0), backup.settings)
    }

    @Test
    fun `entries are sorted oldest first and blank ones are dropped`() {
        val backup = readBackup(
            """{"v":2,"entries":[{"date":"2026-11-04","text":"b 1 x 1"},{"date":"2026-11-01","text":"  "},""" +
                """{"date":"2026-11-02","text":"a 1 x 1"}]}""",
        )
        assertEquals(listOf(date("2026-11-02"), date("2026-11-04")), backup?.entries?.map { it.date })
    }

    @Test
    fun `a date that appears twice keeps both texts`() {
        val backup = readBackup(
            """{"v":2,"entries":[{"date":"2026-11-02","text":"squat 100 x 5"},""" +
                """{"date":"2026-11-02","text":"bench 60 x 5"}]}""",
        )
        assertEquals(listOf(StoredEntry(date("2026-11-02"), "squat 100 x 5\nbench 60 x 5")), backup?.entries)
    }

    @Test
    fun `missing settings fall back to defaults`() {
        val backup = readBackup("""{"v":2,"entries":[]}""")
        assertEquals(UserSettings(), backup?.settings)
        assertEquals(emptyList<Goal>(), backup?.goals)
    }

    @Test
    fun `ghost text is on unless it is explicitly false`() {
        val off = readBackup("""{"v":2,"entries":[],"settings":{"ghost":false,"theme":"dark","bodyweight":0}}""")
        assertEquals(UserSettings(ThemeMode.DARK, ghostText = false, bodyweightKg = null), off?.settings)
    }

    @Test
    fun `invalid goals are skipped and exercise keys normalised`() {
        val backup = readBackup(
            """{"v":2,"entries":[],"settings":{"goals":[""" +
                """{"type":"top","ex":"Bench  Press","per":"week","target":"100"},""" +
                """{"type":"avg","ex":null,"target":90},""" +
                """{"type":"vol","ex":"squat","per":"month","target":0},""" +
                """{"type":"exvol","ex":"squat","per":"month","target":5000},""" +
                """{"type":"nope","target":5}]}}""",
        )
        assertEquals(
            listOf(
                Goal(0, GoalType.TOP_SET, "bench press", null, 100.0),
                Goal(0, GoalType.EXERCISE_VOLUME, "squat", GoalPeriod.MONTH, 5_000.0),
            ),
            backup?.goals,
        )
    }

    @Test
    fun `version 1 backup is converted like the prototype migrate`() {
        val json = """
            {"entries":[
              {"date":"2025-03-03","logged":[{"name":"Squat","sets":[{"w":100,"r":5},{"w":100,"r":5},{"w":110,"r":3}]},
                {"name":"Pull-up","sets":[{"w":0,"r":8,"bw":1},{"w":0,"r":6,"bw":0.5}]}],"notes":"felt good"},
              {"date":"2025-03-03","logged":[{"name":"Bench","sets":[{"w":60,"r":5}]}],"logText":"bench 60 x 5"},
              {"date":"2025-03-04","logged":[],"notes":"rest day"}
            ],"settings":{"theme":"light"}}
        """.trimIndent()
        val backup = requireNotNull(readBackup(json))

        assertEquals(
            listOf(
                StoredEntry(
                    date("2025-03-03"),
                    "Squat 100 x 5, 5, 110 x 3\nPull-up bw x 8, 0.5 bw x 6\nfelt good\nbench 60 x 5",
                ),
            ),
            backup.entries,
        )
        assertEquals(UserSettings(theme = ThemeMode.LIGHT), backup.settings)
        assertEquals(emptyList<Goal>(), backup.goals)
    }

    @Test
    fun `a byte order mark is ignored`() {
        assertEquals(0, readBackup("${Char(0xFEFF)}{\"v\":2,\"entries\":[]}")?.entries?.size)
    }

    @Test
    fun `files that are not backups are rejected`() {
        assertNull(readBackup("not json"))
        assertNull(readBackup("[1, 2]"))
        assertNull(readBackup("""{"v":2,"entries":"none"}"""))
        assertNull(readBackup("""{"something":"else"}"""))
    }
}
