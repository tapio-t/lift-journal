package com.example.liftjournal.domain.stats

import com.example.liftjournal.domain.parser.ParsedExercise
import com.example.liftjournal.domain.parser.effectiveWeightKg
import com.example.liftjournal.domain.parser.volumeKg

/** An exercise's totals over a period: volume, heaviest set and average set weight. */
data class ExerciseTotals(
    val key: String,
    val name: String,
    val volumeKg: Double,
    val heaviestSetKg: Double,
    val weightSumKg: Double,
    val weightedSetCount: Int,
) {
    /** Mean of all counted set weights (SPEC §5.3 "Average set weight"). */
    val averageSetWeightKg: Double get() = if (weightedSetCount == 0) 0.0 else weightSumKg / weightedSetCount
}

/** Collects [ExerciseTotals] while stats are being built; local to one calculation. */
internal class ExerciseTotalsCollector(private val bodyweightKg: Double?) {
    private val byKey = LinkedHashMap<String, ExerciseTotals>()

    fun add(exercise: ParsedExercise) {
        val weights = exercise.countedSetWeights(bodyweightKg)
        val previous = byKey[exercise.key] ?: ExerciseTotals(exercise.key, exercise.name, 0.0, 0.0, 0.0, 0)
        byKey[exercise.key] = previous.copy(
            volumeKg = previous.volumeKg + exercise.volumeKg(bodyweightKg),
            heaviestSetKg = maxOf(previous.heaviestSetKg, weights.maxOrNull() ?: 0.0),
            weightSumKg = previous.weightSumKg + weights.sum(),
            weightedSetCount = previous.weightedSetCount + weights.size,
        )
    }

    /** Exercises with at least one weighted set, largest volume first (SPEC §5.1 exercise chips). */
    fun weightedByVolume(): List<ExerciseTotals> =
        byKey.values.filter { it.heaviestSetKg > 0 }.sortedByDescending { it.volumeKg }

}

/** Weights of the sets that count, skipping uncounted bodyweight sets and zero weights. */
internal fun ParsedExercise.countedSetWeights(bodyweightKg: Double?): List<Double> =
    sets.mapNotNull { set -> set.effectiveWeightKg(bodyweightKg)?.takeIf { it > 0 } }
