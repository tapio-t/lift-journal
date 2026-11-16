package com.example.liftjournal.ui.settings

import com.example.liftjournal.domain.date
import com.example.liftjournal.domain.entry
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.domain.settings.AppSettings
import com.example.liftjournal.domain.settings.BackupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsStateBuilderTest {
    private val today = date("2026-11-11")
    private val entries = listOf(
        entry("2026-11-02", "squat 130 x 5\nbench 80 x 5"),
        entry("2026-11-10", "squat 145 x 2\nfelt good"),
    )

    private fun state(inputs: SettingsInputs = SettingsInputs(), goals: List<Goal> = emptyList()) =
        buildSettingsState(entries, goals, AppSettings(), inputs, today)

    @Test
    fun `counts entries and workouts`() {
        val state = state()
        assertEquals(2, state.entryCount)
        assertEquals(2, state.workoutCount)
    }

    @Test
    fun `goal form defaults to the most logged exercise and suggests 10 percent more`() {
        val form = state().goalForm
        assertEquals("squat", form.exerciseKey)
        // Heaviest squat in the last 4 weeks is 145; 145 × 1.1 = 159.5, rounded up to 160.
        assertEquals(160.0, form.suggestedTargetKg!!, 0.0)
    }

    @Test
    fun `a picked exercise that no longer exists falls back to the default`() {
        assertEquals("squat", state(SettingsInputs(exerciseKey = "deadlift")).goalForm.exerciseKey)
        assertEquals("bench", state(SettingsInputs(exerciseKey = "bench")).goalForm.exerciseKey)
    }

    @Test
    fun `exercise goals cannot be set before anything is logged`() {
        val form = buildSettingsState(emptyList(), emptyList(), AppSettings(), SettingsInputs(), today).goalForm
        assertNull(form.exerciseKey)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `backup folder state is empty until a folder is chosen`() {
        assertEquals(BackupFolderState(), state().backup)

        val settings = AppSettings(backup = BackupStatus("content://tree/1", 1_000L, hasProblem = true))
        val backup = buildSettingsState(entries, emptyList(), settings, SettingsInputs(), today, "LiftJournal").backup
        assertEquals(BackupFolderState(true, "LiftJournal", 1_000L, hasProblem = true), backup)
    }

    @Test
    fun `goal rows show the current level and percentage`() {
        val row = state(goals = listOf(Goal(3, GoalType.TOP_SET, "squat", null, 160.0))).goals.single()
        assertEquals("Squat", row.goal.exerciseName)
        assertEquals(145.0, row.currentKg, 0.0)
        assertEquals(91, row.percent)
    }
}
