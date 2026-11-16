package com.example.liftjournal.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY date DESC")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY date DESC")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE date = :date")
    suspend fun get(date: String): EntryEntity?

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<EntryEntity>)

    @Query("DELETE FROM entries WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM entries")
    suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY id")
    suspend fun getAll(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: GoalEntity): Long

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
