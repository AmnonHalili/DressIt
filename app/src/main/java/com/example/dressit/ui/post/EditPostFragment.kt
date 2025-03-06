package com.example.dressit.ui.post

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.databinding.FragmentEditPostBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditPostFragment : Fragment() {
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditPostViewModel by viewModels()
    private val args: EditPostFragmentArgs by navArgs()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        checkLocationPermission()
        setupViews()
        setupClickListeners()
        setupObservers()
        
        // טעינת הפוסט לעריכה
        viewModel.loadPost(args.postId)
    }

    private fun setupViews() {
        // Add text change listeners for real-time validation
        binding.titleEditText.doAfterTextChanged { text ->
            validateForm()
        }

        binding.descriptionEditText.doAfterTextChanged { text ->
            validateForm()
        }
        
        binding.priceEditText.doAfterTextChanged { text ->
            validateForm()
        }

        // Initially disable update button
        binding.updateButton.isEnabled = false
    }

    private fun validateForm() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val priceText = binding.priceEditText.text.toString().trim()
        
        val isValid = title.length >= 3 && 
                      description.length >= 10 && 
                      priceText.isNotEmpty()
        
        binding.updateButton.isEnabled = isValid
    }

    private fun setupClickListeners() {
        binding.uploadImageButton.setOnClickListener {
            openImagePicker()
        }

        binding.postImage.setOnClickListener {
            openImagePicker()
        }

        binding.updateButton.setOnClickListener {
            if (!isNetworkAvailable()) {
                Snackbar.make(binding.root, "אנא בדוק את חיבור האינטרנט שלך", Snackbar.LENGTH_LONG)
                    .setAction("בדוק שוב") {
                        if (isNetworkAvailable()) {
                            updatePost()
                        } else {
                            Snackbar.make(binding.root, "עדיין אין חיבור לאינטרנט", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .show()
                return@setOnClickListener
            }

            updatePost()
        }
        
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun updatePost() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val priceText = binding.priceEditText.text.toString().trim()
        
        try {
            val price = priceText.toDoubleOrNull() ?: 0.0
            viewModel.updatePost(title, description, price)
        } catch (e: NumberFormatException) {
            Snackbar.make(binding.root, "אנא הזן מחיר תקין", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                // מילוי הטופס בנתוני הפוסט הקיים
                binding.titleEditText.setText(post.title)
                binding.descriptionEditText.setText(post.description)
                binding.priceEditText.setText(post.rentalPrice.toString())
                
                // טעינת התמונה
                Glide.with(requireContext())
                    .load(post.imageUrl)
                    .centerCrop()
                    .into(binding.postImage)
                
                validateForm()
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.updateButton.isEnabled = !isLoading
            binding.cancelButton.isEnabled = !isLoading
            binding.titleEditText.isEnabled = !isLoading
            binding.descriptionEditText.isEnabled = !isLoading
            binding.priceEditText.isEnabled = !isLoading
            binding.postImage.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") {
                        viewModel.clearError()
                        if (isNetworkAvailable()) {
                            binding.updateButton.performClick()
                        } else {
                            Snackbar.make(binding.root, "No internet connection", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
        }

        viewModel.postUpdated.observe(viewLifecycleOwner) { updated ->
            if (updated) {
                Snackbar.make(binding.root, "הפוסט עודכן בהצלחה", Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.imageSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.uploadImageButton.text = if (isSelected) {
                getString(R.string.btn_change_image)
            } else {
                getString(R.string.btn_upload_image)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun handleSelectedImage(uri: Uri) {
        // Load image preview
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.postImage)

        viewModel.setImage(uri)
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    binding.root,
                    "Location permission is required to add location to your post",
                    Snackbar.LENGTH_LONG
                ).setAction("Grant") {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.setLocation(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 