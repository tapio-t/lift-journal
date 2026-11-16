package com.example.liftjournal.domain.progress

import com.example.liftjournal.domain.format.formatCompact
import com.example.liftjournal.domain.format.formatWholeKg
import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.parser.EntryParser
import com.example.liftjournal.readTestResource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * testdata/progress-golden.json was produced by running the prototype headless on its own two-year random
 * training data (plus some bodyweight sets): every Progress chart with its bar values and header stats.
 * The text of titles and readouts is checked on a device by ProgressTextGoldenTest.
 */
class ProgressGoldenTest {
    private val golden = Json.parseToJsonElement(readTestResource("progress-golden.json")).jsonObject
    private val today = LocalDate.parse(golden.string("today"))
    private val bodyweightKg = golden.getValue("bodyweight").jsonPrimitive.double
    private val entries = golden.getValue("entries").jsonArray.map { element ->
        val entry = element.jsonObject
        val text = entry.string("text")
        JournalEntry(LocalDate.parse(entry.string("date")), text, EntryParser.parse(text))
    }
    private val cases = golden.getValue("cases").jsonArray.map { it.jsonObject }

    @Test
    fun `golden file covers years, all-time and every view`() {
        assertTrue(entries.size > 100)
        assertTrue(cases.map { it.string("period") }.toSet().size >= 3)
        assertEquals(setOf("month", "top", "exmonth"), cases.map { it.string("view") }.toSet())
    }

    @Test
    fun `every chart has the prototype's bar values`() {
        cases.forEach { case ->
            val chart = periodStats(case.period(), entries, today, bodyweightKg).chart(case.view(), case.exercise())
            val expected = case.getValue("values").jsonArray.map { it.jsonPrimitive.double }
            assertEquals("${case.name()}: bar count", expected.size, chart.values.size)
            expected.zip(chart.values).forEachIndexed { index, (want, got) ->
                assertEquals("${case.name()}: bar $index", want, got, VALUE_TOLERANCE)
            }
        }
    }

    @Test
    fun `header stats match the prototype for every period`() {
        cases.distinctBy { it.string("period") }.forEach { case ->
            val header = periodStats(case.period(), entries, today, bodyweightKg).header
            val expected = case.getValue("stats").jsonArray.map { it.jsonArray[0].jsonPrimitive.content }
            val third = when (val stat = header.third) {
                is ThirdStat.MonthSoFar -> stat.kg
                is ThirdStat.AveragePerMonth -> stat.kg
            }
            val actual =
                listOf(formatWholeKg(header.workouts.toDouble()), formatCompact(header.totalKg), formatCompact(third))
            assertEquals("${case.string("period")}: header", expected, actual)
        }
    }

    @Test
    fun `exercise chips list the prototype's exercises`() {
        cases.filter { it.exercise() != null }.groupBy { it.string("period") }.forEach { (period, periodCases) ->
            val stats = periodStats(periodCases.first().period(), entries, today, bodyweightKg)
            val expected = periodCases.filter { it.string("view") == "top" }.map { it.exercise() }
            assertEquals("$period: exercises", expected, stats.exercises.map { it.key })
        }
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.period(): ProgressPeriod =
        string("period").toIntOrNull()?.let { ProgressPeriod.OneYear(it) } ?: ProgressPeriod.AllTime

    private fun JsonObject.view(): ProgressView = when (string("view")) {
        "top" -> ProgressView.TOP_SET
        "exmonth" -> ProgressView.EXERCISE_VOLUME
        else -> ProgressView.TOTAL_VOLUME
    }

    private fun JsonObject.exercise(): String? = getValue("exercise").jsonPrimitive.contentOrNull

    private fun JsonObject.name(): String = "${string("period")} ${string("view")} ${exercise().orEmpty()}"

    private companion object {
        const val VALUE_TOLERANCE = 1e-6
    }
}
