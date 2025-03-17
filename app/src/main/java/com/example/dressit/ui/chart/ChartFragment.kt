package com.example.dressit.ui.chart

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dressit.R
import com.example.dressit.data.model.BookingStatus
import com.example.dressit.databinding.FragmentChartBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChartFragment : Fragment() {

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ChartViewModel by viewModels()
    private lateinit var bookingAdapter: BookingAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("ChartFragment", "onViewCreated")
        
        setupRecyclerView()
        setupObservers()
        setupListeners()
        
        // רענון אקטיבי של הזמנות בכל פתיחת המסך
        Log.d("ChartFragment", "Force refresh bookings in onViewCreated")
        viewModel.refreshBookings()
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ChartFragment", "onResume - forcing refresh of bookings")
        
        // ביצוע ריענון אקטיבי בכל פעם שהמסך חוזר לקדמת התצוגה
        viewModel.refreshBookings()
        
        // ריענון נוסף לאחר שניה (מתן זמן להזמנות חדשות להישמר בשרת)
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("ChartFragment", "Delayed refresh of bookings")
            viewModel.refreshBookings()
        }, 1000)
    }
    
    private fun setupRecyclerView() {
        Log.d("ChartFragment", "Setting up RecyclerView")
        bookingAdapter = BookingAdapter(
            onBookingClick = { booking ->
                // ניווט לפוסט המתאים
                Log.d("ChartFragment", "Booking clicked: ${booking.id}, postId: ${booking.postId}")
                
                findNavController().navigate(
                    R.id.action_navigation_chart_to_post_detail_fragment,
                    Bundle().apply {
                        putString("postId", booking.postId)
                    }
                )
            },
            onApproveClick = { booking ->
                Log.d("ChartFragment", "Approve booking: ${booking.id}")
                showConfirmationDialog(
                    "אישור הזמנה",
                    "האם אתה בטוח שברצונך לאשר את ההזמנה?",
                    onConfirm = {
                        viewModel.updateBookingStatus(booking.id, BookingStatus.APPROVED)
                        Snackbar.make(binding.root, "ההזמנה אושרה בהצלחה", Snackbar.LENGTH_SHORT).show()
                    }
                )
            },
            onRejectClick = { booking ->
                Log.d("ChartFragment", "Reject booking: ${booking.id}")
                showConfirmationDialog(
                    "דחיית הזמנה",
                    "האם אתה בטוח שברצונך לדחות את ההזמנה?",
                    onConfirm = {
                        viewModel.updateBookingStatus(booking.id, BookingStatus.REJECTED)
                        Snackbar.make(binding.root, "ההזמנה נדחתה", Snackbar.LENGTH_SHORT).show()
                    }
                )
            },
            onCancelClick = { booking ->
                Log.d("ChartFragment", "Cancel booking: ${booking.id}")
                showConfirmationDialog(
                    "ביטול הזמנה",
                    "האם אתה בטוח שברצונך לבטל את ההזמנה?",
                    onConfirm = {
                        viewModel.updateBookingStatus(booking.id, BookingStatus.CANCELED)
                        Snackbar.make(binding.root, "ההזמנה בוטלה", Snackbar.LENGTH_SHORT).show()
                    }
                )
            }
        )
        
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookingAdapter
        }
    }
    
    private fun setupObservers() {
        Log.d("ChartFragment", "Setting up observers")
        
        viewModel.bookings.observe(viewLifecycleOwner) { bookings ->
            Log.d("ChartFragment", "Got ${bookings.size} bookings")
            bookingAdapter.submitList(bookings)
            
            // עדכון ההצגה של רשימה ריקה
            updateEmptyState(bookings.isEmpty())
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("ChartFragment", "Loading state: $isLoading")
            binding.swipeRefreshLayout.isRefreshing = isLoading
            
            // מציג את טעינת הפרוגרס כשטוען
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("ChartFragment", "Error: $it")
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyText.text = "אין לך הזמנות"
            binding.recyclerViewBookings.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerViewBookings.visibility = View.VISIBLE
        }
    }
    
    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("ChartFragment", "Swipe refresh triggered")
            viewModel.refreshBookings()
        }
    }
    
    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("אישור") { _, _ -> onConfirm() }
            .setNegativeButton("ביטול", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ChartFragment", "onDestroyView")
        _binding = null
    }
} 