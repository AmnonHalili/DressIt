package com.example.dressit.data.repository

import android.net.Uri
import com.example.dressit.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null
        return getUserById(currentUser.uid)
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateProfile(username: String, bio: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val updates = hashMapOf<String, Any>(
            "username" to username,
            "bio" to bio
        )
        firestore.collection("users").document(currentUser.uid).update(updates).await()
    }

    suspend fun updateProfileImage(imageUri: Uri) {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val ref = storage.reference.child("profile_images/${UUID.randomUUID()}")
        ref.putFile(imageUri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        
        firestore.collection("users").document(currentUser.uid)
            .update("profilePicture", downloadUrl)
            .await()
    }

    suspend fun updateUser(user: User) {
        firestore.collection("users")
            .document(user.id)
            .set(user)
            .await()
    }

    suspend fun createUser(user: User) {
        firestore.collection("users")
            .document(user.id)
            .set(user)
            .await()
    }

    suspend fun deleteUser(userId: String) {
        firestore.collection("users")
            .document(userId)
            .delete()
            .await()
    }
} 