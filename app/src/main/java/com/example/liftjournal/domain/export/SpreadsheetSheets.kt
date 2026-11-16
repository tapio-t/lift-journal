package com.example.liftjournal.domain.export

import com.example.liftjournal.domain.format.shortName
import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.parser.ExerciseSet
import com.example.liftjournal.domain.parser.ParsedExercise
import com.example.liftjournal.domain.parser.effectiveWeightKg
import com.example.liftjournal.domain.parser.volumeKg
import com.example.liftjournal.domain.time.isoWeek
import com.example.liftjournal.domain.time.isoWeekYear
import com.example.liftjournal.domain.time.mondayOfWeek
import com.example.liftjournal.domain.time.toStorageString
import com.example.liftjournal.domain.time.yearMonth
import java.time.LocalDate

/** One exercise's sets, reps, volume and heaviest set over a day, week or month. */
private data class ExerciseSummary(
    val name: String,
    val sets: Int,
    val reps: Int,
    val volumeKg: Double,
    val topKg: Double,
) {
    operator fun plus(other: ExerciseSummary) = copy(
        sets = sets + other.sets,
        reps = reps + other.reps,
        volumeKg = volumeKg + other.volumeKg,
        topKg = maxOf(topKg, other.topKg),
    )

    /** A top set of 0 (only uncounted bodyweight sets) is left empty, like the prototype. */
    val topCell: Double? get() = topKg.takeIf { it > 0 }
}

/** An exercise's summary within one week or month, keyed so the same exercise groups together. */
private data class PeriodSummary<P>(val period: P, val summary: ExerciseSummary)

/**
 * The six export sheets (SPEC §6.1, prototype `sheetsData`). Bodyweight sets without a bodyweight setting have
 * empty weight and volume cells and count as 0 kg in the totals.
 */
fun buildExportSheets(entries: List<JournalEntry>, bodyweightKg: Double?): List<Sheet> {
    val oldestFirst = entries.sortedBy { it.date }
    return listOf(
        setsSheet(oldestFirst, bodyweightKg),
        dailyVolumeSheet(oldestFirst, bodyweightKg),
        weeklyVolumeSheet(oldestFirst, bodyweightKg),
        monthlyVolumeSheet(oldestFirst, bodyweightKg),
        monthlyTotalSheet(oldestFirst, bodyweightKg),
        journalTextSheet(oldestFirst, bodyweightKg),
    )
}

private fun setsSheet(entries: List<JournalEntry>, bodyweightKg: Double?): Sheet {
    val header = row(
        "Date", "Weekday", "ISO year", "ISO week", "Exercise", "Set", "Weight (kg)", "Reps", "Volume (kg)",
        "Bodyweight ×",
    )
    val rows = entries.flatMap { entry ->
        entry.exercises.flatMap { exercise ->
            exercise.sets.mapIndexed { index, set -> setRow(entry.date, exercise.name, index + 1, set, bodyweightKg) }
        }
    }
    return Sheet("Sets", listOf(header) + rows)
}

private fun setRow(
    date: LocalDate,
    exercise: String,
    number: Int,
    set: ExerciseSet,
    bodyweightKg: Double?,
): List<Cell> {
    val weight = set.effectiveWeightKg(bodyweightKg)
    return row(
        date.toStorageString(), weekday(date), date.isoWeekYear, date.isoWeek, exercise, number,
        weight, set.reps, weight?.times(set.reps), set.bodyweightMultiplier,
    )
}

private fun dailyVolumeSheet(entries: List<JournalEntry>, bodyweightKg: Double?): Sheet {
    val header = row("Date", "Weekday", "ISO week", "Exercise", "Sets", "Reps", "Volume (kg)", "Top set (kg)")
    val rows = entries.flatMap { entry ->
        entry.exercises.map { exercise ->
            val summary = exercise.summary(bodyweightKg)
            row(
                entry.date.toStorageString(),
                weekday(entry.date),
                entry.date.isoWeek,
                summary.name,
                summary.sets,
                summary.reps,
                summary.volumeKg,
                summary.topCell,
            )
        }
    }
    return Sheet("Daily volume", listOf(header) + rows)
}

