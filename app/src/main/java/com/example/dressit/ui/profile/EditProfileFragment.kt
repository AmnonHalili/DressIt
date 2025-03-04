package com.example.dressit.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.dressit.databinding.FragmentEditProfileBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setProfileImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
        viewModel.loadUserProfile()
    }

    private fun setupClickListeners() {
        binding.btnChangePhoto.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            viewModel.updateProfile(name)
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnSave.isEnabled = !isLoading
            binding.btnChangePhoto.isEnabled = !isLoading
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etName.setText(it.username)
                binding.etEmail.setText(it.email)

                Glide.with(requireContext())
                    .load(it.profilePicture)
                    .circleCrop()
                    .into(binding.ivProfileImage)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.profileUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (isUpdated) {
                findNavController().navigateUp()
            }
        }

        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            uri?.let {
                Glide.with(requireContext())
                    .load(it)
                    .circleCrop()
                    .into(binding.ivProfileImage)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 