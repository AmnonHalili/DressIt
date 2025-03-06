package com.example.dressit.ui.post

import android.location.Location
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postUpdated = MutableLiveData<Boolean>()
    val postUpdated: LiveData<Boolean> = _postUpdated

    private val _imageSelected = MutableLiveData<Boolean>()
    val imageSelected: LiveData<Boolean> = _imageSelected

    private var selectedImageUri: Uri? = null
    private var currentLocation: Location? = null
    private var postId: String = ""

    fun loadPost(postId: String) {
        this.postId = postId
        _loading.value = true
        viewModelScope.launch {
            try {
                val post = postRepository.getPostById(postId)
                _post.value = post
                _loading.value = false
            } catch (e: Exception) {
                _error.value = "שגיאה בטעינת הפוסט: ${e.message}"
                _loading.value = false
            }
        }
    }

    fun updatePost(title: String, description: String, price: Double) {
        if (title.length < 3) {
            _error.value = "כותרת חייבת להיות לפחות 3 תווים"
            return
        }

        if (description.length < 10) {
            _error.value = "תיאור חייב להיות לפחות 10 תווים"
            return
        }

        if (price < 0) {
            _error.value = "מחיר לא יכול להיות שלילי"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val updatedPost = postRepository.updatePost(
                    postId = postId,
                    title = title,
                    description = description,
                    imageUri = selectedImageUri,
                    rentalPrice = price,
                    latitude = currentLocation?.latitude,
                    longitude = currentLocation?.longitude
                )
                _post.value = updatedPost
                _postUpdated.value = true
                _loading.value = false
            } catch (e: Exception) {
                _error.value = "שגיאה בעדכון הפוסט: ${e.message}"
                _loading.value = false
            }
        }
    }

    fun setImage(uri: Uri) {
        selectedImageUri = uri
        _imageSelected.value = true
    }

    fun setLocation(location: Location) {
        currentLocation = location
    }

    fun clearError() {
        _error.value = null
    }
} 