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
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var profilePagerAdapter: ProfilePagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        viewModel.loadUserProfile()
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

    private fun setupViews() {
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
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.apply {
                    topUsername.text = it.username
                    username.text = it.username
                    bio.text = it.bio

                    Glide.with(requireContext())
                        .load(it.profilePicture)
                        .placeholder(R.drawable.profile_placeholder)
                        .circleCrop()
                        .into(profileImage)
                }
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.apply {
                postsCount.text = stats.postsCount.toString()
                followersCount.text = formatCount(stats.followersCount)
                followingCount.text = stats.followingCount.toString()
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

    private fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
            count >= 1000 -> String.format("%.1fK", count / 1000.0)
            else -> count.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 