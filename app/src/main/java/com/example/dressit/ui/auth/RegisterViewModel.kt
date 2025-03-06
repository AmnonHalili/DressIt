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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    init {
        // בדוק אם המשתמש כבר מחובר
        if (authManager.isLoggedIn()) {
            _registerSuccess.value = true
        }
    }

    fun register(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }

        if (password.length < 6) {
            _error.value = "Password must be at least 6 characters long"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                Log.d("RegisterViewModel", "Starting registration for email: $email")
                
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                Log.d("RegisterViewModel", "Registration successful")
                
                // Create user in Firestore
                result.user?.let { firebaseUser ->
                    val user = User(
                        id = firebaseUser.uid,
                        username = username,
                        email = email
                    )
                    userRepository.createUser(user)
                    
                    // שמירת מצב ההתחברות
                    authManager.saveUserSession(firebaseUser.uid)
                }
                
                _registerSuccess.value = true
            } catch (e: FirebaseAuthException) {
                Log.e("RegisterViewModel", "FirebaseAuthException: ${e.message}, ErrorCode: ${e.errorCode}")
                _error.value = when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email address"
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already registered"
                    else -> "Registration failed: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Unexpected error during registration", e)
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