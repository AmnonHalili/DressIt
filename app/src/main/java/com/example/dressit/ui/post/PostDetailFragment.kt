package com.example.dressit.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.databinding.FragmentPostDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PostDetailFragment : Fragment() {
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private val args: PostDetailFragmentArgs by navArgs()
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCommentsRecyclerView()
        setupObservers()
        setupClickListeners()
        viewModel.loadPost(args.postId)
    }

    private fun setupCommentsRecyclerView() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        
        commentAdapter = CommentAdapter(
            currentUserId = currentUserId,
            postOwnerId = "", // יעודכן כשהפוסט יטען
            onDeleteClick = { comment ->
                viewModel.deleteComment(args.postId, comment.id)
            }
        )
        
        binding.commentsRecyclerView.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        binding.editButton.setOnClickListener {
            viewModel.post.value?.let { post ->
                val action = PostDetailFragmentDirections.actionPostDetailToEditPost(post.id)
                findNavController().navigate(action)
            }
        }
        
        binding.contactButton.setOnClickListener {
            // TODO: Implement contact functionality
            Snackbar.make(binding.root, "פונקציונליות יצירת קשר תיושם בקרוב", Snackbar.LENGTH_SHORT).show()
        }
        
        // לייק
        binding.likeButton.setOnClickListener {
            viewModel.toggleLike(args.postId)
        }
        
        // תגובה
        binding.commentButton.setOnClickListener {
            binding.commentInput.requestFocus()
        }
        
        // שמירה
        binding.saveButton.setOnClickListener {
            viewModel.toggleSave(args.postId)
        }
        
        // שליחת תגובה
        binding.sendCommentButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                viewModel.addComment(args.postId, commentText)
                binding.commentInput.text.clear()
            }
        }
    }

    private fun setupObservers() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                binding.apply {
                    title.text = post.title
                    userName.text = post.userName
                    description.text = post.description
                    timestamp.text = formatTimestamp(post.timestamp)
                    
                    // הצגת מחיר השכרה
                    val formattedPrice = formatPrice(post.rentalPrice, post.currency)
                    rentalPrice.text = formattedPrice
                    
                    // עדכון מספר הלייקים
                    likesCount.text = "${post.likes} לייקים"
                    
                    // עדכון מספר התגובות
                    commentsCount.text = "${post.comments.size} תגובות"
                    
                    // עדכון מצב הלייק
                    updateLikeButton(viewModel.isPostLikedByCurrentUser(post))
                    
                    // עדכון מצב השמירה
                    updateSaveButton(viewModel.isPostSavedByCurrentUser(post))
                    
                    // עדכון רשימת התגובות
                    commentAdapter = CommentAdapter(
                        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        postOwnerId = post.userId,
                        onDeleteClick = { comment ->
                            viewModel.deleteComment(post.id, comment.id)
                        }
                    )
                    commentsRecyclerView.adapter = commentAdapter
                    commentAdapter.submitList(post.comments)
                    
                    // בדיקה אם המשתמש הנוכחי הוא בעל הפוסט
                    val isOwner = viewModel.isCurrentUserPostOwner(post)
                    ownerActions.isVisible = isOwner
                    contactButton.isVisible = !isOwner
                }
                
                // Load post image
                Glide.with(requireContext())
                    .load(post.imageUrl)
                    .into(binding.postImage)
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.postImage.isVisible = !isLoading
            binding.title.isVisible = !isLoading
            binding.rentalPrice.isVisible = !isLoading
            binding.userName.isVisible = !isLoading
            binding.description.isVisible = !isLoading
            binding.timestamp.isVisible = !isLoading
            binding.interactionLayout.isVisible = !isLoading
            binding.commentsSection.isVisible = !isLoading
            binding.ownerActions.isVisible = !isLoading && viewModel.post.value?.let { viewModel.isCurrentUserPostOwner(it) } ?: false
            binding.contactButton.isVisible = !isLoading && viewModel.post.value?.let { !viewModel.isCurrentUserPostOwner(it) } ?: false
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.postDeleted.observe(viewLifecycleOwner) { isDeleted ->
            if (isDeleted) {
                findNavController().navigateUp()
            }
        }
        
        viewModel.commentAdded.observe(viewLifecycleOwner) { isAdded ->
            if (isAdded) {
                Snackbar.make(binding.root, "התגובה נוספה בהצלחה", Snackbar.LENGTH_SHORT).show()
            }
        }
        
        viewModel.commentDeleted.observe(viewLifecycleOwner) { isDeleted ->
            if (isDeleted) {
                Snackbar.make(binding.root, "התגובה נמחקה בהצלחה", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateLikeButton(isLiked: Boolean) {
        if (isLiked) {
            binding.likeButton.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            binding.likeButton.setImageResource(android.R.drawable.btn_star_big_off)
        }
    }
    
    private fun updateSaveButton(isSaved: Boolean) {
        if (isSaved) {
            binding.saveButton.setImageResource(android.R.drawable.ic_menu_save)
            binding.saveText.text = "נשמר"
        } else {
            binding.saveButton.setImageResource(android.R.drawable.ic_menu_save)
            binding.saveText.text = "שמור"
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("מחיקת פוסט")
            .setMessage("האם את/ה בטוח/ה שברצונך למחוק את הפוסט?")
            .setNegativeButton("ביטול") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("מחק") { _, _ ->
                viewModel.deletePost()
            }
            .show()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatPrice(price: Double, currency: String): String {
        val format = NumberFormat.getCurrencyInstance(Locale("he", "IL"))
        format.currency = java.util.Currency.getInstance(currency)
        return format.format(price) + " להשכרה"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 