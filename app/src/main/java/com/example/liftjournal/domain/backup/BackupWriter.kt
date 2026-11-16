package com.example.liftjournal.domain.backup

import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.time.toStorageString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Pretty-printed so a backup is readable in any text editor. */
private val BackupJson = Json { prettyPrint = true }

/**
 * Writes a backup in the prototype's version 2 format (SPEC §7.1), so the prototype can restore it too.
 * The prototype's derived `ex` arrays are left out: both apps re-parse the entry text on restore.
 */
fun writeBackup(backup: BackupData): String {
    val root = buildJsonObject {
        put("v", BACKUP_FORMAT_VERSION)
        put(
            "entries",
            buildJsonArray {
                backup.entries.sortedBy { it.date }.forEach { entry ->
                    add(
                        buildJsonObject {
                            put("date", entry.date.toStorageString())
                            put("text", entry.text)
                        },
                    )
                }
            },
        )
        put("settings", settingsJson(backup))
    }
    return BackupJson.encodeToString(JsonObject.serializer(), root)
}

private fun settingsJson(backup: BackupData): JsonObject = buildJsonObject {
    put("theme", backup.settings.theme.storageKey)
    put("ghost", backup.settings.ghostText)
    put("bodyweight", backup.settings.bodyweightKg)
    put("goals", buildJsonArray { backup.goals.forEach { add(goalJson(it)) } })
}

private fun goalJson(goal: Goal): JsonObject = buildJsonObject {
    put("type", goal.type.storageKey)
    put("ex", goal.exerciseKey)
    put("per", goal.period?.storageKey)
    put("target", goal.targetKg)
}
