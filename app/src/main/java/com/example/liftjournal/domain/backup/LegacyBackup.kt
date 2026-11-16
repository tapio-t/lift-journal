package com.example.liftjournal.domain.backup

import com.example.liftjournal.domain.format.formatPlainNumber
import com.example.liftjournal.domain.format.formatWeight
import com.example.liftjournal.domain.journal.appendEntryText
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.domain.settings.UserSettings
import com.example.liftjournal.domain.time.parseStorageDate
import kotlinx.serialization.json.JsonObject

private const val PLAIN_BODYWEIGHT = 1.0

/**
 * Converts a version 1 prototype backup (`liftjournal.v1`) the way the prototype's `migrate()` does.
 * Version 1 kept structured `logged` exercises plus optional `logText` and `notes`; only entries with logged
 * exercises are kept. Version 1 had no goals or bodyweight, so only the theme carries over.
 */
internal fun readVersion1(root: JsonObject): BackupData {
    val entries = root.arrayField("entries").mapNotNull(::readVersion1Entry)
    val theme = root.objectField("settings")?.stringField("theme")
    return BackupData(
        entries = mergeEntriesByDate(entries),
        goals = emptyList(),
        settings = UserSettings(theme = ThemeMode.fromStorageKey(theme)),
    )
}

private fun readVersion1Entry(item: JsonObject): StoredEntry? {
    val date = item.stringField("date")?.let(::parseStorageDate) ?: return null
    val logged = item.arrayField("logged")
    if (logged.isEmpty()) return null
    val logText = item.stringField("logText")?.takeIf { it.isNotEmpty() } ?: loggedExercisesToText(logged)
    return StoredEntry(date, appendEntryText(logText, item.stringField("notes").orEmpty()))
}

/** Writes structured exercises back as entry text, e.g. `Squat 100 x 5, 5, 110 x 3` (prototype `toText`). */
private fun loggedExercisesToText(exercises: List<JsonObject>): String = exercises.joinToString("\n") { exercise ->
    val name = exercise.stringField("name").orEmpty()
    val parts = mutableListOf<String>()
    var previousWeight: String? = null
    exercise.arrayField("sets").forEachIndexed { index, set ->
        val weight = writtenWeight(set)
        val reps = formatPlainNumber(set.numberField("r") ?: 0.0)
        parts += when {
            index == 0 -> "$name $weight x $reps"
            weight != previousWeight -> "$weight x $reps"
            else -> reps
        }
        previousWeight = weight
    }
    parts.joinToString(", ")
}

/** `80`, `bw` or `0.5 bw`: a set's weight as it would be typed. */
private fun writtenWeight(set: JsonObject): String {
    val multiplier = set.numberField("bw") ?: return formatWeight(set.numberField("w") ?: 0.0)
    return if (multiplier == PLAIN_BODYWEIGHT) "bw" else "${formatPlainNumber(multiplier)} bw"
}
