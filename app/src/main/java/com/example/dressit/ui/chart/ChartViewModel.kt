package com.example.dressit.ui.chart

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dressit.data.model.Booking
import com.example.dressit.data.model.BookingStatus
import com.example.dressit.data.model.Post
import com.example.dressit.data.repository.BookingRepository
import com.example.dressit.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _bookings = MutableLiveData<List<Booking>>(emptyList())
    val bookings: LiveData<List<Booking>> = _bookings

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        Log.d("ChartViewModel", "Initializing ChartViewModel")
        loadBookings()
    }

    private fun loadBookings() {
        _loading.value = true
        _error.value = null
        
        Log.d("ChartViewModel", "Loading bookings")
        
        // תמיד נרענן קודם מהשרת ואז נקרא מהמסד המקומי
        viewModelScope.launch {
            try {
                Log.d("ChartViewModel", "Refreshing bookings from server")
                bookingRepository.refreshBookings()
                Log.d("ChartViewModel", "Successfully refreshed bookings")
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Error refreshing bookings", e)
                _error.value = e.message ?: "שגיאה ברענון הזמנות"
            }
        }

        bookingRepository.getUserBookings()
            .onEach { bookings ->
                Log.d("ChartViewModel", "Got ${bookings.size} bookings")
                _bookings.value = bookings.sortedByDescending { it.createdAt }
                _loading.value = false
            }
            .catch { exception ->
                Log.e("ChartViewModel", "Error loading bookings", exception)
                _error.value = exception.message ?: "שגיאה בטעינת הזמנות"
                _loading.value = false
            }
            .launchIn(viewModelScope)
    }

    fun refreshBookings() {
        _loading.value = true
        _error.value = null
        
        Log.d("ChartViewModel", "Manual refresh of bookings")

        viewModelScope.launch {
            try {
                bookingRepository.refreshBookings()
                _loading.value = false
                Log.d("ChartViewModel", "Manual refresh completed")
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Error during manual refresh", e)
                _error.value = e.message ?: "שגיאה ברענון הזמנות"
                _loading.value = false
            }
        }
    }

    fun createBooking(post: Post, startDate: Long, endDate: Long, notes: String = "") {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                Log.d("ChartViewModel", "Creating booking for post: ${post.id}")
                Log.d("ChartViewModel", "Post location: Lat=${post.latitude}, Long=${post.longitude}")
                
                val booking = bookingRepository.createBooking(post, startDate, endDate, notes)
                Log.d("ChartViewModel", "Booking created successfully: ${booking.id}")
                Log.d("ChartViewModel", "Booking location data: ${booking.pickupLocation}, Lat=${booking.latitude}, Long=${booking.longitude}")
                
                // רענון מיידי של ההזמנות אחרי יצירה
                refreshBookings()
                
                // רענון נוסף אחרי חצי שניה
                delay(500)
                refreshBookings()
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Error creating booking", e)
                _error.value = e.message ?: "שגיאה ביצירת הזמנה"
                _loading.value = false
            }
        }
    }

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        viewModelScope.launch {
            try {
                Log.d("ChartViewModel", "Updating booking status: $bookingId -> $newStatus")
                bookingRepository.updateBookingStatus(bookingId, newStatus)
                refreshBookings()
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Error updating booking status", e)
                _error.value = e.message
            }
        }
    }

    fun deleteBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                Log.d("ChartViewModel", "Deleting booking: $bookingId")
                bookingRepository.deleteBooking(bookingId)
                refreshBookings()
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Error deleting booking", e)
                _error.value = e.message
            }
        }
    }
} 