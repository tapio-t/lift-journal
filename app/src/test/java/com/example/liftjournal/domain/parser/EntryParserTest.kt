package com.example.liftjournal.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Rules beyond the golden file. */
class EntryParserTest {
    @Test
    fun `name in parentheses-only line names the next exercise`() {
        val parsed = EntryParser.parse("squat (warm-up)\n100 x 5")
        assertEquals(listOf(ParsedExercise("Squat", listOf(ExerciseSet(100.0, 5)))), parsed.exercises)
    }

    @Test
    fun `unclosed parenthesis hides the rest of the line`() {
        val parsed = EntryParser.parse("squat 100 x 5 (110 x 3, 120 x 1")
        assertEquals(listOf(ExerciseSet(100.0, 5)), parsed.exercises.single().sets)
    }

    @Test
    fun `note line ends the current exercise`() {
        val parsed = EntryParser.parse("squat 100 x 5\nfelt good\n5")
        assertEquals(listOf("5"), parsed.errors)
    }

    @Test
    fun `no-break spaces are read like spaces`() {
        val parsed = EntryParser.parse("squat 100 x 5")
        assertEquals("Squat", parsed.exercises.single().name)
    }

    @Test
    fun `semicolons separate sets like commas`() {
        val parsed = EntryParser.parse("squat 100 x 5; 5")
        assertEquals(2, parsed.exercises.single().sets.size)
    }

    @Test
    fun `colon or dash between name and weight is allowed`() {
        assertEquals("Squat", EntryParser.parse("squat: 100 x 5").exercises.single().name)
        assertEquals("Squat", EntryParser.parse("squat - 100 x 5").exercises.single().name)
    }

    @Test
    fun `bodyweight set has no weight without a bodyweight setting`() {
        val set = EntryParser.parse("pull-up bw x 8").exercises.single().sets.single()
        assertNull(set.effectiveWeightKg(null))
        assertEquals(80.0, set.effectiveWeightKg(80.0)!!, 0.0)
    }

    @Test
    fun `fraction with zero denominator counts as full bodyweight`() {
        val set = EntryParser.parse("dips 1/0 bw x 5").exercises.single().sets.single()
        assertEquals(1.0, set.bodyweightMultiplier!!, 0.0)
    }

    @Test
    fun `huge rep counts are reported as errors instead of crashing`() {
        val parsed = EntryParser.parse("squat 100 x 99999999999")
        assertEquals(listOf("squat 100 x 99999999999"), parsed.errors)
    }
}
