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
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

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

    /**
     * עקיבה אחרי משתמש
     * @param userIdToFollow מזהה המשתמש שרוצים לעקוב אחריו
     * @return האם הפעולה הצליחה
     */
    suspend fun followUser(userIdToFollow: String): Boolean {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val currentUserId = currentUser.uid

        if (currentUserId == userIdToFollow) {
            return false // לא ניתן לעקוב אחרי עצמך
        }

        return try {
            // עדכון רשימת העוקבים אחרי של המשתמש הנוכחי
            val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
            val currentUserObj = currentUserDoc.toObject(User::class.java) ?: return false
            
            if (currentUserObj.following.contains(userIdToFollow)) {
                return true // כבר עוקב אחריו
            }
            
            val updatedFollowing = currentUserObj.following + userIdToFollow
            firestore.collection("users").document(currentUserId)
                .update("following", updatedFollowing)
                .await()
                
            // עדכון רשימת העוקבים של המשתמש השני
            val otherUserDoc = firestore.collection("users").document(userIdToFollow).get().await()
            val otherUserObj = otherUserDoc.toObject(User::class.java) ?: return false
            
            val updatedFollowers = otherUserObj.followers + currentUserId
            firestore.collection("users").document(userIdToFollow)
                .update("followers", updatedFollowers)
                .await()
                
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error following user: ${e.message}")
            false
        }
    }

    /**
     * הפסקת עקיבה אחרי משתמש
     * @param userIdToUnfollow מזהה המשתמש שרוצים להפסיק לעקוב אחריו
     * @return האם הפעולה הצליחה
     */
    suspend fun unfollowUser(userIdToUnfollow: String): Boolean {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val currentUserId = currentUser.uid

        return try {
            // עדכון רשימת העוקבים אחרי של המשתמש הנוכחי
            val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
            val currentUserObj = currentUserDoc.toObject(User::class.java) ?: return false
            
            if (!currentUserObj.following.contains(userIdToUnfollow)) {
                return true // לא עוקב אחריו בכלל
            }
            
            val updatedFollowing = currentUserObj.following.filter { it != userIdToUnfollow }
            firestore.collection("users").document(currentUserId)
                .update("following", updatedFollowing)
                .await()
                
            // עדכון רשימת העוקבים של המשתמש השני
            val otherUserDoc = firestore.collection("users").document(userIdToUnfollow).get().await()
            val otherUserObj = otherUserDoc.toObject(User::class.java) ?: return false
            
            val updatedFollowers = otherUserObj.followers.filter { it != currentUserId }
            firestore.collection("users").document(userIdToUnfollow)
                .update("followers", updatedFollowers)
                .await()
                
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unfollowing user: ${e.message}")
            false
        }
    }

    /**
     * בדיקה האם המשתמש הנוכחי עוקב אחרי משתמש אחר
     * @param userId מזהה המשתמש לבדיקה
     * @return האם עוקב אחריו
     */
    suspend fun isFollowing(userId: String): Boolean {
        val currentUser = auth.currentUser ?: return false
        val currentUserDoc = firestore.collection("users").document(currentUser.uid).get().await()
        val currentUserObj = currentUserDoc.toObject(User::class.java) ?: return false
        
        return currentUserObj.following.contains(userId)
    }

    /**
     * קבלת רשימת עוקבים של משתמש
     * @param userId מזהה המשתמש
     * @return רשימת משתמשים שעוקבים אחרי המשתמש
     */
    fun getFollowers(userId: String): Flow<List<User>> = flow {
        try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userObj = userDoc.toObject(User::class.java)
            
            if (userObj != null && userObj.followers.isNotEmpty()) {
                val followers = userObj.followers.mapNotNull { followerId ->
                    try {
                        val followerDoc = firestore.collection("users").document(followerId).get().await()
                        followerDoc.toObject(User::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                emit(followers)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting followers: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * קבלת רשימת המשתמשים שמשתמש עוקב אחריהם
     * @param userId מזהה המשתמש
     * @return רשימת משתמשים שהמשתמש עוקב אחריהם
     */
    fun getFollowing(userId: String): Flow<List<User>> = flow {
        try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userObj = userDoc.toObject(User::class.java)
            
            if (userObj != null && userObj.following.isNotEmpty()) {
                val following = userObj.following.mapNotNull { followingId ->
                    try {
                        val followingDoc = firestore.collection("users").document(followingId).get().await()
                        followingDoc.toObject(User::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                emit(following)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting following: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
} 