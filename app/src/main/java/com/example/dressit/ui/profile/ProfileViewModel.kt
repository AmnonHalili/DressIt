package com.example.dressit.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.model.User
import com.example.dressit.data.repository.PostRepository
import com.example.dressit.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ProfileStats(
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _posts = MutableLiveData<List<Post>>()
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
        loadUserPosts()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val user = userRepository.getCurrentUser()
                _user.value = user

                user?.let { loadUserPosts(it.id) }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    private fun loadUserPosts() {
        auth.currentUser?.let { currentUser ->
            postRepository.getUserPosts(currentUser.uid)
                .onEach { posts ->
                    _posts.value = posts
                }
                .catch { e ->
                    _error.value = e.message
                }
                .launchIn(viewModelScope)
        }
    }

    private fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            try {
                postRepository.getUserPosts(userId)
                    .collectLatest { posts: List<Post> ->
                        _posts.value = posts
                        updateStats(posts.size)
                    }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun updateStats(postsCount: Int) {
        _stats.value = ProfileStats(
            postsCount = postsCount,
            followersCount = 1234, // Placeholder values
            followingCount = 567   // Placeholder values
        )
    }

    fun refreshPosts() {
        loadUserPosts()
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
        auth.signOut()
        _loggedOut.value = true
    }
} 