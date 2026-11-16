package com.example.liftjournal.domain.backup

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

// Backups are read field by field so a wrong type in one place skips that value instead of the whole file.

internal fun JsonObject.stringField(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf {
    it.isString
}?.content

/** A number; a numeric string is accepted too, since the prototype stored form values as typed. */
internal fun JsonObject.numberField(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

internal fun JsonObject.booleanField(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull

internal fun JsonObject.arrayField(key: String): List<JsonObject> =
    (this[key] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()

internal fun JsonObject.objectField(key: String): JsonObject? = this[key] as? JsonObject
