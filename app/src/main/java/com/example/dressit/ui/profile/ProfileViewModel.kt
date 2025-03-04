package com.example.dressit.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.model.User
import com.example.dressit.data.repository.PostRepository
import com.example.dressit.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository()
    private val postRepository = PostRepository(application)
    private val auth = FirebaseAuth.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _loggedOut = MutableLiveData<Boolean>()
    val loggedOut: LiveData<Boolean> = _loggedOut

    init {
        loadUserProfile()
        loadUserPosts()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val user = userRepository.getUserById(currentUser.uid)
                    user?.let {
                        _user.value = it
                    }
                }
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