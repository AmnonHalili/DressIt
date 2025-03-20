package com.example.dressit.data.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val profilePicture: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList()
) 