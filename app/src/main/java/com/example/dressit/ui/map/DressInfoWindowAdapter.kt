package com.example.dressit.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.dressit.R
import com.example.dressit.data.model.Post
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

/**
 * מחלקה מותאמת אישית להצגת חלון מידע של שמלות במפה
 */
class DressInfoWindowAdapter(private val context: Context, private val posts: Map<String, Post>) : GoogleMap.InfoWindowAdapter {
    
    override fun getInfoWindow(marker: Marker): View? {
        // השתמש בברירת המחדל של חלון המידע
        return null
    }
    
    override fun getInfoContents(marker: Marker): View {
        val view = LayoutInflater.from(context).inflate(R.layout.map_marker_info_window, null)
        
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val snippetTextView = view.findViewById<TextView>(R.id.snippet)
        val imageView = view.findViewById<ImageView>(R.id.dress_image)
        val priceTextView = view.findViewById<TextView>(R.id.price)
        
        // קבל את מזהה הפוסט מתוך התגית של המרקר
        val postId = marker.tag as String
        val post = posts[postId]
        
        // הגדר את התוכן של חלון המידע
        titleTextView.text = marker.title
        snippetTextView.text = marker.snippet
        
        // הצג את המחיר בנפרד אם קיים
        if (post?.rentalPrice != null && post.rentalPrice > 0) {
            priceTextView.visibility = View.VISIBLE
            priceTextView.text = "₪${post.rentalPrice.toInt()}"
        } else {
            priceTextView.visibility = View.GONE
        }
        
        // טען את התמונה הממוזערת של השמלה באמצעות Glide
        post?.let {
            if (it.imageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(it.imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .centerCrop()
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.bg_image_placeholder)
            }
        } ?: run {
            imageView.setImageResource(R.drawable.bg_image_placeholder)
        }
        
        return view
    }
} 