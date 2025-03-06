package com.example.dressit.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val savedBy: List<String> = emptyList(),
    val rentalPrice: Double = 0.0,
    val currency: String = "ILS"
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
) 