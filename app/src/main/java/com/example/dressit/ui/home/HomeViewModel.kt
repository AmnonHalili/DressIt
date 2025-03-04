package com.example.dressit.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.getAllPosts()
                    .onEach { posts ->
                        _posts.value = posts
                    }
                    .catch { exception ->
                        _error.value = exception.message ?: "Unknown error occurred"
                    }
                    .launchIn(this)
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshPosts() {
        loadPosts()
    }

    fun likePost() {
        viewModelScope.launch {
            try {
                // TODO: Implement like functionality
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to like post"
            }
        }
    }

    fun commentOnPost() {
        viewModelScope.launch {
            try {
                // TODO: Implement comment functionality
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to comment on post"
            }
        }
    }
} 