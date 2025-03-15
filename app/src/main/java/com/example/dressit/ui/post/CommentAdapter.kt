package com.example.dressit.ui.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dressit.R
import com.example.dressit.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private val currentUserId: String,
    private val postOwnerId: String,
    private val onDeleteClick: (Comment) -> Unit,
    private val onUserNameClick: ((String) -> Unit)? = null
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
            // וידוא שיש שם משתמש תקף (אם חסר, נשים ברירת מחדל)
            val displayName = if (comment.userName.isNotEmpty()) {
                comment.userName
            } else {
                "משתמש אפליקציה"
            }
            
            // הצגת שם המשתמש
            userName.text = displayName
            
            // הוספת מאזין לחיצה לשם המשתמש
            userName.setOnClickListener {
                if (onUserNameClick != null) {
                    // אם סופק מאזין חיצוני, נשתמש בו
                    onUserNameClick.invoke(comment.userId)
                } else {
                    // אחרת, ננווט ישירות לפרופיל המשתמש
                    if (comment.userId != FirebaseAuth.getInstance().currentUser?.uid) {
                        // אם זה לא המשתמש הנוכחי, ננווט לפרופיל של המשתמש האחר
                        val action = PostDetailFragmentDirections.actionPostDetailToProfileFragment(comment.userId)
                        itemView.findNavController().navigate(action)
                    } else {
                        // אם זה המשתמש הנוכחי, ננווט לטאב של הפרופיל
                        itemView.findNavController().navigate(R.id.profileFragment)
                    }
                }
            }
            
            // הצגת תוכן התגובה
            commentText.text = comment.text
            
            // הצגת זמן התגובה בפורמט הנדרש
            timestamp.text = formatTimestamp(comment.timestamp)
            
            // הצגת כפתור מחיקה רק אם המשתמש הנוכחי הוא בעל התגובה או בעל הפוסט
            val canDelete = comment.userId == currentUserId || postOwnerId == currentUserId
            deleteButton.isVisible = canDelete
            
            deleteButton.setOnClickListener {
                onDeleteClick(comment)
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            // פורמט פשוט וקריא: "12.06.2020 - 19:35"
            val sdf = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault())
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