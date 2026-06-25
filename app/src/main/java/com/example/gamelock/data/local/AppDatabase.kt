package com.example.gamelock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameEntity::class],         version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gamelock_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
