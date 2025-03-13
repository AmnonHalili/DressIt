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
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri
import android.util.Log
import com.example.dressit.data.model.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.example.dressit.data.model.Notification
import com.google.firebase.firestore.FieldValue

@Singleton
class PostRepository @Inject constructor(
    private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postDao = AppDatabase.getDatabase(context).postDao()

    // פונקציה להכנסת פוסטים לבסיס הנתונים המקומי
    suspend fun insertPosts(posts: List<Post>) {
        postDao.insertPosts(posts)
    }

    // פונקציה לניקוי בסיס הנתונים המקומי
    suspend fun clearLocalDatabase() {
        withContext(Dispatchers.IO) {
            postDao.deleteAllPosts()
        }
        Log.d("PostRepository", "Local database cleared")
    }
    
    // פונקציה למחיקת פוסטים של משתמש ספציפי
    suspend fun deleteUserPosts(userId: String) {
        withContext(Dispatchers.IO) {
            postDao.deleteUserPosts(userId)
        }
        Log.d("PostRepository", "Deleted posts for user: $userId")
    }
    
    // פונקציה לרענון פוסטים של משתמש ספציפי
    suspend fun refreshUserPosts(userId: String) {
        try {
            Log.d("PostRepository", "Refreshing posts for user: $userId")
            
            // קבלת הפוסטים מהשרת
            val posts = firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(Post::class.java) }
            
            Log.d("PostRepository", "Found ${posts.size} posts for user: $userId")
            
            // מחיקת הפוסטים הישנים של המשתמש מבסיס הנתונים המקומי
            deleteUserPosts(userId)
            
            // שמירת הפוסטים החדשים בבסיס הנתונים המקומי
            if (posts.isNotEmpty()) {
                insertPosts(posts)
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error refreshing user posts", e)
            throw e
        }
    }

    // פונקציה לרענון כל הפוסטים מהשרת
    suspend fun refreshAllPostsFromServer() {
        try {
            Log.d("PostRepository", "Refreshing all posts from server")
            val posts = firestore.collection("posts")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Post::class.java) }
            
            Log.d("PostRepository", "Found ${posts.size} posts from server")
            if (posts.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    // ניקוי בסיס הנתונים המקומי לפני הכנסת הנתונים החדשים
                    postDao.deleteAllPosts()
                    // הכנסת הפוסטים החדשים
                    postDao.insertPosts(posts)
                }
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error refreshing posts from server", e)
            throw e
        }
    }

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
                    Log.e("PostRepository", "Error refreshing posts", e)
                }
            }
    }

    fun getUserPosts(userId: String): Flow<List<Post>> {
        Log.d("PostRepository", "Getting posts for user: $userId")
        
        // מיד מחזיר את הפוסטים מהמסד נתונים המקומי
        return postDao.getUserPosts(userId)
            .onEach {
                // מנסה לרענן מהשרת
                try {
                    Log.d("PostRepository", "Refreshing user posts from server for user: $userId")
                    val posts = firestore.collection("posts")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { doc -> 
                            doc.toObject(Post::class.java)?.also { post ->
                                Log.d("PostRepository", "Found post: ${post.id} for user: $userId")
                            }
                        }
                    
                    Log.d("PostRepository", "Found ${posts.size} posts for user: $userId")
                    if (posts.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            postDao.insertPosts(posts)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error refreshing user posts", e)
                }
            }
    }

    // פונקציה לקבלת פוסטים שהמשתמש שמר
    fun getSavedPosts(): Flow<List<Post>> {
        val currentUserId = auth.currentUser?.uid
        Log.d("PostRepository", "getSavedPosts called, currentUserId: $currentUserId")
        
        if (currentUserId == null) {
            Log.d("PostRepository", "No user logged in, returning empty list")
            return postDao.getAllPosts().map { emptyList() }
        }
        
        return postDao.getSavedPosts()
            .onEach { posts ->
                Log.d("PostRepository", "Got ${posts.size} posts from local database")
                
                // מנסה לרענן מהשרת
                try {
                    Log.d("PostRepository", "Trying to refresh saved posts from server for user: $currentUserId")
                    val remotePosts = firestore.collection("posts")
                        .whereArrayContains("savedBy", currentUserId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(Post::class.java) }
                    
                    Log.d("PostRepository", "Got ${remotePosts.size} saved posts from server")
                    
                    if (remotePosts.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            postDao.insertPosts(remotePosts)
                            Log.d("PostRepository", "Inserted ${remotePosts.size} saved posts to local database")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error refreshing saved posts", e)
                }
            }
            .map { posts -> 
                val filteredPosts = posts.filter { post -> post.savedBy.contains(currentUserId) }
                Log.d("PostRepository", "Filtered to ${filteredPosts.size} saved posts for user: $currentUserId")
                filteredPosts
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
        val imageRef = storageRef.child("images/posts/${UUID.randomUUID()}.jpg")
        
        return try {
            imageRef.putFile(file.toUri()).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createPost(title: String, description: String, imageUri: Uri, rentalPrice: Double = 0.0, latitude: Double? = null, longitude: Double? = null): Post {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        // נסיון לקבל שם משתמש מ-Firestore תחילה
        var userName = ""
        try {
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val firestoreUser = userDoc.toObject(com.example.dressit.data.model.User::class.java)
            userName = firestoreUser?.username ?: ""
            Log.d("PostRepository", "Got username from Firestore: '$userName'")
        } catch (e: Exception) {
            Log.e("PostRepository", "Error getting username from Firestore", e)
        }
        
        // אם לא מצאנו בפיירסטור, ננסה מ-Auth
        if (userName.isEmpty()) {
            userName = currentUser.displayName ?: ""
            Log.d("PostRepository", "Using displayName: '$userName'")
        }
        
        // עדיין אין? ננסה אימייל או מזהה
        if (userName.isEmpty()) {
            userName = currentUser.email?.substringBefore("@") ?: "משתמש אפליקציה"
            Log.d("PostRepository", "Using fallback username (email or default): '$userName'")
        }
        
        // Upload image with file extension and timestamp
        val timestamp = System.currentTimeMillis()
        val ref = storage.reference.child("images/posts/${timestamp}_${UUID.randomUUID()}.jpg")
        ref.putFile(imageUri).await()
        val imageUrl = ref.downloadUrl.await().toString()

        // Create post
        val post = Post(
            id = UUID.randomUUID().toString(),
            userId = currentUser.uid,
            userName = userName,
            title = title,
            description = description,
            imageUrl = imageUrl,
            timestamp = timestamp,
            rentalPrice = rentalPrice,
            currency = "ILS",  // ברירת מחדל: שקל
            latitude = latitude,
            longitude = longitude
        )

        // Save to Firestore
        firestore.collection("posts").document(post.id).set(post).await()
        
        // Save to local database
        postDao.insertPosts(listOf(post))
        
        return post
    }

    suspend fun updatePost(post: Post) {
        firestore.collection("posts")
            .document(post.id)
            .set(post)
            .await()
        postDao.updatePost(post)
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        description: String,
        imageUri: Uri? = null,
        rentalPrice: Double = 0.0,
        latitude: Double? = null,
        longitude: Double? = null
    ): Post {
        // Get the existing post
        val existingPost = getPostById(postId) ?: throw Exception("Post not found")
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        // Check if the current user is the owner of the post
        if (existingPost.userId != currentUser.uid) {
            throw Exception("You don't have permission to edit this post")
        }
        
        // Upload new image if provided
        var imageUrl = existingPost.imageUrl
        if (imageUri != null) {
            // Delete old image if exists
            if (existingPost.imageUrl.isNotEmpty()) {
                try {
                    storage.getReferenceFromUrl(existingPost.imageUrl).delete().await()
                } catch (e: Exception) {
                    // Ignore if image doesn't exist or can't be deleted
                }
            }
            
            // Upload new image
            val timestamp = System.currentTimeMillis()
            val ref = storage.reference.child("images/posts/${timestamp}_${UUID.randomUUID()}.jpg")
            ref.putFile(imageUri).await()
            imageUrl = ref.downloadUrl.await().toString()
        }
        
        // Create updated post
        val updatedPost = existingPost.copy(
            title = title,
            description = description,
            imageUrl = imageUrl,
            rentalPrice = rentalPrice,
            latitude = latitude ?: existingPost.latitude,
            longitude = longitude ?: existingPost.longitude,
            lastUpdated = System.currentTimeMillis()
        )
        
        // Save to Firestore
        firestore.collection("posts").document(updatedPost.id).set(updatedPost).await()
        
        // Save to local database
        postDao.updatePost(updatedPost)
        
        return updatedPost
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

    // פונקציה להוספת לייק לפוסט
    suspend fun likePost(postId: String): Post {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val post = getPostById(postId) ?: throw Exception("Post not found")
        
        Log.d("PostRepository", "Liking post: $postId by user: ${currentUser.uid}, post owner: ${post.userId}")
        
        // בדיקה אם המשתמש כבר לחץ לייק
        if (post.likedBy.contains(currentUser.uid)) {
            // הסרת הלייק
            Log.d("PostRepository", "User already liked this post, removing like")
            val updatedLikedBy = post.likedBy.filter { it != currentUser.uid }
            val updatedPost = post.copy(
                likes = post.likes - 1,
                likedBy = updatedLikedBy
            )
            
            // עדכון ב-Firestore
            firestore.collection("posts").document(postId).set(updatedPost).await()
            
            // עדכון בבסיס הנתונים המקומי
            postDao.updatePost(updatedPost)
            
            return updatedPost
        } else {
            // הוספת לייק
            Log.d("PostRepository", "Adding new like to post")
            val updatedLikedBy = post.likedBy + currentUser.uid
            val updatedPost = post.copy(
                likes = post.likes + 1,
                likedBy = updatedLikedBy
            )
            
            // עדכון ב-Firestore
            firestore.collection("posts").document(postId).set(updatedPost).await()
            
            // עדכון בבסיס הנתונים המקומי
            postDao.updatePost(updatedPost)
            
            // יצירת התראה על לייק - רק אם המשתמש שלחץ לייק אינו בעל הפוסט
            if (post.userId != currentUser.uid) {
                try {
                    Log.d("PostRepository", "Creating like notification for user: ${post.userId}")
                    
                    // קבלת שם המשתמש הנוכחי
                    var userName = currentUser.displayName ?: ""
                    
                    // אם אין שם משתמש ב-Auth, ננסה לקבל מ-Firestore
                    if (userName.isEmpty()) {
                        try {
                            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                            val firestoreUser = userDoc.toObject(com.example.dressit.data.model.User::class.java)
                            userName = firestoreUser?.username ?: ""
                        } catch (e: Exception) {
                            Log.e("PostRepository", "Error getting username from Firestore", e)
                        }
                    }
                    
                    // אם עדיין אין שם, נשתמש במשהו בסיסי
                    if (userName.isEmpty()) {
                        userName = "משתמש אפליקציה"
                    }
                    
                    withContext(Dispatchers.IO) {
                        notificationRepository.createLikeNotification(
                            post = updatedPost,
                            likedByUserId = currentUser.uid,
                            likedByUserName = userName
                        )
                    }
                    Log.d("PostRepository", "Like notification created successfully")
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error creating like notification", e)
                    // נמשיך גם אם יצירת ההתראה נכשלה
                }
            } else {
                Log.d("PostRepository", "No notification created - user liked their own post")
            }
            
            return updatedPost
        }
    }
    
    // פונקציה להוספת תגובה לפוסט
    suspend fun addComment(postId: String, commentText: String): Post {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val post = getPostById(postId) ?: throw Exception("Post not found")
        
        // קבלת שם המשתמש הנוכחי
        var userName = currentUser.displayName ?: ""
        
        // אם אין שם משתמש ב-Auth, ננסה לקבל מ-Firestore
        if (userName.isEmpty()) {
            try {
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val firestoreUser = userDoc.toObject(com.example.dressit.data.model.User::class.java)
                userName = firestoreUser?.username ?: ""
                Log.d("PostRepository", "Got username from Firestore: '$userName'")
            } catch (e: Exception) {
                Log.e("PostRepository", "Error getting username from Firestore", e)
            }
        }
        
        // אם עדיין אין שם, נשתמש באימייל או בשם ברירת מחדל
        if (userName.isEmpty()) {
            userName = currentUser.email?.substringBefore("@") ?: "משתמש אפליקציה"
            Log.d("PostRepository", "Using fallback username: '$userName'")
        }
        
        // יצירת תגובה חדשה
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            userId = currentUser.uid,
            userName = userName,
            text = commentText,
            timestamp = System.currentTimeMillis()
        )
        
        // הוספת התגובה לרשימת התגובות
        val updatedComments = post.comments + newComment
        val updatedPost = post.copy(comments = updatedComments)
        
        // עדכון ב-Firestore
        firestore.collection("posts").document(postId).set(updatedPost).await()
        
        // עדכון בבסיס הנתונים המקומי
        postDao.updatePost(updatedPost)
        
        return updatedPost
    }
    
    // פונקציה למחיקת תגובה מפוסט
    suspend fun deleteComment(postId: String, commentId: String): Post {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val post = getPostById(postId) ?: throw Exception("Post not found")
        
        // מציאת התגובה
        val comment = post.comments.find { it.id == commentId }
            ?: throw Exception("Comment not found")
        
        // בדיקה אם המשתמש הוא בעל התגובה או בעל הפוסט
        if (comment.userId != currentUser.uid && post.userId != currentUser.uid) {
            throw Exception("You don't have permission to delete this comment")
        }
        
        // הסרת התגובה מרשימת התגובות
        val updatedComments = post.comments.filter { it.id != commentId }
        val updatedPost = post.copy(comments = updatedComments)
        
        // עדכון ב-Firestore
        firestore.collection("posts").document(postId).set(updatedPost).await()
        
        // עדכון בבסיס הנתונים המקומי
        postDao.updatePost(updatedPost)
        
        return updatedPost
    }
    
    // פונקציה לשמירת פוסט
    suspend fun savePost(postId: String): Post {
        val currentUserId = auth.currentUser?.uid
            ?: throw Exception("User not logged in")
        
        // עדכון בשרת
        val postRef = firestore.collection("posts").document(postId)
        
        val post = postRef.get().await().toObject(Post::class.java)
            ?: throw Exception("Post not found")
            
        // בדיקה אם הפוסט כבר שמור
        if (post.savedBy.contains(currentUserId)) {
            // הסר מהשמורים
            postRef.update("savedBy", FieldValue.arrayRemove(currentUserId)).await()
            
            // הסר גם מבסיס הנתונים המקומי
            val updatedPost = post.copy(
                savedBy = post.savedBy.filter { it != currentUserId }
            )
            postDao.updatePost(updatedPost)
            
            return updatedPost
        } else {
            // הוסף לשמורים
            postRef.update("savedBy", FieldValue.arrayUnion(currentUserId)).await()
            
            // עדכון בבסיס הנתונים המקומי
            val updatedPost = post.copy(
                savedBy = post.savedBy + currentUserId
            )
            postDao.updatePost(updatedPost)
            
            return updatedPost
        }
    }

    // הסרת פוסט מהשמורים
    suspend fun unsavePost(postId: String): Post {
        val currentUserId = auth.currentUser?.uid
            ?: throw Exception("User not logged in")
        
        // עדכון בשרת
        val postRef = firestore.collection("posts").document(postId)
        
        val post = postRef.get().await().toObject(Post::class.java)
            ?: throw Exception("Post not found")
            
        // בדיקה אם הפוסט שמור
        if (post.savedBy.contains(currentUserId)) {
            // הסר מהשמורים
            postRef.update("savedBy", FieldValue.arrayRemove(currentUserId)).await()
            
            // הסר גם מבסיס הנתונים המקומי
            val updatedPost = post.copy(
                savedBy = post.savedBy.filter { it != currentUserId }
            )
            postDao.updatePost(updatedPost)
            
            return updatedPost
        }
        
        return post
    }

    // פונקציה לרענון פוסטים שמורים
    suspend fun refreshSavedPosts() {
        val currentUserId = auth.currentUser?.uid
        Log.d("PostRepository", "refreshSavedPosts called, currentUserId: $currentUserId")
        
        if (currentUserId == null) {
            Log.d("PostRepository", "No user logged in, skipping refresh")
            return
        }
        
        try {
            Log.d("PostRepository", "Refreshing saved posts from server for user: $currentUserId")
            val remotePosts = firestore.collection("posts")
                .whereArrayContains("savedBy", currentUserId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Post::class.java) }
            
            Log.d("PostRepository", "Got ${remotePosts.size} saved posts from server")
            
            if (remotePosts.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    postDao.insertPosts(remotePosts)
                    Log.d("PostRepository", "Inserted ${remotePosts.size} saved posts to local database")
                }
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error refreshing saved posts", e)
            throw e
        }
    }

    // פונקציה לקבלת פוסטים רק ממשתמשים שאני עוקב אחריהם
    fun getFollowingPosts(): Flow<List<Post>> = flow {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            emit(emptyList<Post>())
            return@flow
        }
        
        try {
            // קבלת רשימת המשתמשים שאני עוקב אחריהם
            val userDoc = firestore.collection("users").document(currentUserId).get().await()
            val currentUser = userDoc.toObject(com.example.dressit.data.model.User::class.java)
            
            if (currentUser == null || currentUser.following.isEmpty()) {
                Log.d("PostRepository", "User not found or not following anyone")
                emit(emptyList<Post>())
                return@flow
            }
            
            Log.d("PostRepository", "User follows ${currentUser.following.size} users")
            
            // ניסיון לקבל תחילה פוסטים מהמסד המקומי שמשתייכים למשתמשים שאני עוקב אחריהם
            // (נשתמש בלוגיקה חלופית שלא דורשת firstOrNull)
            val filteredPosts = withContext(Dispatchers.IO) {
                val allLocalPosts = postDao.getAllPostsSync()
                allLocalPosts.filter { post -> 
                    currentUser.following.contains(post.userId) 
                }
            }
            
            // שליחת התוצאות הראשוניות מהמסד המקומי
            emit(filteredPosts)
            
            // רענון מהשרת
            try {
                Log.d("PostRepository", "Refreshing posts from followed users")
                
                // קבלת פוסטים ממשתמשים שאני עוקב אחריהם
                val posts = mutableListOf<Post>()
                
                // אוסף פוסטים מכל משתמש שאני עוקב אחריו
                for (followedUserId in currentUser.following) {
                    val userPosts = firestore.collection("posts")
                        .whereEqualTo("userId", followedUserId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(Post::class.java) }
                        
                    posts.addAll(userPosts)
                }
                
                Log.d("PostRepository", "Found ${posts.size} posts from followed users")
                
                if (posts.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        postDao.insertPosts(posts)
                    }
                    
                    // סינון הפוסטים החדשים רק של משתמשים שאני עוקב אחריהם
                    val updatedFilteredPosts = posts.filter { post -> 
                        currentUser.following.contains(post.userId) 
                    }
                    
                    emit(updatedFilteredPosts)
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Error refreshing posts from followed users", e)
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error getting posts from followed users", e)
            emit(emptyList<Post>())
        }
    }.flowOn(Dispatchers.IO)

    private fun File.toUri() = android.net.Uri.fromFile(this)
} 