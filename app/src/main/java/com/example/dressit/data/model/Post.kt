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
    val location: GeoPoint? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val userId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) 