package com.example.dressit.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dressit.R
import com.example.dressit.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableMyLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as? SupportMapFragment
            if (mapFragment != null) {
                mapFragment.getMapAsync(this)
            } else {
                Log.e("MapFragment", "Map fragment is null, trying to find in parent")
                // Try to get from parent fragmentManager as a fallback
                val parentMapFragment = fragmentManager?.findFragmentById(R.id.mapView) as? SupportMapFragment
                parentMapFragment?.getMapAsync(this)
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Error setting up map: ${e.message}")
            Snackbar.make(
                binding.root,
                "שגיאה בטעינת המפה, אנא נסה שוב מאוחר יותר",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        setupClickListeners()
        observeViewModel()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        setupMap()
        
        // הגדרת מאזינים לאירועי לחיצה על המפה
        googleMap?.setOnInfoWindowClickListener(this)
        googleMap?.setOnMarkerClickListener(this)
        
        // צפייה במיפוי הפוסטים כדי להגדיר את מתאם החלון המותאם אישית
        viewModel.postMap.observe(viewLifecycleOwner) { postMap ->
            // הגדרת מתאם מותאם אישית לחלון המידע
            if (postMap.isNotEmpty()) {
                googleMap?.setInfoWindowAdapter(DressInfoWindowAdapter(requireContext(), postMap))
            }
        }
    }

    private fun setupMap() {
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isMapToolbarEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.myLocationFab.setOnClickListener {
            checkLocationPermission()
        }
        
        binding.refreshFab.setOnClickListener {
            viewModel.refreshPosts()
            Snackbar.make(binding.root, "מרענן פוסטים...", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            googleMap?.clear()
            posts.forEach { post ->
                if (post.latitude != null && post.longitude != null) {
                    val position = LatLng(post.latitude, post.longitude)
                    
                    // יצירת מחרוזת המחיר עם סמל השקל בצבע ירוק
                    val priceText = if (post.rentalPrice > 0) {
                        "₪${post.rentalPrice.toInt()}"
                    } else {
                        ""
                    }
                    
                    // כותרת המרקר תהיה שם המוצר (במקום שם המשתמש)
                    val marker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(post.title)
                            .snippet("${post.userName} • ${priceText}")
                    )
                    marker?.tag = post.id
                }
            }
            
            // אם זה הטעינה הראשונה של הנתונים, נמקם את המצלמה במיקום שמכיל את כל המרקרים
            if (posts.isNotEmpty()) {
                val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                var hasValidLocation = false
                
                posts.forEach { post ->
                    if (post.latitude != null && post.longitude != null) {
                        builder.include(LatLng(post.latitude, post.longitude))
                        hasValidLocation = true
                    }
                }
                
                if (hasValidLocation) {
                    val bounds = builder.build()
                    // הזזת המצלמה לאחר שהמפה כבר מאותחלת
                    googleMap?.setOnMapLoadedCallback {
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    }
                }
            }
        }
    }

    // מיושם כאשר לוחצים על חלון המידע של המרקר
    override fun onInfoWindowClick(marker: Marker) {
        navigateToPostDetail(marker.tag as String)
    }
    
    // מיושם כאשר לוחצים על מרקר
    override fun onMarkerClick(marker: Marker): Boolean {
        // הצגת חלון המידע
        marker.showInfoWindow()
        // החזרת false כדי שגם ברירת המחדל תקרה (הזזת המצלמה למרקר)
        return false
    }
    
    // פונקציה לניווט לדף פרטי הפוסט
    private fun navigateToPostDetail(postId: String) {
        val action = MapFragmentDirections.actionMapFragmentToPostDetailFragment(postId)
        findNavController().navigate(action)
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    binding.root,
                    "אישור מיקום נדרש כדי להציג את מיקומך הנוכחי",
                    Snackbar.LENGTH_LONG
                ).setAction("אשר") {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 