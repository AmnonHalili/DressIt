package com.example.dressit.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.User
import com.example.dressit.data.repository.AuthManager
import com.example.dressit.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    init {
        Log.d("LoginViewModel", "Current user: ${auth.currentUser?.email}")
        // בדוק אם המשתמש כבר מחובר
        if (authManager.isLoggedIn()) {
            _loginSuccess.value = true
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Please fill in all fields"
            Log.d("LoginViewModel", "Login failed: Empty fields")
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                Log.d("LoginViewModel", "Starting login for email: $email")
                
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Log.d("LoginViewModel", "Login result: ${result.user?.uid}")
                
                if (result.user != null) {
                    Log.d("LoginViewModel", "Login successful for user: ${result.user?.email}")
                    
                    // שמירת מצב ההתחברות
                    authManager.saveUserSession(result.user!!.uid)
                    
                    // בדוק אם המשתמש קיים ב-Firestore
                    val userDoc = firestore.collection("users").document(result.user!!.uid).get().await()
                    if (!userDoc.exists()) {
                        // אם המשתמש לא קיים, צור אותו
                        Log.d("LoginViewModel", "Creating new user document in Firestore")
                        val newUser = User(
                            id = result.user!!.uid,
                            email = result.user!!.email ?: "",
                            username = result.user!!.email?.substringBefore("@") ?: "",
                            bio = "",
                            profilePicture = result.user!!.photoUrl?.toString() ?: "",
                            createdAt = System.currentTimeMillis()
                        )
                        userRepository.createUser(newUser)
                        Log.d("LoginViewModel", "User document created successfully")
                    }
                    
                    _loginSuccess.value = true
                } else {
                    Log.e("LoginViewModel", "Login failed: User is null after successful authentication")
                    _error.value = "Failed to get user data after login"
                }
            } catch (e: FirebaseAuthException) {
                Log.e("LoginViewModel", "FirebaseAuthException during login", e)
                Log.e("LoginViewModel", "Error code: ${e.errorCode}")
                Log.e("LoginViewModel", "Error message: ${e.message}")
                
                _error.value = when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email address"
                    "ERROR_WRONG_PASSWORD" -> "Wrong password"
                    "ERROR_USER_NOT_FOUND" -> "No user found with this email"
                    "ERROR_USER_DISABLED" -> "This account has been disabled"
                    else -> "Authentication failed: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Unexpected error during login", e)
                Log.e("LoginViewModel", "Error class: ${e.javaClass.simpleName}")
                Log.e("LoginViewModel", "Error message: ${e.message}")
                Log.e("LoginViewModel", "Stack trace: ${e.stackTraceToString()}")
                
                _error.value = "An unexpected error occurred: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 