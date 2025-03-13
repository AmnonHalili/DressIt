package com.example.dressit.data.repository

import android.util.Log
import com.example.dressit.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    fun getAllPosts(): Flow<List<Post>> = flow {
        try {
            Log.d("FirebaseRepository", "Getting all posts from Firestore")
            val snapshot = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            Log.d("FirebaseRepository", "Got ${snapshot.documents.size} documents from Firestore")
            
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.let { post ->
                    // Ensure the image URL is not empty
                    if (!post.imageUrl.isNullOrEmpty()) {
                        post.copy(id = doc.id)
                    } else {
                        Log.w("FirebaseRepository", "Post ${doc.id} has empty imageUrl, skipping")
                        null
                    }
                }
            }
            Log.d("FirebaseRepository", "Mapped ${posts.size} valid posts from Firestore")
            emit(posts)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting all posts", e)
            throw e
        }
    }

    suspend fun getPost(postId: String): Post? {
        return try {
            Log.d("FirebaseRepository", "Getting post with ID: $postId")
            val doc = postsCollection.document(postId).get().await()
            val post = doc.toObject(Post::class.java)?.copy(id = doc.id)
            if (post != null) {
                Log.d("FirebaseRepository", "Successfully retrieved post with ID: $postId")
            } else {
                Log.w("FirebaseRepository", "Post with ID: $postId not found")
            }
            post
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting post with ID: $postId", e)
            throw e
        }
    }

    suspend fun createPost(post: Post): String {
        return try {
            Log.d("FirebaseRepository", "Creating new post")
            val docRef = postsCollection.add(post).await()
            Log.d("FirebaseRepository", "Successfully created post with ID: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error creating post", e)
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