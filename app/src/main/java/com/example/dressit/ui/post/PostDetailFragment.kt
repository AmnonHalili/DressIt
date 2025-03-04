package com.example.dressit.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.databinding.FragmentPostDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class PostDetailFragment : Fragment() {
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private val args: PostDetailFragmentArgs by navArgs()

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

        setupObservers()
        viewModel.loadPost(args.postId)
    }

    private fun setupObservers() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                binding.apply {
                    tvTitle.text = post.title
                    tvDescription.text = post.description
                    tvLikes.text = post.likes.toString()
                    tvComments.text = "${post.comments.size} comments"

                    // Load post image
                    Glide.with(requireContext())
                        .load(post.imageUrl)
                        .into(ivPostImage)

                    // Show edit/delete buttons only if current user is the post owner
                    val isOwner = viewModel.isCurrentUserPostOwner(post)
                    btnLike.isVisible = !isOwner
                    btnComment.isVisible = !isOwner
                }
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
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
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_post_title)
            .setMessage(R.string.dialog_delete_post_message)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deletePost()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 