package com.example.dressit.data.repository

import android.content.Context
import android.util.Log
import com.example.dressit.data.local.AppDatabase
import com.example.dressit.data.model.Notification
import com.example.dressit.data.model.NotificationType
import com.example.dressit.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationDao = AppDatabase.getDatabase(context).notificationDao()

    // קבלת התראות עבור המשתמש הנוכחי
    fun getUserNotifications(): Flow<List<Notification>> {
        val currentUserId = auth.currentUser?.uid ?: return notificationDao.getNotifications("")
        return notificationDao.getNotifications(currentUserId)
    }

    // קבלת מספר ההתראות שלא נקראו
    fun getUnreadNotificationsCount(): Flow<Int> {
        val currentUserId = auth.currentUser?.uid ?: return notificationDao.getUnreadCount("")
        return notificationDao.getUnreadCount(currentUserId)
    }

    // סימון התראה כנקראה
    suspend fun markAsRead(notificationId: String) {
        withContext(Dispatchers.IO) {
            try {
                notificationDao.markAsRead(notificationId)
                
                // עדכון ב-Firestore
                val currentUserId = auth.currentUser?.uid ?: return@withContext
                firestore.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .document(notificationId)
                    .update("isRead", true)
                    .await()
            } catch (e: Exception) {
                Log.e("NotificationRepository", "Error marking notification as read", e)
            }
        }
    }

    // סימון כל ההתראות כנקראות
    suspend fun markAllAsRead() {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@withContext
                notificationDao.markAllAsRead(currentUserId)
                
                // עדכון ב-Firestore
                val batch = firestore.batch()
                val notificationsRef = firestore.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                
                for (doc in notificationsRef.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("NotificationRepository", "Error marking all notifications as read", e)
            }
        }
    }

    // יצירת התראת לייק
    suspend fun createLikeNotification(post: Post, likedByUserId: String, likedByUserName: String) {
        val currentUserId = auth.currentUser?.uid
        
        Log.d("NotificationRepository", "Creating like notification: post=${post.id}, likedBy=$likedByUserId, postOwner=${post.userId}")
        
        if (post.userId == likedByUserId) {
            // אם המשתמש עשה לייק לפוסט של עצמו, אין צורך בהתראה
            Log.d("NotificationRepository", "Skipping notification - user liked their own post")
            return
        }
        
        if (currentUserId != likedByUserId && currentUserId != post.userId) {
            Log.d("NotificationRepository", "Warning: Current user ($currentUserId) doesn't match either the liker ($likedByUserId) or post owner (${post.userId})")
        }
        
        withContext(Dispatchers.IO) {
            try {
                val notificationId = UUID.randomUUID().toString()
                
                // יצירת אובייקט התראה
                val notification = Notification(
                    id = notificationId,
                    userId = post.userId,  // חשוב: זה המשתמש שיקבל את ההתראה (בעל הפוסט)
                    type = NotificationType.LIKE,
                    postId = post.id,
                    actorId = likedByUserId,  // מי שעשה את הלייק
                    actorName = likedByUserName,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    thumbnailUrl = post.imageUrl
                )
                
                Log.d("NotificationRepository", "Created notification object: $notificationId for user ${post.userId}")
                
                // שמירה במסד נתונים מקומי - רק אם המשתמש הנוכחי הוא בעל הפוסט
                if (currentUserId == post.userId) {
                    notificationDao.insertNotification(notification)
                    Log.d("NotificationRepository", "Saved notification to local database")
                } else {
                    Log.d("NotificationRepository", "Skipping local save - current user is not post owner")
                }
                
                // שמירה ב-Firestore - תמיד
                try {
                    firestore.collection("users")
                        .document(post.userId)  // שמירה תחת בעל הפוסט
                        .collection("notifications")
                        .document(notificationId)
                        .set(notification)
                        .await()
                    
                    Log.d("NotificationRepository", "Saved notification to Firestore for user ${post.userId}")
                    
                    // עדכון מונה התראות שלא נקראו
                    firestore.collection("users")
                        .document(post.userId)
                        .update("unreadNotifications", FieldValue.increment(1))
                        .await()
                    
                    Log.d("NotificationRepository", "Updated unread notifications counter")
                } catch (e: Exception) {
                    Log.e("NotificationRepository", "Error saving notification to Firestore", e)
                    // נמשיך למרות השגיאה כדי שלפחות יהיה מידע מקומי
                }
                
                Log.d("NotificationRepository", "Like notification created successfully for post ${post.id}")
            } catch (e: Exception) {
                Log.e("NotificationRepository", "Error creating like notification", e)
                throw e
            }
        }
    }

    // רענון התראות מהשרת
    suspend fun refreshNotifications() {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    Log.d("NotificationRepository", "No user logged in, skipping refresh")
                    return@withContext
                }
                
                Log.d("NotificationRepository", "Refreshing notifications for user: $currentUserId")
                
                // מחיקת כל ההתראות הישנות של המשתמש הנוכחי
                notificationDao.deleteAllUserNotifications(currentUserId)
                Log.d("NotificationRepository", "Deleted old notifications for user: $currentUserId")
                
                val userNotificationsRef = firestore.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                
                val remoteNotifications = userNotificationsRef
                    .get()
                    .await()
                
                Log.d("NotificationRepository", "Got ${remoteNotifications.documents.size} notifications from Firestore")
                
                val notificationList = remoteNotifications.documents
                    .mapNotNull { doc -> 
                        try {
                            // וידוא שה-userId של ההתראה הוא המשתמש הנוכחי
                            val notification = doc.toObject(Notification::class.java)
                            notification?.copy(userId = currentUserId)
                        } catch (e: Exception) {
                            Log.e("NotificationRepository", "Error converting document to Notification", e)
                            null
                        }
                    }
                
                if (notificationList.isNotEmpty()) {
                    Log.d("NotificationRepository", "Inserting ${notificationList.size} notifications to local database")
                    notificationDao.insertNotifications(notificationList)
                    Log.d("NotificationRepository", "Successfully refreshed notifications")
                } else {
                    Log.d("NotificationRepository", "No notifications found")
                }
            } catch (e: Exception) {
                Log.e("NotificationRepository", "Error refreshing notifications", e)
                throw e
            }
        }
    }
} 