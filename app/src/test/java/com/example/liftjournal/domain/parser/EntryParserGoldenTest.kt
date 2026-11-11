package com.example.liftjournal.domain.parser

import com.example.liftjournal.readTestResource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every case in testdata/parser-golden.json (generated from the prototype) must match exactly. */
class EntryParserGoldenTest {
    private val cases: List<JsonObject> = Json.parseToJsonElement(readTestResource("parser-golden.json"))
        .jsonObject.getValue("cases").jsonArray.map { it.jsonObject }

    @Test
    fun `golden file has cases`() {
        assertTrue(cases.size >= 20)
    }

    @Test
    fun `every golden case parses to the expected exercises, notes and errors`() {
        cases.forEach { case ->
            val name = case.string("case")
            val expected = case.getValue("expected").jsonObject
            val actual = EntryParser.parse(case.string("input"))

            assertEquals("$name: exercises", expected.exercises(), actual.exercises)
            assertEquals("$name: notes", expected.strings("notes"), actual.notes)
            assertEquals("$name: errors", expected.strings("errors"), actual.errors)
        }
    }

    @Test
    fun `every golden case has the expected volumes with and without bodyweight`() {
        cases.forEach { case ->
            val name = case.string("case")
            val expected = case.getValue("expected").jsonObject
            val actual = EntryParser.parse(case.string("input"))

            assertEquals("$name: no bodyweight", expected.volumes("volume_kg_no_bodyweight"), actual.volumes(null))
            assertEquals(
                "$name: bodyweight 80",
                expected.volumes("volume_kg_bodyweight_80"),
                actual.volumes(BODYWEIGHT),
            )
        }
    }

    private fun ParsedEntry.volumes(bodyweightKg: Double?) = exercises.map { it.name to it.volumeKg(bodyweightKg) }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }

    private fun JsonObject.volumes(key: String) = getValue(key).jsonArray.map {
        val item = it.jsonObject
        item.string("name") to item.getValue("volume").jsonPrimitive.double
    }

    private fun JsonObject.exercises() = getValue("exercises").jsonArray.map { exercise ->
        val item = exercise.jsonObject
        ParsedExercise(item.string("name"), item.getValue("sets").jsonArray.map { it.jsonObject.toSet() })
    }

    private fun JsonObject.toSet() = ExerciseSet(
        weightKg = getValue("w").jsonPrimitive.double,
        reps = getValue("r").jsonPrimitive.int,
        bodyweightMultiplier = get("bw")?.jsonPrimitive?.doubleOrNull,
    )

    private companion object {
        const val BODYWEIGHT = 80.0
    }
}
