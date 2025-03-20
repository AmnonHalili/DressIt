package com.example.dressit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * מודל המייצג הזמנה להשכרת שמלה
 */
@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey
    val id: String = "",
    
    // מזהי המשתמשים הקשורים להזמנה
    val renterId: String = "",      // מי ששוכר את השמלה
    val renterName: String = "",
    val ownerId: String = "",       // בעל השמלה
    val ownerName: String = "",

    // פרטי הפוסט והשמלה
    val postId: String = "",        
    val postTitle: String = "",     
    val postImage: String = "",
    val dressPrice: Double = 0.0,
    val currency: String = "ILS",
    
    // פרטי המיקום לאיסוף
    val pickupLocation: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // תאריכי ההשכרה
    val startDate: Long = 0,        // בפורמט timestamp
    val endDate: Long = 0,          // בפורמט timestamp
    
    // מידע נוסף
    val status: BookingStatus = BookingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

enum class BookingStatus {
    PENDING,    // ממתין לאישור
    APPROVED,   // אושר
    REJECTED,   // נדחה
    COMPLETED,  // הושלם
    CANCELED    // בוטל
} 