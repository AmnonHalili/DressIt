package com.example.dressit.data.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val profilePicture: String = "",
    val createdAt: Long = System.currentTimeMillis()
) 