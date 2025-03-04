package com.example.dressit.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.databinding.FragmentProfileBinding
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        viewModel.loadUserProfile()
    }

    private fun setupViews() {
        profilePagerAdapter = ProfilePagerAdapter(this)
        binding.viewPager.adapter = profilePagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Posts"
                1 -> "Videos"
                else -> "Tagged"
            }
            tab.setIcon(
                when (position) {
                    0 -> R.drawable.ic_grid
                    1 -> R.drawable.ic_video
                    else -> R.drawable.ic_tag
                }
            )
        }.attach()

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit_profile)
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.apply {
                    fullUsername.text = it.username
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