package com.example.dressit.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                auth.signInWithEmailAndPassword(email, password).await()
                _loginSuccess.value = true
            } catch (e: FirebaseAuthException) {
                _error.value = when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email address"
                    "ERROR_WRONG_PASSWORD" -> "Wrong password"
                    "ERROR_USER_NOT_FOUND" -> "No user found with this email"
                    "ERROR_USER_DISABLED" -> "This account has been disabled"
                    else -> "Authentication failed: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 