private fun weeklyVolumeSheet(entries: List<JournalEntry>, bodyweightKg: Double?): Sheet {
    val header =
        row("ISO year", "ISO week", "Week starts (Mon)", "Exercise", "Sets", "Reps", "Volume (kg)", "Top set (kg)")
    val rows = summariesPerPeriod(entries, bodyweightKg) { it.mondayOfWeek() }.map { (monday, summary) ->
        row(
            monday.isoWeekYear,
            monday.isoWeek,
            monday.toStorageString(),
            summary.name,
            summary.sets,
            summary.reps,
            summary.volumeKg,
            summary.topCell,
        )
    }
    return Sheet("Weekly volume", listOf(header) + rows)
}

// Months are written as `yyyy-MM` (YearMonth.toString()), the prototype's month key.
private fun monthlyVolumeSheet(entries: List<JournalEntry>, bodyweightKg: Double?): Sheet {
    val header = row("Month", "Exercise", "Sets", "Reps", "Volume (kg)", "Top set (kg)")
    val rows = summariesPerPeriod(entries, bodyweightKg) { it.yearMonth }.map { (month, summary) ->
        row(month.toString(), summary.name, summary.sets, summary.reps, summary.volumeKg, summary.topCell)
    }
    return Sheet("Monthly volume", listOf(header) + rows)
}

/** Every month with an entry, even one without sets, like the prototype. */
private fun monthlyTotalSheet(entries: List<JournalEntry>, bodyweightKg: Double?): Sheet {
    val header = row("Month", "Workouts", "Sets", "Total volume (kg)")
    val rows = entries.groupBy { it.date.yearMonth }.toSortedMap().map { (month, monthEntries) ->
        val exercises = monthEntries.flatMap { it.exercises }
        row(
            month.toString(),
            monthEntries.count { it.isWorkout },
            exercises.sumOf { it.sets.size },
            exercises.sumOf { it.volumeKg(bodyweightKg) },
        )
    }
    return Sheet("Monthly total", listOf(header) + rows)
}

private fun journalTextSheet(entries: List<JournalEntry>, bodyweightKg: Double?): Sheet {
    val header = row("Date", "Weekday", "Day volume (kg)", "Entry text")
    val rows = entries.map { entry ->
        val dayVolume = entry.exercises.sumOf { it.volumeKg(bodyweightKg) }
        row(entry.date.toStorageString(), weekday(entry.date), dayVolume, entry.text)
    }
    return Sheet("Journal text", listOf(header) + rows)
}

/**
 * Adds up each exercise per period (week or month), named with its first spelling, sorted by period and then
 * exercise name.
 */
private fun <P : Comparable<P>> summariesPerPeriod(
    entries: List<JournalEntry>,
    bodyweightKg: Double?,
    periodOf: (LocalDate) -> P,
): List<Pair<P, ExerciseSummary>> {
    val totals = LinkedHashMap<Pair<P, String>, PeriodSummary<P>>()
    entries.forEach { entry ->
        val period = periodOf(entry.date)
        entry.exercises.forEach { exercise ->
            val key = period to exercise.key
            val summary = exercise.summary(bodyweightKg)
            totals[key] = totals[key]?.let { it.copy(summary = it.summary + summary) } ?: PeriodSummary(period, summary)
        }
    }
    return totals.values
        .sortedWith(compareBy<PeriodSummary<P>> { it.period }.thenBy { it.summary.name.lowercase() })
        .map { it.period to it.summary }
}

private fun ParsedExercise.summary(bodyweightKg: Double?) = ExerciseSummary(
    name = name,
    sets = sets.size,
    reps = sets.sumOf { it.reps },
    volumeKg = volumeKg(bodyweightKg),
    topKg = sets.maxOfOrNull { it.effectiveWeightKg(bodyweightKg) ?: 0.0 }?.coerceAtLeast(0.0) ?: 0.0,
)

private fun weekday(date: LocalDate): String = date.dayOfWeek.shortName()
