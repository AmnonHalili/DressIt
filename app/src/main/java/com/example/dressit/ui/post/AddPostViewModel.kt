package com.example.dressit.ui.post

import android.app.Application
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.GeoPoint
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.PostRepository
import com.example.dressit.data.repository.UserRepository
import com.example.dressit.data.repository.WeatherRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class AddPostViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository = PostRepository(application)
    private val userRepository = UserRepository()
    private val weatherRepository = WeatherRepository()

    private var imageUri: Uri? = null
    private var imageFile: File? = null
    private var currentLocation: Location? = null

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postCreated = MutableLiveData<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

    fun setImage(uri: Uri, file: File) {
        imageUri = uri
        imageFile = file
    }

    fun setLocation(location: Location) {
        currentLocation = location
    }

    fun createPost(title: String, description: String) {
        if (imageUri == null || imageFile == null) {
            _error.value = "Please select an image"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    _error.value = "User not logged in"
                    return@launch
                }

                val user = userRepository.getUserById(currentUser.uid)
                if (user == null) {
                    _error.value = "User data not found"
                    return@launch
                }

                // Upload image
                val imageUrl = postRepository.uploadImage(imageFile!!)

                val post = Post(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser.uid,
                    userName = user.username,
                    title = title,
                    description = description,
                    imageUrl = imageUrl,
                    location = currentLocation?.let { 
                        GeoPoint(it.latitude, it.longitude)
                    },
                    timestamp = System.currentTimeMillis(),
                    likes = 0,
                    comments = emptyList()
                )

                postRepository.createPost(post)
                _postCreated.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
} 