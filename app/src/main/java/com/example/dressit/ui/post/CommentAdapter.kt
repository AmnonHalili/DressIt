package com.example.dressit.ui.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dressit.R
import com.example.dressit.data.model.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private val currentUserId: String,
    private val postOwnerId: String,
    private val onDeleteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        holder.bind(comment)
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.comment_user_name)
        private val commentText: TextView = itemView.findViewById(R.id.comment_text)
        private val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_comment_button)

        fun bind(comment: Comment) {
            userName.text = comment.userName
            commentText.text = comment.text
            timestamp.text = formatTimestamp(comment.timestamp)
            
            // הצגת כפתור מחיקה רק אם המשתמש הנוכחי הוא בעל התגובה או בעל הפוסט
            val canDelete = comment.userId == currentUserId || postOwnerId == currentUserId
            deleteButton.isVisible = canDelete
            
            deleteButton.setOnClickListener {
                onDeleteClick(comment)
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
} 