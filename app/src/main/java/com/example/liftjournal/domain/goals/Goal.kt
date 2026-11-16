package com.example.liftjournal.domain.goals

/** Goal types (SPEC §6, §7). [storageKey] is the value used in the database and backups. */
enum class GoalType(val storageKey: String) {
    TOP_SET("top"),
    AVERAGE_SET("avg"),
    TOTAL_VOLUME("vol"),
    EXERCISE_VOLUME("exvol"),
    ;

    /** Every type except total volume is about one exercise. */
    val needsExercise: Boolean get() = this != TOTAL_VOLUME

    /** Volume goals count per week or per month. */
    val needsPeriod: Boolean get() = this == TOTAL_VOLUME || this == EXERCISE_VOLUME

    companion object {
        fun fromStorageKey(key: String?): GoalType? = entries.firstOrNull { it.storageKey == key }
    }
}

enum class GoalPeriod(val storageKey: String) {
    WEEK("week"),
    MONTH("month"),
    ;

    companion object {
        fun fromStorageKey(key: String?): GoalPeriod? = entries.firstOrNull { it.storageKey == key }
    }
}

/** A goal. [exerciseKey] is a normalised exercise name, null for total volume; [period] is null for set goals. */
data class Goal(
    val id: Long,
    val type: GoalType,
    val exerciseKey: String?,
    val period: GoalPeriod?,
    val targetKg: Double,
)
