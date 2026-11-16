package com.example.liftjournal.ui.settings

import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.goalPercent
import com.example.liftjournal.domain.goals.goalProgress
import com.example.liftjournal.domain.goals.suggestedTarget
import com.example.liftjournal.domain.importer.ImportPreview
import com.example.liftjournal.domain.importer.previewImport
import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.settings.AppSettings
import com.example.liftjournal.domain.settings.BackupStatus
import com.example.liftjournal.domain.stats.exerciseDisplayName
import com.example.liftjournal.domain.stats.knownExercises
import com.example.liftjournal.ui.common.NamedGoal
import java.time.LocalDate

/** Combines the stored data with what the user is doing on the screen. */
internal fun buildSettingsState(
    entries: List<JournalEntry>,
    goals: List<Goal>,
    settings: AppSettings,
    inputs: SettingsInputs,
    today: LocalDate,
    backupFolderName: String? = null,
): SettingsUiState {
    val user = settings.user
    return SettingsUiState(
        today = today,
        entryCount = entries.size,
        workoutCount = entries.count { it.isWorkout },
        goals = goals.map { goalRow(it, entries, today, user.bodyweightKg) },
        goalForm = goalForm(entries, inputs, today, user.bodyweightKg),
        bodyweightKg = user.bodyweightKg,
        ghostText = user.ghostText,
        theme = user.theme,
        backup = backupFolderState(settings.backup, backupFolderName),
        dialog = inputs.dialog,
        importPreview = importPreview(inputs.dialog, entries),
    )
}

private fun importPreview(dialog: SettingsDialog?, entries: List<JournalEntry>): ImportPreview? {
    if (dialog !is SettingsDialog.ConfirmImport) return null
    return previewImport(dialog.textImport, entries.associate { it.date to it.text }, dialog.mode)
}

private fun backupFolderState(status: BackupStatus, folderName: String?): BackupFolderState {
    if (status.folderUri == null) return BackupFolderState()
    return BackupFolderState(
        isChosen = true,
        folderName = folderName,
        lastBackupAtMillis = status.lastBackupAtMillis,
        hasProblem = status.hasProblem,
    )
}

private fun goalRow(goal: Goal, entries: List<JournalEntry>, today: LocalDate, bodyweightKg: Double?): GoalRow {
    val progress = goalProgress(goal, entries, today, bodyweightKg)
    return GoalRow(
        goal = NamedGoal(goal, exerciseDisplayName(goal.exerciseKey.orEmpty(), entries)),
        currentKg = progress.currentKg,
        percent = goalPercent(goal, progress),
    )
}

/**
 * The exercise defaults to the most logged one until the user picks another (prototype `goalsHtml`).
 * The target placeholder suggests about 10% above the current level of the goal being drafted.
 */
private fun goalForm(
    entries: List<JournalEntry>,
    inputs: SettingsInputs,
    today: LocalDate,
    bodyweightKg: Double?,
): GoalForm {
    val exercises = knownExercises(entries)
    val type = inputs.goalType
    val exerciseKey = inputs.exerciseKey?.takeIf { key -> exercises.any { it.key == key } }
        ?: exercises.firstOrNull()?.key
    val canMeasure = !type.needsExercise || exerciseKey != null
    // Only the current level is needed here, and that doesn't depend on the target.
    val draft = Goal(
        id = 0,
        type = type,
        exerciseKey = exerciseKey.takeIf { type.needsExercise },
        period = inputs.period.takeIf { type.needsPeriod },
        targetKg = 1.0,
    )
    val suggestion =
        if (canMeasure) suggestedTarget(type, goalProgress(draft, entries, today, bodyweightKg).currentKg) else null
    return GoalForm(
        type = type,
        exercises = exercises,
        exerciseKey = exerciseKey,
        period = inputs.period,
        targetText = inputs.targetText,
        suggestedTargetKg = suggestion,
    )
}
