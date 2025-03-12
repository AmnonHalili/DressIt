package com.example.dressit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dressit.data.model.Converters
import com.example.dressit.data.model.Notification
import com.example.dressit.data.model.Post
import com.example.dressit.data.model.Booking
import android.util.Log

@Database(entities = [Post::class, Notification::class, Booking::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun notificationDao(): NotificationDao
    abstract fun bookingDao(): BookingDao

    companion object {
        private const val DATABASE_NAME = "app_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Migrating database from version 1 to 2")
                // Migration logic from version 1 to 2
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Migrating database from version 2 to 3")
                // No schema changes needed, just version bump
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Migrating database from version 3 to 4")
                // Create notifications table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        postId TEXT NOT NULL,
                        actorId TEXT NOT NULL,
                        actorName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        thumbnailUrl TEXT NOT NULL DEFAULT ''
                    )
                    """
                )
            }
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Migrating database from version 4 to 5")
                // Create bookings table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookings (
                        id TEXT PRIMARY KEY NOT NULL,
                        renterId TEXT NOT NULL,
                        renterName TEXT NOT NULL,
                        ownerId TEXT NOT NULL,
                        ownerName TEXT NOT NULL,
                        postId TEXT NOT NULL,
                        postTitle TEXT NOT NULL,
                        postImage TEXT NOT NULL,
                        dressPrice REAL NOT NULL,
                        currency TEXT NOT NULL,
                        pickupLocation TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """
                )
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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