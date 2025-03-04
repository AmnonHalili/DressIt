package com.example.dressit.data.repository

import com.example.dressit.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getUserById(userId: String): User? {
        return try {
            db.collection("users")
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(user: User) {
        db.collection("users")
            .document(user.id)
            .set(user)
            .await()
    }

    suspend fun createUser(user: User) {
        db.collection("users")
            .document(user.id)
            .set(user)
            .await()
    }

    suspend fun deleteUser(userId: String) {
        db.collection("users")
            .document(userId)
            .delete()
            .await()
    }
} 