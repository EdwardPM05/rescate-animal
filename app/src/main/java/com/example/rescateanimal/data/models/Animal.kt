package com.example.rescateanimal.data.models

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Animal(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val breed: String = "",
    val age: String = "",
    val size: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUrl: String = "",
    val status: String = "available",
    @get:PropertyName("isVaccinated")
    @set:PropertyName("isVaccinated")
    var isVaccinated: Boolean = false,
    @get:PropertyName("isSterilized")
    @set:PropertyName("isSterilized")
    var isSterilized: Boolean = false,
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
    val distance: Float
) : Parcelable