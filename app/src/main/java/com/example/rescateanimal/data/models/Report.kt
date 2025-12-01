package com.example.rescateanimal

data class Report(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // "danger", "lost", "abandoned"
    val description: String = "",
    val phone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val photoUrls: List<String> = listOf(),
    var status: String = "pending", // "pending", "in_progress", "resolved" - var para permitir cambios
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)