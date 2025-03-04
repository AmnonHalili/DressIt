package com.example.dressit.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postDeleted = MutableLiveData<Boolean>()
    val postDeleted: LiveData<Boolean> = _postDeleted

    fun loadPost(postId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val post = postRepository.getPostById(postId)
                _post.value = post
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _post.value?.let { post ->
                    postRepository.deletePost(post)
                    _postDeleted.value = true
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun isCurrentUserPostOwner(post: Post): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid == post.userId
    }
} 