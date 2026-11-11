package com.example.liftjournal.domain.parser

import java.math.BigDecimal
import java.math.RoundingMode

/** A weight as written: either plain kilograms or a multiple of bodyweight. */
sealed interface WrittenWeight {
    data class Kilograms(val kg: Double) : WrittenWeight

    data class Bodyweight(val multiplier: Double) : WrittenWeight
}

private const val MULTIPLIER_DECIMALS = 4
private const val DEFAULT_MULTIPLIER = 1.0
private val WHITESPACE = Regex("\\s+")
private val BODYWEIGHT_SUFFIX = Regex("^(.*)bw$", RegexOption.IGNORE_CASE)

/**
 * Reads a weight token such as `80`, `72.5`, `bw`, `0.5 bw`, `1/2bw` (prototype `num`).
 * A fraction with a zero denominator counts as plain bodyweight, like the prototype.
 */
fun parseWrittenWeight(token: String): WrittenWeight {
    val compact = token.replace(WHITESPACE, "")
    val match = BODYWEIGHT_SUFFIX.find(compact) ?: return WrittenWeight.Kilograms(compact.toDouble())
    val multiplierText = match.groupValues[1]
    val multiplier = if (multiplierText.isEmpty()) DEFAULT_MULTIPLIER else parseMultiplier(multiplierText)
    return WrittenWeight.Bodyweight(roundMultiplier(multiplier))
}

private fun parseMultiplier(text: String): Double {
    if (!text.contains('/')) return text.toDouble()
    val (numerator, denominator) = text.split('/').map { it.toDouble() }
    return if (denominator == 0.0) DEFAULT_MULTIPLIER else numerator / denominator
}

private fun roundMultiplier(value: Double): Double =
    BigDecimal.valueOf(value).setScale(MULTIPLIER_DECIMALS, RoundingMode.HALF_UP).toDouble()

/** Builds a set from a written weight (prototype `mkSet`). */
fun WrittenWeight.toSet(reps: Int): ExerciseSet = when (this) {
    is WrittenWeight.Kilograms -> ExerciseSet(weightKg = kg, reps = reps)
    is WrittenWeight.Bodyweight -> ExerciseSet(weightKg = 0.0, reps = reps, bodyweightMultiplier = multiplier)
}
