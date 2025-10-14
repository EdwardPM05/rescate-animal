package com.example.rescateanimal.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Animal(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "perro", "gato", "otro"
    val breed: String = "",
    val age: String = "",
    val size: String = "", // "pequeño", "mediano", "grande"
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUrl: String = "",
    val status: String = "available", // "available", "adopted", "reserved"
    val isVaccinated: Boolean = false,
    val isSterilized: Boolean = false,
    val description: String = "",
    val shelterId: String = "",
    val shelterName: String = "",
    val shelterPhone: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) : Parcelable

@Parcelize
data class AnimalWithDistance(
    val animal: Animal,
    val distance: Float // Distance in kilometers, -1 if unknown
) : Parcelable