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
    
    private val _commentAdded = MutableLiveData<Boolean>()
    val commentAdded: LiveData<Boolean> = _commentAdded
    
    private val _commentDeleted = MutableLiveData<Boolean>()
    val commentDeleted: LiveData<Boolean> = _commentDeleted

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
    
    // פונקציה לבדיקה אם המשתמש הנוכחי לחץ לייק על הפוסט
    fun isPostLikedByCurrentUser(post: Post): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        return post.likedBy.contains(currentUser.uid)
    }
    
    // פונקציה לבדיקה אם המשתמש הנוכחי שמר את הפוסט
    fun isPostSavedByCurrentUser(post: Post): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        return post.savedBy.contains(currentUser.uid)
    }
    
    // פונקציה להוספת/הסרת לייק
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val updatedPost = postRepository.likePost(postId)
                _post.value = updatedPost
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
    
    // פונקציה להוספת תגובה
    fun addComment(postId: String, commentText: String) {
        if (commentText.isBlank()) {
            _error.value = "התגובה לא יכולה להיות ריקה"
            return
        }
        
        viewModelScope.launch {
            try {
                _loading.value = true
                val updatedPost = postRepository.addComment(postId, commentText)
                _post.value = updatedPost
                _commentAdded.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
                _commentAdded.value = false
            }
        }
    }
    
    // פונקציה למחיקת תגובה
    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val updatedPost = postRepository.deleteComment(postId, commentId)
                _post.value = updatedPost
                _commentDeleted.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
                _commentDeleted.value = false
            }
        }
    }
    
    // פונקציה להוספת/הסרת שמירה
    fun toggleSave(postId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val updatedPost = postRepository.savePost(postId)
                _post.value = updatedPost
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
} 