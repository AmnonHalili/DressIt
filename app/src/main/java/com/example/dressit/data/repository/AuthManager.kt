package com.example.dressit.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    init {
        Log.d("AuthManager", "Initialized, current user: ${auth.currentUser?.uid}")
    }
    
    /**
     * בדיקה אם המשתמש מחובר
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = auth.currentUser != null
        Log.d("AuthManager", "isLoggedIn check: $isLoggedIn")
        return isLoggedIn
    }
    
    /**
     * קבלת המשתמש הנוכחי
     */
    fun getCurrentUser(): FirebaseUser? {
        val user = auth.currentUser
        Log.d("AuthManager", "getCurrentUser: ${user?.uid ?: "null"}")
        return user
    }
    
    /**
     * התנתקות מהמערכת
     */
    fun logout() {
        Log.d("AuthManager", "Logging out user: ${auth.currentUser?.uid}")
        auth.signOut()
        clearSession()
        Log.d("AuthManager", "User logged out, session cleared")
    }
    
    /**
     * שמירת מזהה המשתמש בהעדפות המקומיות
     */
    fun saveUserSession(userId: String) {
        Log.d("AuthManager", "Saving user session for userId: $userId")
        prefs.edit().apply {
            putString("user_id", userId)
            putBoolean("is_logged_in", true)
            apply()
        }
        Log.d("AuthManager", "User session saved")
    }
    
    /**
     * בדיקה אם קיים סשן שמור
     */
    fun hasSavedSession(): Boolean {
        val hasSession = prefs.getBoolean("is_logged_in", false)
        val userId = prefs.getString("user_id", null)
        Log.d("AuthManager", "hasSavedSession: $hasSession, userId: $userId")
        return hasSession
    }
    
    /**
     * מחיקת הסשן השמור
     */
    fun clearSession() {
        Log.d("AuthManager", "Clearing user session")
        prefs.edit().apply {
            remove("user_id")
            putBoolean("is_logged_in", false)
            apply()
        }
        Log.d("AuthManager", "User session cleared")
    }
} 