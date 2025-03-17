package com.example.dressit.ui.saved

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dressit.R
import com.example.dressit.databinding.FragmentSavedPostsBinding
import com.example.dressit.ui.home.PostAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SavedPostsFragment : Fragment() {
    private var _binding: FragmentSavedPostsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SavedPostsViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("SavedPostsFragment", "onCreateView called")
        _binding = FragmentSavedPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SavedPostsFragment", "onViewCreated called")
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
    }
    
    private fun setupRecyclerView() {
        Log.d("SavedPostsFragment", "Setting up RecyclerView")
        
        postAdapter = PostAdapter(
            onPostClick = { post ->
                Log.d("SavedPostsFragment", "Post clicked: ${post.id}")
                findNavController().navigate(R.id.action_navigation_saved_to_post_detail_fragment, Bundle().apply {
                    putString("postId", post.id)
                })
            },
            onLikeClick = { post ->
                Log.d("SavedPostsFragment", "Like clicked for post: ${post.id}")
                viewModel.likePost(post.id)
                val currentUserId = viewModel.getCurrentUserId()
                val isLiked = post.likedBy.contains(currentUserId)
                val message = if (isLiked) {
                    "הסרת לייק מהפוסט"
                } else {
                    "הוספת לייק לפוסט"
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            },
            onCommentClick = { post ->
                Log.d("SavedPostsFragment", "Comment clicked for post: ${post.id}")
                findNavController().navigate(R.id.action_navigation_saved_to_post_detail_fragment, Bundle().apply {
                    putString("postId", post.id)
                    putBoolean("openComments", true)
                })
            },
            onSaveClick = { post ->
                Log.d("SavedPostsFragment", "Save clicked for post: ${post.id}")
                viewModel.unsavePost(post.id)
                Snackbar.make(binding.root, "הפוסט הוסר מהשמורים", Snackbar.LENGTH_SHORT).show()
            },
            onUserNameClick = { userId ->
                val action = SavedPostsFragmentDirections.actionNavigationSavedToProfileFragment(userId)
                findNavController().navigate(action)
            }
        )

        binding.savedPostsRecyclerView.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }
    
    private fun setupObservers() {
        Log.d("SavedPostsFragment", "Setting up observers")
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            Log.d("SavedPostsFragment", "Received ${posts.size} saved posts")
            postAdapter.submitList(posts)
            binding.emptyView.isVisible = posts.isEmpty()
            binding.savedPostsRecyclerView.isVisible = posts.isNotEmpty()
            
            if (posts.isEmpty()) {
                Log.d("SavedPostsFragment", "No saved posts found, showing empty view")
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("SavedPostsFragment", "Loading state changed: $isLoading")
            binding.swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e("SavedPostsFragment", "Error received: $it")
                
                // בדיקה אם השגיאה קשורה ל-Room schema
                if (it.contains("Room cannot verify the data integrity") || it.contains("schema")) {
                    val snackbar = Snackbar.make(
                        binding.root,
                        "שגיאה בבסיס הנתונים. נסה לאפס את בסיס הנתונים.",
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction("אפס") {
                        showResetDatabaseDialog()
                    }
                    snackbar.show()
                } else {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showResetDatabaseDialog() {
        Log.d("SavedPostsFragment", "Showing reset database dialog")
        AlertDialog.Builder(requireContext())
            .setTitle("איפוס בסיס נתונים")
            .setMessage("האם אתה בטוח שברצונך לאפס את בסיס הנתונים המקומי?")
            .setPositiveButton("איפוס") { _, _ ->
                Log.d("SavedPostsFragment", "User confirmed database reset")
                viewModel.clearLocalDatabase()
                Snackbar.make(
                    binding.root,
                    "בסיס הנתונים אופס בהצלחה",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("ביטול") { dialog, _ ->
                Log.d("SavedPostsFragment", "User cancelled database reset")
                dialog.dismiss()
            }
            .show()
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshSavedPosts()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 