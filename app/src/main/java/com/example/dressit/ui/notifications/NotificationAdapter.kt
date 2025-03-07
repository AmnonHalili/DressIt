package com.example.dressit.ui.notifications

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.data.model.Notification
import com.example.dressit.data.model.NotificationType
import com.example.dressit.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit,
    private val onMarkAsRead: (String) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val notification = getItem(position)
                    // Mark as read when clicked
                    if (!notification.isRead) {
                        onMarkAsRead(notification.id)
                    }
                    onNotificationClick(notification)
                }
            }
        }

        fun bind(notification: Notification) {
            binding.apply {
                // Set notification message based on type
                val message = when (notification.type) {
                    NotificationType.LIKE -> 
                        "${notification.actorName} עשה/תה לייק לפוסט שלך"
                    NotificationType.COMMENT -> 
                        "${notification.actorName} הגיב/ה על הפוסט שלך"
                    NotificationType.FOLLOW -> 
                        "${notification.actorName} התחיל/ה לעקוב אחריך"
                    NotificationType.SAVE -> 
                        "${notification.actorName} שמר/ה את הפוסט שלך"
                }
                notificationText.text = message

                // Set notification time
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                notificationTime.text = timeAgo.toString()

                // Set icon based on notification type
                val iconRes = when (notification.type) {
                    NotificationType.LIKE -> R.drawable.ic_like_filled
                    NotificationType.COMMENT -> R.drawable.ic_comment_outline
                    NotificationType.FOLLOW -> R.drawable.ic_profile
                    NotificationType.SAVE -> R.drawable.ic_save_filled
                }
                notificationIcon.setImageResource(iconRes)

                // Set post thumbnail if available
                if (notification.thumbnailUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(notification.thumbnailUrl)
                        .placeholder(R.drawable.ic_error_placeholder)
                        .error(R.drawable.ic_error_placeholder)
                        .into(postImage)
                    postImage.visibility = View.VISIBLE
                } else {
                    postImage.visibility = View.GONE
                }

                // Show unread indicator for unread notifications
                unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
} 