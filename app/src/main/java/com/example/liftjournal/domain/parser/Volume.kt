package com.example.liftjournal.domain.parser

/**
 * The weight a set counts with (prototype `eff`). Bodyweight sets use multiplier × bodyweight;
 * without a bodyweight setting they return null and are left out of every figure (SPEC §4).
 */
fun ExerciseSet.effectiveWeightKg(bodyweightKg: Double?): Double? {
    val multiplier = bodyweightMultiplier ?: return weightKg
    val bodyweight = bodyweightKg?.takeIf { it > 0 } ?: return null
    return multiplier * bodyweight
}

/** Weight × reps for one set, or 0 when the set doesn't count. */
fun ExerciseSet.volumeKg(bodyweightKg: Double?): Double = (effectiveWeightKg(bodyweightKg) ?: 0.0) * reps

/** Total volume of an exercise (prototype `exVol`). */
fun ParsedExercise.volumeKg(bodyweightKg: Double?): Double = sets.sumOf { it.volumeKg(bodyweightKg) }

/** Number of sets that can't count because no bodyweight is set. */
fun ParsedExercise.uncountedBodyweightSets(bodyweightKg: Double?): Int =
    sets.count { it.effectiveWeightKg(bodyweightKg) == null }
