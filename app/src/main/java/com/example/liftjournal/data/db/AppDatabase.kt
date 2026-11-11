package com.example.liftjournal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The on-device database. Schema changes must bump [version], export the schema and come with a
 * tested migration: user data must never be lost on app update (CLAUDE.md).
 */
@Database(entities = [EntryEntity::class, GoalEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    abstract fun goalDao(): GoalDao

    companion object {
        const val NAME = "lift-journal.db"

        /** Opens the app database. There is deliberately no destructive-migration fallback. */
        fun open(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
