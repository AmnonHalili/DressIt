package com.example.dressit.ui.chart

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.data.model.Booking
import com.example.dressit.data.model.BookingStatus
import com.example.dressit.databinding.ItemBookingBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

class BookingAdapter(
    private val onBookingClick: (Booking) -> Unit,
    private val onApproveClick: (Booking) -> Unit,
    private val onRejectClick: (Booking) -> Unit,
    private val onCancelClick: (Booking) -> Unit
) : ListAdapter<Booking, BookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onBookingClick(getItem(position))
                }
            }

            binding.approveButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onApproveClick(getItem(position))
                }
            }

            binding.rejectButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRejectClick(getItem(position))
                }
            }

            binding.cancelButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCancelClick(getItem(position))
                }
            }
        }

        fun bind(booking: Booking) {
            binding.apply {
                // טעינת תמונת השמלה
                Glide.with(root.context)
                    .load(booking.postImage)
                    .placeholder(R.drawable.ic_error_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .into(dressImage)

                // כותרת השמלה
                dressTitle.text = booking.postTitle

                // מחיר השכרה
                val priceText = "${booking.dressPrice} ${booking.currency}"
                dressPrice.text = priceText

                // מיקום איסוף
                if (booking.latitude != null && booking.longitude != null) {
                    try {
                        val geocoder = android.location.Geocoder(root.context, java.util.Locale.getDefault())
                        val addresses = geocoder.getFromLocation(booking.latitude, booking.longitude, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val formattedAddress = StringBuilder()
                            
                            // בניית כתובת קצרה וברורה
                            if (address.thoroughfare != null) {
                                formattedAddress.append(address.thoroughfare)
                                if (address.subThoroughfare != null) {
                                    formattedAddress.append(" ").append(address.subThoroughfare)
                                }
                                
                                if (address.locality != null) {
                                    formattedAddress.append(", ").append(address.locality)
                                }
                            } else if (address.locality != null) {
                                formattedAddress.append(address.locality)
                            } else if (address.subAdminArea != null) {
                                formattedAddress.append(address.subAdminArea)
                            } else if (address.adminArea != null) {
                                formattedAddress.append(address.adminArea)
                            } else if (address.getAddressLine(0) != null) {
                                // אם לא הצלחנו לבנות כתובת, ננסה את הכתובת המלאה
                                formattedAddress.append(address.getAddressLine(0))
                            }
                            
                            if (formattedAddress.isNotEmpty()) {
                                pickupLocation.text = formattedAddress.toString()
                            } else {
                                pickupLocation.text = "פנה למוכר לפרטים"
                            }
                            
                            // האייקון יופיע מימין לטקסט (RTL)
                            pickupLocation.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_location, 0)
                            pickupLocation.compoundDrawablePadding = 8
                            
                            // הוספת גישה למפה בלחיצה על המיקום
                            val mapUrl = "https://maps.google.com/maps?q=${booking.latitude},${booking.longitude}"
                            pickupLocation.setOnClickListener {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mapUrl))
                                root.context.startActivity(intent)
                            }
                            
                            pickupLocation.visibility = View.VISIBLE
                        } else {
                            pickupLocation.text = "פנה למוכר לפרטים"
                            pickupLocation.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_location, 0)
                            pickupLocation.compoundDrawablePadding = 8
                            pickupLocation.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BookingAdapter", "Error getting address", e)
                        pickupLocation.text = "פנה למוכר לפרטים"
                        pickupLocation.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_location, 0)
                        pickupLocation.compoundDrawablePadding = 8
                        pickupLocation.visibility = View.VISIBLE
                    }
                } else {
                    // אם אין מיקום, נציג הודעה כללית
                    pickupLocation.text = "לא צוין"
                    pickupLocation.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_location, 0)
                    pickupLocation.compoundDrawablePadding = 8
                    pickupLocation.visibility = View.VISIBLE
                    pickupLocation.setOnClickListener(null)
                }

                // תאריכי השכרה
                val startDateStr = DateFormat.format("dd/MM/yyyy", Date(booking.startDate)).toString()
                val endDateStr = DateFormat.format("dd/MM/yyyy", Date(booking.endDate)).toString()
                val dateRangeText = "מ-$startDateStr עד $endDateStr"
                dateRange.text = dateRangeText

                // סטטוס ההזמנה
                val statusText = when (booking.status) {
                    BookingStatus.PENDING -> "ממתין לאישור"
                    BookingStatus.APPROVED -> "מאושר"
                    BookingStatus.REJECTED -> "נדחה"
                    BookingStatus.COMPLETED -> "הושלם"
                    BookingStatus.CANCELED -> "בוטל"
                }
                bookingStatus.text = statusText

                // צבע הסטטוס
                val statusColor = when (booking.status) {
                    BookingStatus.PENDING -> R.color.status_pending
                    BookingStatus.APPROVED -> R.color.status_approved
                    BookingStatus.REJECTED -> R.color.status_rejected
                    BookingStatus.COMPLETED -> R.color.status_completed
                    BookingStatus.CANCELED -> R.color.status_canceled
                }
                bookingStatus.setTextColor(root.context.getColor(statusColor))

                // הצגת כפתורי פעולה בהתאם לסטטוס ולמשתמש
                val isOwner = booking.ownerId == currentUserId
                val isRenter = booking.renterId == currentUserId

                // כפתורי אישור/דחייה מוצגים רק לבעל השמלה ורק אם ההזמנה ממתינה לאישור
                val showApproveReject = isOwner && booking.status == BookingStatus.PENDING
                approveButton.visibility = if (showApproveReject) View.VISIBLE else View.GONE
                rejectButton.visibility = if (showApproveReject) View.VISIBLE else View.GONE

                // כפתור ביטול מוסתר תמיד - הוסר בהתאם לדרישה
                cancelButton.visibility = View.GONE

                // הצגת פרטי המשתמש השני
                if (isOwner) {
                    // אם המשתמש הנוכחי הוא בעל השמלה, מציגים את פרטי השוכר
                    userLabel.text = "שוכר:"
                    userName.text = booking.renterName
                } else {
                    // אם המשתמש הנוכחי הוא השוכר, מציגים את פרטי בעל השמלה
                    userLabel.text = "בעל השמלה:"
                    userName.text = booking.ownerName
                }

                // הצגת הערות אם יש
                if (booking.notes.isNotEmpty()) {
                    notesLayout.visibility = View.VISIBLE
                    notesText.text = booking.notes
                } else {
                    notesLayout.visibility = View.GONE
                }
            }
        }
    }

    private class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
} 