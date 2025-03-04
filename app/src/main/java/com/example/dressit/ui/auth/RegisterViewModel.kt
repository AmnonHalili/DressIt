package com.example.dressit.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
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
                auth.createUserWithEmailAndPassword(email, password).await()
                _registerSuccess.value = true
            } catch (e: FirebaseAuthException) {
                _error.value = when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email address"
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already registered"
                    else -> "Registration failed: ${e.message}"
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