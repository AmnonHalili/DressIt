package com.example.dressit.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.User
import com.example.dressit.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _profileUpdated = MutableLiveData<Boolean>()
    val profileUpdated: LiveData<Boolean> = _profileUpdated

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val user = userRepository.getUserById(currentUser.uid)
                    _user.value = user
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun setProfileImage(uri: Uri) {
        _selectedImageUri.value = uri
    }

    fun updateProfile(username: String) {
        if (username.isBlank()) {
            _error.value = "Please enter a name"
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

                // Upload new profile image if selected
                var profilePictureUrl = _user.value?.profilePicture ?: ""
                _selectedImageUri.value?.let { uri ->
                    val ref = storage.reference.child("profile_images/${UUID.randomUUID()}")
                    ref.putFile(uri).await()
                    profilePictureUrl = ref.downloadUrl.await().toString()
                }

                // Update user profile
                val updatedUser = User(
                    id = currentUser.uid,
                    username = username,
                    email = currentUser.email ?: "",
                    profilePicture = profilePictureUrl
                )

                userRepository.updateUser(updatedUser)
                _profileUpdated.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
} 