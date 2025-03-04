package com.example.dressit.data.repository

import com.example.dressit.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    fun getAllPosts(): Flow<List<Post>> = flow {
        try {
            val snapshot = postsCollection.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            emit(posts)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getPost(postId: String): Post? {
        return try {
            val doc = postsCollection.document(postId).get().await()
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createPost(post: Post): String {
        return try {
            val docRef = postsCollection.add(post).await()
            docRef.id
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updatePost(post: Post) {
        try {
            postsCollection.document(post.id).set(post).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deletePost(postId: String) {
        try {
            postsCollection.document(postId).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }
} 