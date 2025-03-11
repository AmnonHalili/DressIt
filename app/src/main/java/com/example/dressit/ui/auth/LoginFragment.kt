package com.example.dressit.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dressit.R
import com.example.dressit.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LoginFragment", "Fragment created")
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            
            Log.d("LoginFragment", "Login button clicked with email: $email")
            
            if (email.isBlank() || password.isBlank()) {
                Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                Log.d("LoginFragment", "Login validation failed: Empty fields")
                return@setOnClickListener
            }
            
            viewModel.login(email, password)
        }

        binding.registerButton.setOnClickListener {
            Log.d("LoginFragment", "Register button clicked")
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("LoginFragment", "Loading state changed: $isLoading")
            binding.progressBar.isVisible = isLoading
            binding.loginButton.isEnabled = !isLoading
            binding.registerButton.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("LoginFragment", "Error observed: $it")
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.loginSuccess.observe(viewLifecycleOwner) {
            Log.d("LoginFragment", "Login success observed")
            findNavController().navigate(R.id.action_login_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 