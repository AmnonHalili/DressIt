package com.example.dressit.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dressit.R
import com.example.dressit.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val username = binding.nameEditText.text.toString()
            
            if (email.isBlank() || password.isBlank() || confirmPassword.isBlank() || username.isBlank()) {
                Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Snackbar.make(binding.root, "Passwords do not match", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!binding.termsCheckbox.isChecked) {
                Snackbar.make(binding.root, "Please accept the terms and conditions", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.register(email, password, username)
        }

        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.registerButton.isEnabled = !isLoading
            binding.loginButton.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.registerSuccess.observe(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_register_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 