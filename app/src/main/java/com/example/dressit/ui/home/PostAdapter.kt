package com.example.dressit.ui.home

import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
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
import com.example.dressit.data.repository.UserRepository
import com.example.dressit.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onSaveClick: ((Post) -> Unit)? = null,
    private val onChartClick: ((Post) -> Unit)? = null,
    private val onUserNameClick: ((String) -> Unit)? = null
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val userRepository = UserRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
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
            
            binding.btnChart.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && onChartClick != null) {
                    val post = getItem(position)
                    Log.d("PostAdapter", "Chart clicked for post: ${post.id}")
                    onChartClick.invoke(post)
                }
            }
        }

        fun bind(post: Post) {
            binding.apply {
                // הצגת שם המשתמש האמיתי - אם יש שם משתמש, נציג אותו
                val userName = post.userName.trim()
                
                // אם יש שם משתמש, נשתמש בו. אחרת נשתמש בברירת מחדל
                val displayName = if (userName.isNotEmpty()) {
                    userName  // השתמש בשם המשתמש כפי שהוא
                } else {
                    "משתמש אפליקציה"  // רק אם אין שם בכלל
                }
                
                // הצגת שם המשתמש
                tvPostPublisher.text = displayName
                
                // טעינת תמונת פרופיל של המשתמש
                loadUserProfileImage(post.userId, tvPostPublisher)
                
                Log.d("PostAdapter", "Post ${post.id}: Using actual username: '$displayName' (original: '${post.userName}')")
                
                // הצגת שם המשתמש
                tvUserName.text = displayName
                
                // הוספת מאזין לחיצה לשם המשתמש
                tvUserName.setOnClickListener {
                    if (onUserNameClick != null) {
                        // אם סופק מאזין חיצוני, נשתמש בו
                        onUserNameClick.invoke(post.userId)
                    } else {
                        // במצב זה, אנחנו לא יכולים לנווט ישירות
                        // וההורה שלנו צריך לספק פונקציית onUserNameClick
                        Log.w("PostAdapter", "onUserNameClick is null, navigation not possible")
                    }
                }
                
                // הגדרת כותרת הפוסט בחלקים התחתונים
                tvTitle.text = post.title
                
                // הגדרת התיאור (מוסתר כברירת מחדל)
                tvDescription.text = post.description
                
                // הצגת מיקום אם קיים
                if (post.latitude != null && post.longitude != null) {
                    try {
                        val geocoder = Geocoder(root.context, java.util.Locale.getDefault())
                        val addresses = geocoder.getFromLocation(post.latitude, post.longitude, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val formattedAddress = StringBuilder()
                            
                            // בניית כתובת קצרה וברורה
                            if (address.thoroughfare != null) {
                                formattedAddress.append(address.thoroughfare)
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
                                tvLocation.text = formattedAddress
                                tvLocation.visibility = View.VISIBLE
                            } else {
                                tvLocation.visibility = View.GONE
                            }
                        } else {
                            tvLocation.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        // במקרה של שגיאה, לא נציג את המיקום כלל
                        Log.e("PostAdapter", "Error getting address", e)
                        tvLocation.visibility = View.GONE
                    }
                } else {
                    tvLocation.visibility = View.GONE
                }
                
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

    // פונקציה שטוענת את תמונת הפרופיל של המשתמש
    private fun loadUserProfileImage(userId: String, userNameView: View) {
        if (userId.isBlank()) return
        
        coroutineScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    userRepository.getUserById(userId)
                }
                
                if (user != null && user.profilePicture.isNotEmpty()) {
                    // טעינת תמונת הפרופיל רק אם קיימת
                    if (userNameView is android.widget.TextView) {
                        withContext(Dispatchers.Main) {
                            Glide.with(userNameView.context)
                                .load(user.profilePicture)
                                .circleCrop() // עיגול של התמונה
                                .placeholder(R.drawable.ic_profile) // התמונה הזמנית עד שהתמונה האמיתית נטענת
                                .error(R.drawable.ic_profile) // התמונה שתוצג במקרה של שגיאה
                                .into(object : com.bumptech.glide.request.target.CustomTarget<Drawable>() {
                                    override fun onResourceReady(
                                        resource: Drawable,
                                        transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                                    ) {
                                        // הגדרת גודל האייקון
                                        resource.setBounds(0, 0, 80, 80)
                                        
                                        // שמירה על הריווח המקורי
                                        val drawables = userNameView.compoundDrawables
                                        userNameView.setCompoundDrawables(resource, drawables[1], drawables[2], drawables[3])
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                        // אין צורך לעשות כלום
                                    }
                                })
                        }
                    }
                } else {
                    Log.d("PostAdapter", "No profile picture for user $userId or user not found")
                }
            } catch (e: Exception) {
                Log.e("PostAdapter", "Error loading profile picture for user $userId", e)
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