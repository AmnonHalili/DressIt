package com.example.dressit.data.repository

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.local.AppDatabase
import com.example.dressit.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri

@Singleton
class PostRepository @Inject constructor(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postDao = AppDatabase.getDatabase(context).postDao()

    fun getAllPosts(): Flow<List<Post>> {
        return postDao.getAllPosts()
            .onEach { 
                // Refresh in background
                try {
                    val posts = firestore.collection("posts")
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(Post::class.java) }
                    postDao.insertPosts(posts)
                } catch (e: Exception) {
                    // Handle error
                }
            }
    }

    fun getUserPosts(userId: String): Flow<List<Post>> {
        return postDao.getUserPosts(userId)
            .onEach {
                // Refresh in background
                try {
                    val posts = firestore.collection("posts")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(Post::class.java) }
                    postDao.insertPosts(posts)
                } catch (e: Exception) {
                    // Handle error
                }
            }
    }

    suspend fun getPostById(postId: String): Post? {
        // First try to get from local database
        val localPost = postDao.getPostById(postId)
        if (localPost != null) {
            return localPost
        }

        // If not found locally, fetch from Firebase
        return try {
            val document = firestore.collection("posts").document(postId).get().await()
            val post = document.toObject(Post::class.java)
            
            // Cache the post locally
            post?.let { postDao.insertPosts(listOf(it)) }
            post
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadImage(file: File): String {
        val storageRef = storage.reference
        val imageRef = storageRef.child("posts/${UUID.randomUUID()}.jpg")
        
        return try {
            imageRef.putFile(file.toUri()).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createPost(title: String, description: String, imageUri: Uri): Post {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        // Upload image
        val ref = storage.reference.child("post_images/${UUID.randomUUID()}")
        ref.putFile(imageUri).await()
        val imageUrl = ref.downloadUrl.await().toString()

        // Create post
        val post = Post(
            id = UUID.randomUUID().toString(),
            userId = currentUser.uid,
            userName = currentUser.displayName ?: "",
            title = title,
            description = description,
            imageUrl = imageUrl
        )

        // Save to Firestore
        firestore.collection("posts").document(post.id).set(post).await()
        return post
    }

    suspend fun updatePost(post: Post) {
        firestore.collection("posts")
            .document(post.id)
            .set(post)
            .await()
        postDao.updatePost(post)
    }

    suspend fun deletePost(post: Post) {
        // Delete image from storage if exists
        if (post.imageUrl.isNotEmpty()) {
            try {
                storage.getReferenceFromUrl(post.imageUrl).delete().await()
            } catch (e: Exception) {
                // Ignore if image doesn't exist
            }
        }

        // Delete post from Firestore
        firestore.collection("posts")
            .document(post.id)
            .delete()
            .await()
        postDao.deletePost(post)
    }

    private fun File.toUri() = android.net.Uri.fromFile(this)
} 