package com.example.rescateanimal

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val fullName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)