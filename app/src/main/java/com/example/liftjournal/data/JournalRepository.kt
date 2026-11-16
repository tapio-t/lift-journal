package com.example.liftjournal.data

import androidx.room.withTransaction
import com.example.liftjournal.data.db.AppDatabase
import com.example.liftjournal.data.db.EntryEntity
import com.example.liftjournal.data.db.GoalEntity
import com.example.liftjournal.domain.goals.Goal
import com.example.liftjournal.domain.goals.GoalPeriod
import com.example.liftjournal.domain.goals.GoalType
import com.example.liftjournal.domain.importer.ImportConflictMode
import com.example.liftjournal.domain.importer.ImportPlan
import com.example.liftjournal.domain.importer.planImport
import com.example.liftjournal.domain.journal.appendEntryText
import com.example.liftjournal.domain.model.JournalEntry
import com.example.liftjournal.domain.model.StoredEntry
import com.example.liftjournal.domain.time.parseStorageDate
import com.example.liftjournal.domain.time.toStorageString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Entries and goals. Every committed change is reported to [changeNotifier] for real-time backup. */
class JournalRepository(
    private val database: AppDatabase,
    private val changeNotifier: DataChangeNotifier,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val entryDao = database.entryDao()
    private val goalDao = database.goalDao()
    private val parsedCache = ParsedEntryCache()

    /** All entries, newest first, with parsed sets. */
    val entries: Flow<List<JournalEntry>> = entryDao.observeAll().map { rows ->
        parsedCache.parseAll(rows.mapNotNull { row -> row.toStored()?.let { it.date to it.text } })
    }

    /** All goals in the order they were set. */
    val goals: Flow<List<Goal>> = goalDao.observeAll().map { rows -> rows.mapNotNull { it.toGoal() } }

    /** Saves an entry's text. Blank text deletes the entry, since an entry is never empty. */
    suspend fun saveEntry(date: LocalDate, text: String) {
        val key = date.toStorageString()
        val existing = entryDao.get(key)
        if (text.isBlank()) {
            if (existing == null) return
            entryDao.delete(key)
        } else {
            if (existing?.text == text) return
            entryDao.upsert(EntryEntity(key, text, currentTimeMillis()))
        }
        changeNotifier.notifyChanged()
    }

    /** Deletes an entry and returns what was deleted, for Undo. */
    suspend fun deleteEntry(date: LocalDate): StoredEntry? {
        val existing = entryDao.get(date.toStorageString())?.toStored() ?: return null
        entryDao.delete(date.toStorageString())
        changeNotifier.notifyChanged()
        return existing
    }

    /** Undoes a delete. If the date got a new entry meanwhile, the restored text goes above it. */
    suspend fun restoreEntry(entry: StoredEntry) {
        database.withTransaction {
            val current = entryDao.get(entry.date.toStorageString())?.text.orEmpty()
            entryDao.upsert(
                EntryEntity(entry.date.toStorageString(), appendEntryText(entry.text, current), currentTimeMillis()),
            )
        }
        changeNotifier.notifyChanged()
    }

    suspend fun hasEntry(date: LocalDate): Boolean = entryDao.get(date.toStorageString()) != null

    /** Moves an entry to another date; if that date has an entry, the moved text is appended below it (SPEC §3.3). */
    suspend fun moveEntry(from: LocalDate, to: LocalDate) {
        if (from == to) return
        database.withTransaction {
            val moving = entryDao.get(from.toStorageString()) ?: return@withTransaction
            val target = entryDao.get(to.toStorageString())?.text.orEmpty()
            entryDao.upsert(
                EntryEntity(to.toStorageString(), appendEntryText(target, moving.text), currentTimeMillis()),
            )
            entryDao.delete(from.toStorageString())
        }
        changeNotifier.notifyChanged()
    }

    /** Adds a goal and returns its id. */
    suspend fun addGoal(goal: Goal): Long {
        val id = goalDao.insert(goal.toEntity().copy(id = 0))
        changeNotifier.notifyChanged()
        return id
    }

    suspend fun removeGoal(id: Long) {
        goalDao.delete(id)
        changeNotifier.notifyChanged()
    }

    /** Undoes a goal removal. The goal keeps its id, so it returns to its place in the list. */
    suspend fun restoreGoal(goal: Goal) {
        goalDao.insert(goal.toEntity())
        changeNotifier.notifyChanged()
    }

    /**
     * Replaces every entry and goal in one transaction (restore from backup, erase all data), so a failure
     * leaves the old data untouched. Goals get new ids in the given order.
     */
    suspend fun replaceAll(entries: List<StoredEntry>, goals: List<Goal>) {
        database.withTransaction {
            entryDao.deleteAll()
            goalDao.deleteAll()
            val now = currentTimeMillis()
            entryDao.upsertAll(entries.map { EntryEntity(it.date.toStorageString(), it.text, now) })
            goals.forEach { goalDao.insert(it.toEntity().copy(id = 0)) }
        }
        changeNotifier.notifyChanged()
    }

    /**
     * Imports entries in one transaction (PLAN phase 7). The plan is worked out inside the transaction against
     * the stored entries, so it can't overwrite a change made since the preview. Nothing is deleted.
     */
    suspend fun importEntries(imported: List<StoredEntry>, mode: ImportConflictMode): ImportPlan {
        val plan = database.withTransaction {
            val existing = entryDao.getAll().mapNotNull { it.toStored() }.associate { it.date to it.text }
            val plan = planImport(imported, existing, mode)
            val now = currentTimeMillis()
            entryDao.upsertAll(plan.entriesToWrite.map { EntryEntity(it.date.toStorageString(), it.text, now) })
            plan
        }
        if (plan.entriesToWrite.isNotEmpty()) changeNotifier.notifyChanged()
        return plan
    }

    /** Entries and goals as stored, for backups and export. */
    suspend fun snapshot(): Pair<List<StoredEntry>, List<Goal>> = database.withTransaction {
        entryDao.getAll().mapNotNull { it.toStored() }.sortedBy { it.date } to
            goalDao.getAll().mapNotNull { it.toGoal() }
    }

    private fun EntryEntity.toStored(): StoredEntry? = parseStorageDate(date)?.let { StoredEntry(it, text) }
}

private fun GoalEntity.toGoal(): Goal? {
    val goalType = GoalType.fromStorageKey(type) ?: return null
    return Goal(id, goalType, exerciseKey, GoalPeriod.fromStorageKey(period), targetKg)
}

private fun Goal.toEntity() = GoalEntity(id, type.storageKey, exerciseKey, period?.storageKey, targetKg)
