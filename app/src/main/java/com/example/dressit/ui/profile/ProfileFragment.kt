package com.example.dressit.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.databinding.FragmentProfileBinding
import com.example.dressit.ui.home.PostAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

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

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                findNavController().navigate(
                    R.id.action_profile_to_post_detail,
                    Bundle().apply {
                        putString("postId", post.id)
                    }
                )
            },
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                findNavController().navigate(
                    R.id.action_profile_to_post_detail,
                    Bundle().apply {
                        putString("postId", post.id)
                    }
                )
            }
        )

        binding.postsRecyclerView.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit_profile)
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading && postAdapter.currentList.isEmpty()
            binding.swipeRefresh.isRefreshing = isLoading && postAdapter.currentList.isNotEmpty()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.nameText.text = user.username
            binding.emailText.text = user.email

            Glide.with(requireContext())
                .load(user.profilePicture)
                .circleCrop()
                .into(binding.profileImage)
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }

        viewModel.loggedOut.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut) {
                findNavController().navigate(R.id.action_profile_to_login)
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.btn_logout)
            .setMessage(R.string.dialog_logout_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                viewModel.logout()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 