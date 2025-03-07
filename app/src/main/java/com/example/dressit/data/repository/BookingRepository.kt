package com.example.dressit.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.example.dressit.data.local.AppDatabase
import com.example.dressit.data.model.Booking
import com.example.dressit.data.model.BookingStatus
import com.example.dressit.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class BookingRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val bookingDao = AppDatabase.getDatabase(context).bookingDao()

    // קבלת כל ההזמנות של המשתמש (הן כשוכר והן כבעלים)
    fun getUserBookings(): Flow<List<Booking>> {
        val currentUserId = auth.currentUser?.uid ?: return bookingDao.getUserBookings("")
        return bookingDao.getUserBookings(currentUserId)
    }

    // קבלת הזמנות שהמשתמש יצר (כשוכר)
    fun getUserRentalBookings(): Flow<List<Booking>> {
        val currentUserId = auth.currentUser?.uid ?: return bookingDao.getUserRentalBookings("")
        return bookingDao.getUserRentalBookings(currentUserId)
    }

    // קבלת הזמנות שהמשתמש קיבל (כבעלים)
    fun getUserDressBookings(): Flow<List<Booking>> {
        val currentUserId = auth.currentUser?.uid ?: return bookingDao.getUserDressBookings("")
        return bookingDao.getUserDressBookings(currentUserId)
    }

    // קבלת הזמנות עבור פוסט ספציפי
    fun getBookingsForPost(postId: String): Flow<List<Booking>> {
        return bookingDao.getBookingsForPost(postId)
    }

    // יצירת הזמנה חדשה
    suspend fun createBooking(
        post: Post,
        startDate: Long,
        endDate: Long,
        notes: String = ""
    ): Booking {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        // קבלת שם המשתמש הנוכחי
        var renterName = currentUser.displayName ?: ""
        
        // אם אין שם משתמש ב-Auth, ננסה לקבל מ-Firestore
        if (renterName.isEmpty()) {
            try {
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val firestoreUser = userDoc.toObject(com.example.dressit.data.model.User::class.java)
                renterName = firestoreUser?.username ?: ""
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error getting username from Firestore", e)
            }
        }
        
        // אם עדיין אין שם, נשתמש במשהו בסיסי
        if (renterName.isEmpty()) {
            renterName = "משתמש אפליקציה"
        }
        
        // הכנת מיקום האיסוף - המרת קואורדינטות לכתובת אם אפשר
        var pickupLocationText = "מיקום לא צוין"
        if (post.latitude != null && post.longitude != null) {
            try {
                val geocoder = Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(post.latitude, post.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    pickupLocationText = when {
                        address.thoroughfare != null -> "${address.thoroughfare}, ${address.locality ?: address.adminArea ?: ""}"
                        address.locality != null -> address.locality
                        else -> "מיקום: ${post.latitude.toString().take(6)}, ${post.longitude.toString().take(6)}"
                    }
                } else {
                    pickupLocationText = "מיקום: ${post.latitude.toString().take(6)}, ${post.longitude.toString().take(6)}"
                }
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error getting address", e)
                pickupLocationText = "מיקום: ${post.latitude.toString().take(6)}, ${post.longitude.toString().take(6)}"
            }
        }
        
        // יצירת אובייקט הזמנה
        val booking = Booking(
            id = UUID.randomUUID().toString(),
            renterId = currentUser.uid,
            renterName = renterName,
            ownerId = post.userId,
            ownerName = post.userName,
            postId = post.id,
            postTitle = post.title,
            postImage = post.imageUrl,
            dressPrice = post.rentalPrice,
            currency = post.currency,
            pickupLocation = pickupLocationText,
            latitude = post.latitude,
            longitude = post.longitude,
            startDate = startDate,
            endDate = endDate,
            status = BookingStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            notes = notes
        )
        
        return withContext(Dispatchers.IO) {
            try {
                // שמירה ב-Firestore
                Log.d("BookingRepository", "Attempting to save booking to Firestore: ${booking.id}")
                firestore.collection("bookings").document(booking.id).set(booking).await()
                Log.d("BookingRepository", "Booking saved to Firestore successfully")
                
                // שמירה במסד הנתונים המקומי
                Log.d("BookingRepository", "Saving booking to local database")
                bookingDao.insertBooking(booking)
                
                Log.d("BookingRepository", "Booking created successfully: ${booking.id}")
                booking
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error creating booking", e)
                // הדפסת פרטי שגיאה מפורטים יותר
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    Log.e("BookingRepository", "Permission denied error. Check Firestore security rules.", e)
                } else {
                    Log.e("BookingRepository", "Unexpected error: ${e.javaClass.simpleName}: ${e.message}", e)
                }
                throw e
            }
        }
    }

    // עדכון סטטוס הזמנה
    suspend fun updateBookingStatus(bookingId: String, newStatus: BookingStatus): Booking {
        return withContext(Dispatchers.IO) {
            try {
                // קבלת ההזמנה מ-Firestore
                val bookingDoc = firestore.collection("bookings").document(bookingId).get().await()
                val booking = bookingDoc.toObject(Booking::class.java) ?: throw Exception("Booking not found")
                
                // עדכון הסטטוס
                val updatedBooking = booking.copy(status = newStatus)
                
                // שמירה ב-Firestore
                firestore.collection("bookings").document(bookingId).set(updatedBooking).await()
                
                // שמירה במסד הנתונים המקומי
                bookingDao.updateBooking(updatedBooking)
                
                Log.d("BookingRepository", "Booking status updated: $bookingId -> $newStatus")
                updatedBooking
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error updating booking status", e)
                throw e
            }
        }
    }

    // מחיקת הזמנה
    suspend fun deleteBooking(bookingId: String) {
        withContext(Dispatchers.IO) {
            try {
                // מחיקה מ-Firestore
                firestore.collection("bookings").document(bookingId).delete().await()
                
                // מחיקה ממסד הנתונים המקומי
                bookingDao.deleteBooking(bookingId)
                
                Log.d("BookingRepository", "Booking deleted: $bookingId")
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error deleting booking", e)
                throw e
            }
        }
    }

    // רענון הזמנות מהשרת
    suspend fun refreshBookings() {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@withContext
                
                // מחיקת הזמנות ישנות
                bookingDao.deleteUserBookings(currentUserId)
                
                // קבלת הזמנות שהמשתמש יצר (כשוכר)
                val rentalBookings = firestore.collection("bookings")
                    .whereEqualTo("renterId", currentUserId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Booking::class.java) }
                
                // קבלת הזמנות שהמשתמש קיבל (כבעלים)
                val dressBookings = firestore.collection("bookings")
                    .whereEqualTo("ownerId", currentUserId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Booking::class.java) }
                
                // איחוד ההזמנות ושמירה במסד הנתונים המקומי
                val allBookings = rentalBookings + dressBookings
                if (allBookings.isNotEmpty()) {
                    bookingDao.insertBookings(allBookings)
                }
                
                Log.d("BookingRepository", "Refreshed ${allBookings.size} bookings")
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error refreshing bookings", e)
                throw e
            }
        }
    }

    // קבלת מספר ההזמנות הממתינות לאישור
    fun getPendingBookingsCount(): Flow<Int> {
        val currentUserId = auth.currentUser?.uid ?: return bookingDao.getPendingBookingsCount("")
        return bookingDao.getPendingBookingsCount(currentUserId)
    }
} 