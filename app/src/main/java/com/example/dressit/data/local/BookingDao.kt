package com.example.dressit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dressit.data.model.Booking
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE renterId = :userId OR ownerId = :userId ORDER BY startDate DESC")
    fun getUserBookings(userId: String): Flow<List<Booking>>
    
    @Query("SELECT * FROM bookings WHERE renterId = :userId ORDER BY startDate DESC")
    fun getUserRentalBookings(userId: String): Flow<List<Booking>>
    
    @Query("SELECT * FROM bookings WHERE ownerId = :userId ORDER BY startDate DESC")
    fun getUserDressBookings(userId: String): Flow<List<Booking>>
    
    @Query("SELECT * FROM bookings WHERE postId = :postId ORDER BY startDate DESC")
    fun getBookingsForPost(postId: String): Flow<List<Booking>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<Booking>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)
    
    @Update
    suspend fun updateBooking(booking: Booking)
    
    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun deleteBooking(bookingId: String)
    
    @Query("DELETE FROM bookings WHERE renterId = :userId OR ownerId = :userId")
    suspend fun deleteUserBookings(userId: String)
    
    @Query("DELETE FROM bookings")
    suspend fun deleteAllBookings()
    
    @Query("SELECT COUNT(*) FROM bookings WHERE ownerId = :userId AND status = 'PENDING'")
    fun getPendingBookingsCount(userId: String): Flow<Int>
} 