package com.example.dressit.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dressit.databinding.FragmentProfilePostsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfilePostsFragment : Fragment() {
    private var _binding: FragmentProfilePostsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })
    private lateinit var postsAdapter: ProfilePostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observePosts()
    }

    private fun setupRecyclerView() {
        postsAdapter = ProfilePostsAdapter { post ->
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileToPostDetail(post.id)
            )
        }

        binding.postsRecyclerView.apply {
            adapter = postsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(true)
        }
    }

    private fun observePosts() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 