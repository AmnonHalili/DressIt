package com.example.dressit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dressit.data.model.Converters
import com.example.dressit.data.model.Post
import android.util.Log

@Database(entities = [Post::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    companion object {
        private const val DATABASE_NAME = "app_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Migrating database from version 1 to 2")
                // Migration logic from version 1 to 2
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Migrating database from version 2 to 3")
                // No schema changes needed, just version bump
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun clearDatabase(context: Context) {
            Log.d("AppDatabase", "Clearing database")
            try {
                context.applicationContext.deleteDatabase(DATABASE_NAME)
                Log.d("AppDatabase", "Database cleared successfully")
                INSTANCE = null
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error clearing database", e)
            }
        }
    }
} 