package com.example.dressit.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    protected fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                block()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    protected fun handleError(e: Exception) {
        _error.value = e.message
    }

    protected fun clearError() {
        _error.value = null
    }
} 