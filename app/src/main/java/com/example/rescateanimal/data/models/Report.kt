package com.example.rescateanimal

data class Report(
    var id: String = "",
    val userId: String = "",
    val type: String = "", // "danger", "lost", "abandoned"
    val description: String = "",
    val phone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val photoUrls: List<String> = listOf(),
    val status: String = "pending", // "pending", "in_progress", "resolved"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)