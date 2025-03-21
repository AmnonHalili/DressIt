package com.example.dressit.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.data.model.User
import com.example.dressit.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var profilePagerAdapter: ProfilePagerAdapter
    
    private val args: ProfileFragmentArgs by navArgs()
    private var isCurrentUserProfile = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // בדיקה האם מדובר בפרופיל של המשתמש הנוכחי או של משתמש אחר
        val userId = args.userId
        if (userId.isNotEmpty() && userId != FirebaseAuth.getInstance().currentUser?.uid) {
            isCurrentUserProfile = false
            viewModel.loadUserProfile(userId)
        } else {
            isCurrentUserProfile = true
        }
        
        setHasOptionsMenu(isCurrentUserProfile)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTabs()
        setupObservers()
        
        // הצגת/הסתרת כפתורי העריכה אם זה פרופיל של משתמש אחר
        if (!isCurrentUserProfile) {
            binding.editProfileButton.visibility = View.GONE
            binding.settingsButton.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_force_refresh -> {
                showForceRefreshDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showForceRefreshDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("רענון מאולץ")
            .setMessage("פעולה זו תנקה את הפוסטים שלך מבסיס הנתונים המקומי ותטען אותם מחדש מהשרת. האם להמשיך?")
            .setPositiveButton("כן") { _, _ ->
                viewModel.forceRefreshUserPosts()
                Snackbar.make(binding.root, "מרענן נתונים מהשרת...", Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton("לא", null)
            .create()
        dialog.show()
    }

    private fun setupTabs() {
        profilePagerAdapter = ProfilePagerAdapter(this)
        binding.viewPager.adapter = profilePagerAdapter

        // הסתרת ה-TabLayout כיוון שיש רק לשונית אחת
        binding.tabLayout.visibility = View.GONE

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit_profile)
        }

        // עדכון כפתור שלוש הנקודות כדי שיציג תפריט
        binding.settingsButton.setOnClickListener { view ->
            showOptionsMenu(view)
        }
    }

    // פונקציה להצגת תפריט האפשרויות
    private fun showOptionsMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popup.inflate(R.menu.profile_options_menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    // התנתקות
                    viewModel.logout()
                    true
                }
                R.id.action_account_settings -> {
                    // פעולת הגדרות חשבון
                    Snackbar.make(binding.root, "הגדרות חשבון - פונקציונליות עתידית", Snackbar.LENGTH_LONG).show()
                    true
                }
                R.id.action_change_password -> {
                    // פעולת שינוי סיסמה
                    Snackbar.make(binding.root, "שינוי סיסמה - פונקציונליות עתידית", Snackbar.LENGTH_LONG).show()
                    true
                }
                R.id.action_privacy -> {
                    // פעולת פרטיות וביטחון
                    Snackbar.make(binding.root, "פרטיות וביטחון - פונקציונליות עתידית", Snackbar.LENGTH_LONG).show()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun setupObservers() {
        if (isCurrentUserProfile) {
            viewModel.currentUser.observe(viewLifecycleOwner) { user ->
                updateUI(user)
            }
        } else {
            viewModel.otherUser.observe(viewLifecycleOwner) { user ->
                updateUI(user)
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.apply {
                postsCount.text = stats.postsCount.toString()
            }
        }

        viewModel.loggedOut.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut) {
                findNavController().navigate(R.id.action_profile_to_login)
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUI(user: User) {
        binding.apply {
            topUsername.text = user.username
            username.text = user.username
            bio.text = user.bio

            Glide.with(requireContext())
                .load(user.profilePicture)
                .placeholder(R.drawable.profile_placeholder)
                .circleCrop()
                .into(profileImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 