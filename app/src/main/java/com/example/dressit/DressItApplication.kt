package com.example.dressit

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.dressit.data.local.AppDatabase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DressItApplication : Application() {
    companion object {
        private const val PREFS_NAME = "DressItPrefs"
        private const val KEY_DB_VERSION = "db_version"
        private const val CURRENT_DB_VERSION = 2
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // בדיקה אם צריך לנקות את בסיס הנתונים
        handleDatabaseMigration()
    }
    
    private fun handleDatabaseMigration() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedVersion = prefs.getInt(KEY_DB_VERSION, 0)
        
        // אם הגרסה השמורה שונה מהגרסה הנוכחית, ננקה את בסיס הנתונים
        if (savedVersion != CURRENT_DB_VERSION) {
            // ניקוי בסיס הנתונים
            AppDatabase.clearDatabase(this)
            
            // שמירת הגרסה הנוכחית
            prefs.edit().putInt(KEY_DB_VERSION, CURRENT_DB_VERSION).apply()
        }
    }
} 