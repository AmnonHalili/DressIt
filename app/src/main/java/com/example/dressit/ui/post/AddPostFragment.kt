package com.example.dressit.ui.post

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.databinding.FragmentAddPostBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.*
import android.os.Looper

@AndroidEntryPoint
class AddPostFragment : Fragment() {
    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddPostViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Snackbar.make(
                binding.root,
                "הרשאת מיקום נדרשת לשמירת מיקום השמלה",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    companion object {
        private const val TAG = "AddPostFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupTextChangeListeners()
        setupClickListeners()
        setupObservers()
        
        // בקשת מיקום אוטומטית עם פתיחת המסך
        checkLocationPermission()
    }

    private fun setupTextChangeListeners() {
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

        // Initially disable post button
        binding.postButton.isEnabled = false
    }

    private fun validateForm() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val priceText = binding.priceEditText.text.toString().trim()
        
        val isValid = title.length >= 3 && 
                      description.length >= 10 && 
                      priceText.isNotEmpty() &&
                      viewModel.imageSelected.value == true
        
        binding.postButton.isEnabled = isValid
    }

    private fun setupClickListeners() {
        binding.uploadImageButton.setOnClickListener {
            openImagePicker()
        }

        binding.postImage.setOnClickListener {
            openImagePicker()
        }

        binding.postButton.setOnClickListener {
            if (isNetworkAvailable()) {
                val title = binding.titleEditText.text.toString().trim()
                val description = binding.descriptionEditText.text.toString().trim()
                val priceStr = binding.priceEditText.text.toString().trim()
                val price = if (priceStr.isNotEmpty()) priceStr.toDouble() else 0.0
                
                viewModel.createPost(title, description, price)
            } else {
                Snackbar.make(binding.root, "אין חיבור לאינטרנט. וודא שאתה מחובר ונסה שוב", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.viewLocationButton.setOnClickListener {
            val location = viewModel.currentLocation
            if (location != null) {
                openLocationOnMap(location.latitude, location.longitude)
            }
        }

        binding.refreshLocationButton.setOnClickListener {
            checkLocationPermission()
        }
        
        // הוספת מאזין לכפתור בחירת מיקום ידנית
        binding.chooseLocationButton.setOnClickListener {
            showManualLocationDialog()
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.postButton.isEnabled = !isLoading
            binding.titleEditText.isEnabled = !isLoading
            binding.descriptionEditText.isEnabled = !isLoading
            binding.priceEditText.isEnabled = !isLoading
            binding.postImage.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") {
                        // Clear the error
                        viewModel.clearError()
                        // Check network connectivity
                        if (isNetworkAvailable()) {
                            // Retry the last action
                            binding.postButton.performClick()
                        } else {
                            Snackbar.make(binding.root, "No internet connection", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
        }

        viewModel.postCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                findNavController().navigate(R.id.action_add_post_to_home)
                Snackbar.make(requireActivity().findViewById(android.R.id.content), 
                    "הפוסט פורסם בהצלחה!", Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.imageSelected.observe(viewLifecycleOwner) { isSelected ->
            validateForm()
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
        pickImageLauncher.launch(intent)
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
                    "הרשאת מיקום נדרשת כדי לשמור את מיקום השמלה",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("אישור") {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(
                binding.root,
                "נדרשת הרשאת מיקום כדי לשמור את מיקום השמלה",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        // הצגת סטטוס עדכון מיקום
        binding.locationStatus.text = "מקבל מיקום נוכחי..."

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.setLocation(location)
                    updateLocationUI(location)
                } else {
                    // אם המיקום האחרון אינו זמין, בקש עדכון מיקום חדש
                    requestNewLocationData()
                }
            }
            .addOnFailureListener { e ->
                binding.locationStatus.text = "שגיאה בקבלת מיקום: ${e.message}"
                Log.e(TAG, "Error getting location", e)
            }
    }

    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .setMaxUpdates(1)
            .build()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val lastLocation = locationResult.lastLocation
                        if (lastLocation != null) {
                            viewModel.setLocation(lastLocation)
                            updateLocationUI(lastLocation)
                        } else {
                            binding.locationStatus.text = "לא ניתן לקבל מיקום. נסה שוב מאוחר יותר"
                        }
                    }
                },
                Looper.myLooper()
            )
        }
    }

    private fun updateLocationUI(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressLine = when {
                    address.thoroughfare != null -> "${address.thoroughfare}, ${address.locality ?: address.adminArea ?: ""}"
                    address.locality != null -> address.locality
                    else -> "${location.latitude.format(4)}, ${location.longitude.format(4)}"
                }
                binding.locationStatus.text = "המיקום הנוכחי: $addressLine"
            } else {
                binding.locationStatus.text = "המיקום הנוכחי: ${location.latitude.format(4)}, ${location.longitude.format(4)}"
            }
            // הפעלת כפתור הצגת מיקום במפה
            binding.viewLocationButton.isEnabled = true
        } catch (e: IOException) {
            binding.locationStatus.text = "המיקום הנוכחי: ${location.latitude.format(4)}, ${location.longitude.format(4)}"
            binding.viewLocationButton.isEnabled = true
            Log.e(TAG, "Error getting address from location", e)
        }
    }

    private fun openLocationOnMap(latitude: Double, longitude: Double) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        // בדיקה אם קיימת אפליקציית מפות מתאימה
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // חלופה במקרה שאין אפליקציית מפות
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }

    private fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)

    // פונקציה להצגת דיאלוג להזנת מיקום ידנית
    private fun showManualLocationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_location, null)
        val addressEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.address_edit_text)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("הזנת מיקום ידנית")
            .setView(dialogView)
            .setPositiveButton("חפש") { _, _ ->
                val addressText = addressEditText.text.toString().trim()
                if (addressText.isNotEmpty()) {
                    searchLocationByAddress(addressText)
                }
            }
            .setNegativeButton("ביטול", null)
            .create()
        
        dialog.show()
    }
    
    // פונקציה לחיפוש מיקום לפי כתובת
    private fun searchLocationByAddress(address: String) {
        binding.locationStatus.text = "מחפש את הכתובת..."
        
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            // רצים בתהליכון נפרד כדי לא לחסום את ה-UI
            Thread {
                try {
                    val addresses = geocoder.getFromLocationName(address, 1)
                    
                    // חזרה ל-UI thread
                    activity?.runOnUiThread {
                        if (addresses != null && addresses.isNotEmpty()) {
                            val location = Location("manual-input").apply {
                                latitude = addresses[0].latitude
                                longitude = addresses[0].longitude
                            }
                            
                            // עדכון המיקום ב-ViewModel
                            viewModel.setLocation(location)
                            
                            // עדכון ממשק המשתמש
                            updateLocationUI(location)
                            
                            Snackbar.make(binding.root, "מיקום נמצא!", Snackbar.LENGTH_SHORT).show()
                        } else {
                            binding.locationStatus.text = "לא ניתן למצוא את הכתובת. נסה שנית"
                            Snackbar.make(binding.root, "לא נמצאה כתובת. נסה להיות ספציפי יותר", Snackbar.LENGTH_LONG).show()
                        }
                    }
                } catch (e: IOException) {
                    activity?.runOnUiThread {
                        binding.locationStatus.text = "שגיאה בחיפוש כתובת"
                        Snackbar.make(binding.root, "שגיאה בחיפוש: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }.start()
        } catch (e: Exception) {
            binding.locationStatus.text = "שגיאה בחיפוש כתובת"
            Snackbar.make(binding.root, "שגיאה: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 