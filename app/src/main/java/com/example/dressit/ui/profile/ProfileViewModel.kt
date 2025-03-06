package com.example.dressit.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.model.User
import com.example.dressit.data.repository.AuthManager
import com.example.dressit.data.repository.PostRepository
import com.example.dressit.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import javax.inject.Inject

data class ProfileStats(
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _stats = MutableLiveData(ProfileStats())
    val stats: LiveData<ProfileStats> = _stats

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loggedOut = MutableLiveData<Boolean>()
    val loggedOut: LiveData<Boolean> = _loggedOut

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val user = userRepository.getCurrentUser()
                _user.value = user

                // טעינת הפוסטים של המשתמש הנוכחי
                auth.currentUser?.let { currentUser ->
                    loadUserPosts(currentUser.uid)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    private fun loadUserPosts(userId: String) {
        _loading.value = true
        
        postRepository.getUserPosts(userId)
            .onEach { posts ->
                _posts.value = posts
                updateStats(posts.size)
                _loading.value = false
            }
            .catch { e ->
                _error.value = e.message
                _loading.value = false
            }
            .launchIn(viewModelScope)
    }

    private fun updateStats(postsCount: Int) {
        viewModelScope.launch {
            try {
                // קבלת המשתמש הנוכחי
                val user = _user.value
                
                if (user != null) {
                    _stats.value = ProfileStats(
                        postsCount = postsCount,
                        followersCount = user.followers.size,
                        followingCount = user.following.size
                    )
                } else {
                    _stats.value = ProfileStats(
                        postsCount = postsCount,
                        followersCount = 0,
                        followingCount = 0
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating stats", e)
            }
        }
    }

    fun refreshPosts() {
        auth.currentUser?.let { currentUser ->
            loadUserPosts(currentUser.uid)
        }
    }

    fun forceRefreshUserPosts() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                auth.currentUser?.let { currentUser ->
                    val userId = currentUser.uid
                    
                    // ניסיון לרענן את הפוסטים של המשתמש מהשרת
                    Log.d("ProfileViewModel", "Force refreshing posts for user: $userId")
                    
                    // שימוש בפונקציה החדשה לרענון פוסטים של משתמש
                    postRepository.refreshUserPosts(userId)
                }
                
                _loading.value = false
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during force refresh", e)
                _error.value = e.message ?: "Failed to refresh posts"
                _loading.value = false
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            try {
                // TODO: Implement like functionality
                val updatedPost = post.copy(likes = post.likes + 1)
                postRepository.updatePost(updatedPost)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun logout() {
        // התנתקות מ-Firebase
        auth.signOut()
        // מחיקת הסשן השמור
        authManager.clearSession()
        _loggedOut.value = true
    }

    // פונקציה לעקיבה אחרי משתמש
    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.followUser(userId)
                
                if (result) {
                    // רענון נתוני המשתמש
                    loadUserProfile()
                } else {
                    _error.value = "Failed to follow user"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // פונקציה להפסקת עקיבה אחרי משתמש
    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.unfollowUser(userId)
                
                if (result) {
                    // רענון נתוני המשתמש
                    loadUserProfile()
                } else {
                    _error.value = "Failed to unfollow user"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // פונקציה לבדיקה האם המשתמש הנוכחי עוקב אחרי משתמש מסוים
    suspend fun isFollowing(userId: String): Boolean {
        return userRepository.isFollowing(userId)
    }
} 