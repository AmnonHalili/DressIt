package com.example.dressit.ui.post

import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.repository.PostRepository
import com.example.dressit.data.repository.UserRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private var imageUri: Uri? = null
    var currentLocation: Location? = null
        private set

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postCreated = MutableLiveData<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

    private val _imageSelected = MutableLiveData<Boolean>()
    val imageSelected: LiveData<Boolean> = _imageSelected

    fun setImage(uri: Uri) {
        imageUri = uri
        _imageSelected.value = true
    }

    fun setLocation(location: Location) {
        currentLocation = location
    }

    fun createPost(title: String, description: String, price: Double = 0.0) {
        if (title.length < 3) {
            _error.value = "הכותרת חייבת להיות לפחות 3 תווים"
            return
        }

        if (description.length < 10) {
            _error.value = "התיאור חייב להיות לפחות 10 תווים"
            return
        }

        if (imageUri == null) {
            _error.value = "אנא בחר תמונה"
            return
        }
        
        if (price < 0) {
            _error.value = "המחיר לא יכול להיות שלילי"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                Log.d("AddPostViewModel", "Starting post creation...")
                
                // בדיקת מצב התחברות
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                Log.d("AddPostViewModel", "Firebase Auth User: ${firebaseUser?.uid ?: "null"}")
                
                // Get current user
                val currentUser = userRepository.getCurrentUser()
                if (currentUser == null) {
                    Log.e("AddPostViewModel", "User is null. Firebase Auth User: ${firebaseUser?.uid ?: "null"}")
                    _error.value = "נראה שחל נתק בחיבור שלך. אנא התחבר מחדש"
                    return@launch
                }
                Log.d("AddPostViewModel", "Current user found: ${currentUser.username} (${currentUser.id})")

                // Extract location information if available
                val latitude = currentLocation?.latitude
                val longitude = currentLocation?.longitude
                
                if (latitude != null && longitude != null) {
                    Log.d("AddPostViewModel", "Adding post with location data: Lat=$latitude, Long=$longitude")
                } else {
                    Log.d("AddPostViewModel", "No location data available for this post")
                }
                
                // Create post with all data in a single call
                val post = postRepository.createPost(
                    title = title, 
                    description = description, 
                    imageUri = imageUri!!, 
                    rentalPrice = price,
                    latitude = latitude,
                    longitude = longitude
                )
                
                Log.d("AddPostViewModel", "Post created successfully with ID: ${post.id}")
                if (post.latitude != null && post.longitude != null) {
                    Log.d("AddPostViewModel", "Post saved with location: Lat=${post.latitude}, Long=${post.longitude}")
                }
                
                // רענון פוסטים מהשרת אחרי שמירה
                try {
                    postRepository.refreshAllPostsFromServer()
                    Log.d("AddPostViewModel", "Successfully refreshed posts from server after creating post")
                } catch (e: Exception) {
                    Log.e("AddPostViewModel", "Error refreshing posts after creation: ${e.message}", e)
                }
                
                _postCreated.value = true
                Log.d("AddPostViewModel", "Post creation completed successfully")
            } catch (e: FirebaseNetworkException) {
                Log.e("AddPostViewModel", "Network error: ${e.message}", e)
                _error.value = "אנא בדוק את חיבור האינטרנט שלך ונסה שוב"
            } catch (e: FirebaseFirestoreException) {
                Log.e("AddPostViewModel", "Firestore error: ${e.message}, code: ${e.code}", e)
                when (e.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE -> {
                        Log.e("AddPostViewModel", "Service unavailable error", e)
                        _error.value = "השירות אינו זמין כרגע. אנא נסה שוב מאוחר יותר"
                    }
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        Log.e("AddPostViewModel", "Permission denied error", e)
                        _error.value = "אין לך הרשאות מתאימות לביצוע פעולה זו"
                    }
                    else -> {
                        Log.e("AddPostViewModel", "Other Firestore error: ${e.message}", e)
                        _error.value = e.message ?: "Failed to create post"
                    }
                }
            } catch (e: Exception) {
                Log.e("AddPostViewModel", "Unexpected error: ${e.message}", e)
                _error.value = e.message ?: "Failed to create post"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 