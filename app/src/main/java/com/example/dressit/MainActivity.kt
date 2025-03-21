package com.example.dressit

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.dressit.databinding.ActivityMainBinding
import com.example.dressit.data.repository.AuthManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    @Inject
    lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("MainActivity", "Firebase initialized")
        
        // בדיקה אם יש צורך לאפס את בסיס הנתונים
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val needsReset = prefs.getBoolean("needs_db_reset", false)
        if (needsReset) {
            Log.d("MainActivity", "Database reset flag found, clearing database")
            com.example.dressit.data.local.AppDatabase.clearDatabase(this)
            prefs.edit().putBoolean("needs_db_reset", false).apply()
            Log.d("MainActivity", "Database reset completed")
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "Content view set")

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        Log.d("MainActivity", "NavController initialized")

        // Setup bottom navigation with NavController
        binding.bottomNav.setupWithNavController(navController)
        Log.d("MainActivity", "Bottom navigation setup complete")

        // הגדרת לחיצה על כפתור המפה
        binding.mapButton.setOnClickListener {
            try {
                Log.d("MainActivity", "Map button clicked")
                navController.navigate(R.id.mapFragment)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error navigating to map fragment", e)
                Snackbar.make(
                    binding.root,
                    "שגיאה בניווט למפה",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        // בדיקת מצב התחברות והפניה למסך המתאים
        checkAuthState(navController)

        // Hide bottom navigation on auth screens and adjust map button visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("MainActivity", "Navigation destination changed to: ${destination.label}")
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    Log.d("MainActivity", "Hiding bottom navigation for auth screen")
                    binding.bottomNav.visibility = View.GONE
                    binding.mapButton.visibility = View.GONE
                }
                R.id.profileFragment -> {
                    // הסתר את כפתור המפה במסך הפרופיל כדי שלא יסתיר אלמנטים אחרים
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.mapButton.visibility = View.GONE
                }
                else -> {
                    Log.d("MainActivity", "Showing bottom navigation")
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.mapButton.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun checkAuthState(navController: androidx.navigation.NavController) {
        // בדיקה אם המשתמש מחובר
        val currentUser = authManager.getCurrentUser()
        if (!authManager.isLoggedIn()) {
            Log.d("MainActivity", "User is not logged in, navigating to login screen")
            // אם המשתמש לא מחובר, נווט למסך ההתחברות
            navController.navigate(R.id.loginFragment)
        } else {
            Log.d("MainActivity", "User is logged in as: ${currentUser?.email}, UID: ${currentUser?.uid}")
            // אם המשתמש מחובר, השאר אותו במסך הנוכחי (בדרך כלל מסך הבית)
        }
    }
    
    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart called")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause called")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                Log.d("MainActivity", "Logout menu item selected")
                authManager.logout()
                navigateToLogin()
                true
            }
            R.id.action_reset_database -> {
                Log.d("MainActivity", "Reset database menu item selected")
                showResetDatabaseDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showResetDatabaseDialog() {
        Log.d("MainActivity", "Showing reset database dialog")
        AlertDialog.Builder(this)
            .setTitle("איפוס בסיס נתונים")
            .setMessage("האם אתה בטוח שברצונך לאפס את בסיס הנתונים המקומי? פעולה זו תמחק את כל הנתונים המקומיים ותסגור את האפליקציה.")
            .setPositiveButton("איפוס") { _, _ ->
                Log.d("MainActivity", "User confirmed database reset")
                
                // הגדרת דגל לאיפוס בסיס הנתונים בהפעלה הבאה
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("needs_db_reset", true).apply()
                
                Snackbar.make(
                    binding.root,
                    "בסיס הנתונים יאופס בהפעלה הבאה של האפליקציה",
                    Snackbar.LENGTH_LONG
                ).show()
                
                // סגירת האפליקציה לאחר 2 שניות
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("MainActivity", "Closing application after database reset")
                    finish()
                }, 2000)
            }
            .setNegativeButton("ביטול") { dialog, _ ->
                Log.d("MainActivity", "User cancelled database reset")
                dialog.dismiss()
            }
            .show()
    }

    private fun navigateToLogin() {
        Log.d("MainActivity", "Navigating to login screen")
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.loginFragment)
    }
} 