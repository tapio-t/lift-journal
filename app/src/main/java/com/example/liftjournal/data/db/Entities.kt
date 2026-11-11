package com.example.liftjournal.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A journal entry row. The date (`yyyy-MM-dd`) is the identity: one entry per date (SPEC §7). */
@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val date: String,
    val text: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** A goal row. The auto-generated id also gives the display order. */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "exercise_key") val exerciseKey: String?,
    val period: String?,
    @ColumnInfo(name = "target_kg") val targetKg: Double,
)
