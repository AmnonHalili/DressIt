package com.example.dressit.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.dressit.R
import com.example.dressit.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import com.example.dressit.data.local.AppDatabase
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.dressit.ui.post.DateRangePickerDialog
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeFragment", "onCreate called")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomeFragment", "onCreateView called")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated called")
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_force_refresh -> {
                showForceRefreshDialog()
                true
            }
            R.id.action_reset_database -> {
                showResetDatabaseDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showForceRefreshDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("רענון מאולץ")
            .setMessage("פעולה זו תנקה את בסיס הנתונים המקומי ותטען את כל הפוסטים מחדש מהשרת. האם להמשיך?")
            .setPositiveButton("כן") { _, _ ->
                viewModel.forceRefreshAndClearLocalData()
                Snackbar.make(binding.root, "מרענן נתונים מהשרת...", Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton("לא", null)
            .create()
        dialog.show()
    }
    
    private fun showResetDatabaseDialog() {
        Log.d("HomeFragment", "Showing reset database dialog")
        AlertDialog.Builder(requireContext())
            .setTitle("איפוס בסיס נתונים")
            .setMessage("האם אתה בטוח שברצונך לאפס את בסיס הנתונים המקומי?")
            .setPositiveButton("איפוס") { _, _ ->
                Log.d("HomeFragment", "User confirmed database reset")
                viewModel.clearLocalDatabase()
                Snackbar.make(
                    binding.root,
                    "בסיס הנתונים אופס בהצלחה",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("ביטול") { dialog, _ ->
                Log.d("HomeFragment", "User cancelled database reset")
                dialog.dismiss()
            }
            .show()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                val action = HomeFragmentDirections.actionNavigationHomeToPostDetailFragment(post.id)
                findNavController().navigate(action)
            },
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                val action = HomeFragmentDirections.actionNavigationHomeToPostDetailFragment(post.id, true)
                findNavController().navigate(action)
            },
            onSaveClick = { post ->
                viewModel.toggleSave(post)
            },
            onChartClick = { post ->
                // בדיקה אם המשתמש הנוכחי הוא בעל הפוסט
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (post.userId == currentUserId) {
                    // אם המשתמש הוא בעל הפוסט, מציגים הודעה שלא ניתן להשכיר שמלות שהמשתמש עצמו פרסם
                    Snackbar.make(binding.root, "לא ניתן להשכיר שמלות שאתה פרסמת", Snackbar.LENGTH_SHORT).show()
                } else {
                    // אם המשתמש אינו בעל הפוסט, מציגים את דיאלוג בחירת התאריכים
                    showDateRangePicker(post)
                }
            },
            onUserNameClick = { userId ->
                val action = HomeFragmentDirections.actionNavigationHomeToProfileFragment(userId)
                findNavController().navigate(action)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupSwipeRefresh() {
        Log.d("HomeFragment", "Setting up SwipeRefresh")
        binding.swipeRefresh.setOnRefreshListener {
            Log.d("HomeFragment", "SwipeRefresh triggered")
            viewModel.refreshPosts()
        }
    }

    private fun observeViewModel() {
        Log.d("HomeFragment", "Setting up ViewModel observers")
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("HomeFragment", "Loading state changed: $isLoading")
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e("HomeFragment", "Error received: $it")
                
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

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            Log.d("HomeFragment", "Received ${posts.size} posts")
            postAdapter.submitList(posts)
            binding.emptyView.isVisible = posts.isEmpty()
            binding.recyclerView.isVisible = posts.isNotEmpty()
            
            if (posts.isEmpty()) {
                Log.d("HomeFragment", "No posts found, showing empty view")
            }
        }
    }

    private fun showDateRangePicker(post: com.example.dressit.data.model.Post) {
        val dialog = DateRangePickerDialog(
            requireContext(),
            post,
            onDateRangeSelected = { startDate, endDate, notes ->
                // יצירת הזמנה חדשה
                val chartViewModel = ViewModelProvider(requireActivity())[com.example.dressit.ui.chart.ChartViewModel::class.java]
                chartViewModel.createBooking(post, startDate, endDate, notes)
                
                // הצגת הודעה למשתמש
                Snackbar.make(binding.root, "ההזמנה נוצרה בהצלחה", Snackbar.LENGTH_LONG).show()
            }
        )
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "onResume called - refreshing posts")
        
        // רענון פוסטים בכל פעם שחוזרים למסך
        viewModel.refreshPosts()
        
        // רענון נוסף לאחר חצי שניה (לתת זמן לפוסטים להתעדכן בשרת)
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("HomeFragment", "Delayed refresh for posts after returning to screen")
            viewModel.refreshPosts()
        }, 500)
    }
} 