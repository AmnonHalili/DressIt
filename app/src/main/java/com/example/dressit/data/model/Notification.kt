package com.example.dressit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",  // מי מקבל את ההתראה
    val type: NotificationType = NotificationType.LIKE,
    val postId: String = "",  // הפוסט שעליו בוצעה הפעולה
    val actorId: String = "", // מי שביצע את הפעולה (לייק, תגובה וכו')
    val actorName: String = "", // שם המשתמש שביצע את הפעולה
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val thumbnailUrl: String = "" // תמונה ממוזערת של הפוסט
)

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    SAVE
} 