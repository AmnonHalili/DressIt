package com.example.dressit.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.FirebaseRepository
import com.example.dressit.data.repository.PostRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.switchMap
import kotlinx.coroutines.flow.first

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    private val firebaseRepository = FirebaseRepository()

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts
    
    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        loadPosts()
    }

    private fun loadPosts() {
        loadAllPosts()
    }
    
    // פונקציה לטעינת כל הפוסטים
    private fun loadAllPosts() {
        _loading.value = true
        _error.value = null
        
        Log.d("HomeViewModel", "Loading all posts from local database")
        
        // קודם נטען מבסיס הנתונים המקומי
        postRepository.getAllPosts()
            .onEach { posts ->
                Log.d("HomeViewModel", "Received ${posts.size} posts from local database")
                _posts.postValue(posts)
                _loading.postValue(false)
            }
            .catch { exception ->
                Log.e("HomeViewModel", "Error loading posts from local database", exception)
                _error.postValue(exception.message ?: "Unknown error occurred")
                _loading.postValue(false)
            }
            .launchIn(viewModelScope)
            
        // ואז ננסה לרענן מהשרת
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Trying to refresh posts from server")
                firebaseRepository.getAllPosts()
                    .collect { remotePosts ->
                        Log.d("HomeViewModel", "Received ${remotePosts.size} posts from server")
                        if (remotePosts.isNotEmpty()) {
                            // שמירת הפוסטים בבסיס הנתונים המקומי
                            postRepository.insertPosts(remotePosts)
                            Log.d("HomeViewModel", "Inserted ${remotePosts.size} posts to local database")
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing posts from server", e)
                _error.postValue(e.message ?: "Failed to refresh posts from server")
            }
        }
    }
    
    fun refreshPosts() {
        _loading.value = true
        _error.value = null
        
        Log.d("HomeViewModel", "Refreshing posts")
        
        viewModelScope.launch {
            try {
                firebaseRepository.getAllPosts()
                    .collect { remotePosts ->
                        Log.d("HomeViewModel", "Refresh received ${remotePosts.size} posts from server")
                        if (remotePosts.isNotEmpty()) {
                            postRepository.insertPosts(remotePosts)
                            Log.d("HomeViewModel", "Refresh inserted ${remotePosts.size} posts to local database")
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error during refresh posts", e)
                _error.postValue(e.message ?: "Failed to refresh posts")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun forceRefreshAndClearLocalData() {
        _loading.value = true
        _error.value = null
        
        Log.d("HomeViewModel", "Force refreshing and clearing local data")
        
        viewModelScope.launch {
            try {
                // מחיקת כל הפוסטים המקומיים
                postRepository.clearLocalDatabase()
                Log.d("HomeViewModel", "Deleted all local posts")
                
                // טעינה מחדש מהשרת
                firebaseRepository.getAllPosts()
                    .collect { remotePosts ->
                        Log.d("HomeViewModel", "Force refresh received ${remotePosts.size} posts from server")
                        if (remotePosts.isNotEmpty()) {
                            postRepository.insertPosts(remotePosts)
                            Log.d("HomeViewModel", "Force refresh inserted ${remotePosts.size} posts to local database")
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error during force refresh", e)
                _error.postValue(e.message ?: "Failed to refresh posts")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun likePost(postId: String) {
        Log.d("HomeViewModel", "Liking post with ID: $postId")
        viewModelScope.launch {
            try {
                val updatedPost = postRepository.likePost(postId)
                Log.d("HomeViewModel", "Successfully liked/unliked post with ID: $postId")
                
                // עדכן את המצב המקומי של הפוסטים כדי לשקף את השינוי מיד
                _posts.value = _posts.value?.map { post ->
                    if (post.id == postId) updatedPost else post
                }
                
                // רענן את הפוסטים מהשרת
                refreshPosts()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error liking post with ID: $postId", e)
                _error.postValue(e.message ?: "Failed to like post")
            }
        }
    }
    
    fun savePost(postId: String) {
        Log.d("HomeViewModel", "Saving post with ID: $postId")
        viewModelScope.launch {
            try {
                val updatedPost = postRepository.savePost(postId)
                Log.d("HomeViewModel", "Successfully saved/unsaved post with ID: $postId")
                
                // עדכן את המצב המקומי של הפוסטים כדי לשקף את השינוי מיד
                _posts.value = _posts.value?.map { post ->
                    if (post.id == postId) updatedPost else post
                }
                
                // רענן את הפוסטים מהשרת
                refreshPosts()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saving post with ID: $postId", e)
                _error.postValue(e.message ?: "Failed to save post")
            }
        }
    }

    fun commentOnPost(postId: String, commentText: String) {
        if (commentText.isBlank()) {
            _error.value = "התגובה לא יכולה להיות ריקה"
            return
        }
        
        viewModelScope.launch {
            try {
                postRepository.addComment(postId, commentText)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to comment on post")
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                
                // קריאה לפונקציה הייעודית במקום updatePost
                postRepository.likePost(post.id)
                
                // רענון הפוסטים כדי לקבל את השינויים
                refreshPosts()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error toggling like", e)
                _error.value = e.message
            }
        }
    }
    
    fun toggleSave(post: Post) {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                
                // קריאה לפונקציה הייעודית במקום updatePost
                postRepository.savePost(post.id)
                
                // רענון הפוסטים כדי לקבל את השינויים
                refreshPosts()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error toggling save", e)
                _error.value = e.message
            }
        }
    }
    
    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun clearLocalDatabase() {
        Log.d("HomeViewModel", "Clearing local database")
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                postRepository.clearLocalDatabase()
                Log.d("HomeViewModel", "Local database cleared successfully")
                _posts.value = emptyList()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error clearing local database", e)
                _error.value = e.message ?: "Error clearing local database"
            } finally {
                _loading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
} 