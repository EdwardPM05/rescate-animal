package com.example.rescateanimal.data.models

import java.io.Serializable

data class Affiliate(
    val id: String = "",
    val businessName: String = "",
    val type: String = "", // veterinaria, tienda, albergue
    val address: String = "",
    val phone: String = "",
    val contactPerson: String = "",
    val description: String = "",
    val hours: String = "",
    val socialMedia: String = "",
    val mainPhotoUrl: String = "",
    val licenseUrl: String = "",
    val staffLicenseUrl: String = "",
    val status: String = "", // pending, approved, rejected
    val userEmail: String = "",
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val verified: Boolean = false // Campo clave para el funcionamiento del partner
) : Serializable