package com.example.dressit.ui.post

import android.location.Location
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.GeoPoint
import com.example.dressit.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    private var imageUri: Uri? = null
    private var currentLocation: Location? = null

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postCreated = MutableLiveData<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

    fun setImage(uri: Uri) {
        imageUri = uri
    }

    fun setLocation(location: Location) {
        currentLocation = location
    }

    fun createPost(title: String, description: String) {
        if (imageUri == null) {
            _error.value = "Please select an image"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                val post = postRepository.createPost(title, description, imageUri!!)
                
                // Update location if available
                currentLocation?.let { location ->
                    val updatedPost = post.copy(
                        location = GeoPoint(location.latitude, location.longitude)
                    )
                    postRepository.updatePost(updatedPost)
                }

                _postCreated.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
} 