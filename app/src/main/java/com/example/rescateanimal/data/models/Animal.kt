package com.example.rescateanimal.data.models

data class Animal(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "perro", "gato", "otro"
    val breed: String = "",
    val age: String = "", // "6 meses", "2 años", etc.
    val size: String = "", // "pequeño", "mediano", "grande"
    val gender: String = "", // "macho", "hembra"
    val description: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUrl: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val rescueCenter: String = "", // Nombre del centro de rescate
    val isVaccinated: Boolean = false,
    val isSterilized: Boolean = false,
    val isHealthy: Boolean = true,
    val specialNeeds: String = "",
    val personality: String = "", // "amigable", "juguetón", etc.
    val goodWithKids: Boolean = false,
    val goodWithPets: Boolean = false,
    val status: String = "available", // "available", "in_process", "adopted"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Para calcular distancias
data class AnimalWithDistance(
    val animal: Animal,
    val distance: Float // en kilómetros
)