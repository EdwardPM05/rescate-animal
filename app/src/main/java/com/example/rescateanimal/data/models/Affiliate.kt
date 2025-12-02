package com.example.rescateanimal.data.models

data class Affiliate(
    val id: String = "",
    val businessName: String = "",
    val type: String = "", // veterinaria, tienda, albergue
    val description: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val contactPerson: String = "", // Nombre de la persona de contacto
    val userEmail: String = "",
    val userId: String = "",
    val mainPhotoUrl: String = "",
    // URLs de fotos organizadas por categoría
    val photosUrls: List<String> = emptyList(),        // Fotos del negocio (carpeta photos/)
    val licenseUrls: List<String> = emptyList(),       // Licencias del negocio (carpeta license/)
    val staffLicensesUrls: List<String> = emptyList(), // Licencias del personal (carpeta staff_licenses/)

    val status: String = "pending", // pending, approved, rejected
    val verified: Boolean = false, // Si está verificado o no
    val createdAt: Long = 0L,
    val hours: String = "",
    val socialMedia: String = ""
)