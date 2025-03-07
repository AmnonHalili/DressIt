package com.example.dressit.ui.saved

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedPostsViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        Log.d("SavedPostsViewModel", "Initializing SavedPostsViewModel")
        loadSavedPosts()
    }

    private fun loadSavedPosts() {
        Log.d("SavedPostsViewModel", "Loading saved posts")
        _loading.value = true
        _error.value = null
        
        postRepository.getSavedPosts()
            .onEach { posts ->
                Log.d("SavedPostsViewModel", "Received ${posts.size} saved posts")
                _posts.postValue(posts)
                _loading.postValue(false)
            }
            .catch { exception ->
                Log.e("SavedPostsViewModel", "Error loading saved posts", exception)
                _error.postValue(exception.message ?: "Unknown error occurred")
                _loading.postValue(false)
            }
            .launchIn(viewModelScope)
    }

    fun refreshSavedPosts() {
        Log.d("SavedPostsViewModel", "Refreshing saved posts")
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                postRepository.refreshSavedPosts()
                Log.d("SavedPostsViewModel", "Saved posts refreshed successfully")
            } catch (e: Exception) {
                Log.e("SavedPostsViewModel", "Error refreshing saved posts", e)
                _error.value = e.message ?: "Error refreshing saved posts"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearLocalDatabase() {
        Log.d("SavedPostsViewModel", "Clearing local database")
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                postRepository.clearLocalDatabase()
                Log.d("SavedPostsViewModel", "Local database cleared successfully")
                _posts.value = emptyList()
            } catch (e: Exception) {
                Log.e("SavedPostsViewModel", "Error clearing local database", e)
                _error.value = e.message ?: "Error clearing local database"
            } finally {
                _loading.value = false
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val updatedPost = postRepository.likePost(postId)
                
                // עדכן את המצב המקומי של הפוסטים כדי לשקף את השינוי מיד
                _posts.value = _posts.value?.map { post ->
                    if (post.id == postId) updatedPost else post
                }
                
                // רענון הפוסטים מהשרת
                refreshSavedPosts()
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to like post")
            }
        }
    }
    
    fun savePost(postId: String) {
        _error.value = null
        viewModelScope.launch {
            try {
                val post = postRepository.getPostById(postId)
                if (post != null) {
                    postRepository.savePost(postId)
                    refreshSavedPosts()
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun unsavePost(postId: String) {
        _error.value = null
        viewModelScope.launch {
            try {
                val post = postRepository.getPostById(postId)
                if (post != null) {
                    postRepository.unsavePost(postId)
                    refreshSavedPosts()
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
} 