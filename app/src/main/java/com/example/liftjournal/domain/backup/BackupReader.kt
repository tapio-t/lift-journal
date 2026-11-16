package com.example.liftjournal.domain.backup

import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.domain.journal.appendEntryText
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.parser.normalizeExerciseName
import com.example.liftjournal.domain.settings.ThemeMode
import com.example.liftjournal.domain.settings.UserSettings
import com.example.liftjournal.domain.time.parseStorageDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** The backup format version the prototype and this app write (SPEC §7.1). */
const val BACKUP_FORMAT_VERSION = 2

private val BYTE_ORDER_MARK = Char(0xFEFF)

/**
 * Reads a backup file (prototype restore handler). Version 2 files are read as they are; any other file with an
 * `entries` array is treated as a version 1 prototype backup and converted. Returns null when the text isn't a
 * Lift Journal backup at all.
 */
fun readBackup(text: String): BackupData? {
    val root = runCatching { Json.parseToJsonElement(text.trimStart(BYTE_ORDER_MARK)) }.getOrNull() as? JsonObject
        ?: return null
    if (root["entries"] !is JsonArray) return null
    val isVersion2 = root.numberField("v") == BACKUP_FORMAT_VERSION.toDouble()
    return if (isVersion2) readVersion2(root) else readVersion1(root)
}

/**
 * Entries keep only their date and text; the prototype's derived `ex` arrays are ignored and the text is
 * re-parsed (SPEC §7.1).
 */
private fun readVersion2(root: JsonObject): BackupData {
    val entries = root.arrayField("entries").mapNotNull { item ->
        val date = item.stringField("date")?.let(::parseStorageDate) ?: return@mapNotNull null
        StoredEntry(date, item.stringField("text").orEmpty())
    }
    val settings = root.objectField("settings") ?: JsonObject(emptyMap())
    return BackupData(
        entries = mergeEntriesByDate(entries),
        goals = settings.arrayField("goals").mapNotNull(::readGoal),
        settings = UserSettings(
            theme = ThemeMode.fromStorageKey(settings.stringField("theme")),
            // The prototype treats anything but an explicit false as on.
            ghostText = settings.booleanField("ghost") != false,
            bodyweightKg = settings.numberField("bodyweight")?.takeIf { it > 0 },
        ),
    )
}

/**
 * Entries are unique per date and never empty: blank ones are dropped, and a date that appears twice keeps
 * both texts, one below the other. Sorted oldest first.
 */
internal fun mergeEntriesByDate(entries: List<StoredEntry>): List<StoredEntry> = entries.groupBy { it.date }
    .map { (date, sameDay) ->
        StoredEntry(date, sameDay.fold("") { text, entry -> appendEntryText(text, entry.text) })
    }
    .filter { it.text.isNotBlank() }
    .sortedBy { it.date }

/** A goal as the prototype stores it: `{type, ex, per, target}`. Goals that can't work are skipped. */
private fun readGoal(item: JsonObject): Goal? {
    val type = GoalType.fromStorageKey(item.stringField("type")) ?: return null
    val target = item.numberField("target")?.takeIf { it > 0 } ?: return null
    val exerciseKey = item.stringField("ex")?.let(::normalizeExerciseName)?.takeIf { it.isNotEmpty() }
    if (type.needsExercise && exerciseKey == null) return null
    val period = if (type.needsPeriod) GoalPeriod.fromStorageKey(item.stringField("per")) ?: GoalPeriod.WEEK else null
    return Goal(
        id = 0,
        type = type,
        exerciseKey = exerciseKey.takeIf { type.needsExercise },
        period = period,
        targetKg = target,
    )
}
