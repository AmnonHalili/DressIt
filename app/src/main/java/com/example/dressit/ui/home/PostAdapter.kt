package com.example.dressit.ui.home

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.dressit.R
import com.example.dressit.data.model.Post
import com.example.dressit.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth

class PostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onSaveClick: ((Post) -> Unit)? = null
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    init {
        Log.d("PostAdapter", "Initialized with currentUserId: $currentUserId")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        Log.d("PostAdapter", "Creating new ViewHolder")
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        Log.d("PostAdapter", "Binding post at position $position: ${post.id}, title: ${post.title}")
        holder.bind(post)
    }
    
    override fun submitList(list: List<Post>?) {
        Log.d("PostAdapter", "Submitting list with ${list?.size ?: 0} posts")
        if (list != null && list.isNotEmpty()) {
            Log.d("PostAdapter", "First post ID: ${list[0].id}, title: ${list[0].title}")
        }
        super.submitList(list)
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = getItem(position)
                    Log.d("PostAdapter", "Post clicked: ${post.id}")
                    onPostClick(post)
                }
            }

            binding.btnLike.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = getItem(position)
                    Log.d("PostAdapter", "Like clicked for post: ${post.id}")
                    onLikeClick(post)
                }
            }

            binding.btnComment.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = getItem(position)
                    Log.d("PostAdapter", "Comment clicked for post: ${post.id}")
                    onCommentClick(post)
                }
            }
            
            binding.btnSave.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && onSaveClick != null) {
                    val post = getItem(position)
                    Log.d("PostAdapter", "Save clicked for post: ${post.id}")
                    onSaveClick.invoke(post)
                }
            }
        }

        fun bind(post: Post) {
            binding.apply {
                // הגדרת שם המשתמש וכותרת הפוסט
                tvUserName.text = post.userName
                tvTitle.text = post.title
                
                // הגדרת התיאור (מוסתר כברירת מחדל)
                tvDescription.text = post.description
                
                // הגדרת מחיר השכרה
                tvRentalPrice.text = "₪${post.rentalPrice}"
                
                // הגדרת מידע על לייקים ותגובות (מוסתר כברירת מחדל)
                tvLikes.text = "${post.likes} לייקים"
                tvComments.text = "${post.comments.size} תגובות"
                
                // עדכון מצב הלייק
                val isLiked = post.likedBy.contains(currentUserId)
                Log.d("PostAdapter", "Post ${post.id} isLiked: $isLiked")
                updateLikeButton(isLiked)
                
                // עדכון מצב השמירה
                val isSaved = post.savedBy.contains(currentUserId)
                Log.d("PostAdapter", "Post ${post.id} isSaved: $isSaved")
                updateSaveButton(isSaved)

                // הצגת אינדיקטור טעינה
                progressBar.visibility = View.VISIBLE
                ivPostImage.visibility = View.VISIBLE

                // טעינת התמונה באמצעות Glide
                Log.d("PostAdapter", "Loading image for post ${post.id}: ${post.imageUrl}")
                Glide.with(root)
                    .load(post.imageUrl)
                    .placeholder(R.drawable.ic_error_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            progressBar.visibility = View.GONE
                            Log.e("PostAdapter", "Failed to load image for post ${post.id}", e)
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            progressBar.visibility = View.GONE
                            Log.d("PostAdapter", "Image loaded successfully for post ${post.id}")
                            return false
                        }
                    })
                    .into(ivPostImage)
            }
        }
        
        private fun updateLikeButton(isLiked: Boolean) {
            if (isLiked) {
                binding.btnLike.setImageResource(R.drawable.ic_like_filled)
            } else {
                binding.btnLike.setImageResource(R.drawable.ic_like_outline)
            }
        }
        
        private fun updateSaveButton(isSaved: Boolean) {
            if (isSaved) {
                binding.btnSave.setImageResource(R.drawable.ic_save_filled)
            } else {
                binding.btnSave.setImageResource(R.drawable.ic_save_outline)
            }
        }
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
} 