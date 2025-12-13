package com.example.pikephish_v2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LinkHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun linkHistoryDao(): LinkHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pikephish_database"
                )
                    .fallbackToDestructiveMigration()  // При изменении схемы пересоздаёт БД
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
