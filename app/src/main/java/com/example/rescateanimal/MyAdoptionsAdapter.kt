package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.rescateanimal.data.models.Animal
import java.text.SimpleDateFormat
import java.util.*

class MyAdoptionsAdapter(
    private val animals: List<Animal>,
    private val onDeleteClick: (Animal) -> Unit,
    private val onItemClick: (Animal) -> Unit
) : RecyclerView.Adapter<MyAdoptionsAdapter.AdoptionViewHolder>() {

    inner class AdoptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPetPhoto: ImageView = itemView.findViewById(R.id.ivPetPhoto)
        val tvPetName: TextView = itemView.findViewById(R.id.tvPetName)
        val tvPetInfo: TextView = itemView.findViewById(R.id.tvPetInfo)
        val tvPetDate: TextView = itemView.findViewById(R.id.tvPetDate)
        val tvPetLocation: TextView = itemView.findViewById(R.id.tvPetLocation)
        val tvPetStatus: TextView = itemView.findViewById(R.id.tvPetStatus)
        val btnDelete: TextView = itemView.findViewById(R.id.btnDeleteAdoption)

        fun bind(animal: Animal) {
            // Pet name
            tvPetName.text = animal.name

            // Pet info (type, breed, age)
            val infoText = buildString {
                append(getSpeciesEmoji(animal.type))
                append(" ")
                append(animal.breed)
                if (animal.age.isNotEmpty()) {
                    append(" • ")
                    append(animal.age)
                }
                if (animal.size.isNotEmpty()) {
                    append(" • ")
                    append(getSizeText(animal.size))
                }
            }
            tvPetInfo.text = infoText

            // Format date
            tvPetDate.text = formatDate(animal.createdAt)

            // Location (first part of address)
            val locationText = animal.location.split(",").take(2).joinToString(", ").ifEmpty { "Ubicación no disponible" }
            tvPetLocation.text = "📍 $locationText"

            // Status
            tvPetStatus.text = "✅ Disponible"
            tvPetStatus.setBackgroundResource(R.drawable.status_badge_available)

            // Load pet photo
            if (animal.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(animal.photoUrl)
                    .transform(CenterCrop(), RoundedCorners(24))
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .into(ivPetPhoto)
            } else {
                ivPetPhoto.setImageResource(R.drawable.ic_pet_placeholder)
            }

            // Delete button
            btnDelete.setOnClickListener {
                onDeleteClick(animal)
            }

            // Item click
            itemView.setOnClickListener {
                onItemClick(animal)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdoptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_adoption, parent, false)
        return AdoptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdoptionViewHolder, position: Int) {
        holder.bind(animals[position])
    }

    override fun getItemCount() = animals.size

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Fecha no disponible"

        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> {
                SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(date)
            }
            days > 0 -> "Hace ${days.toInt()} día${if (days.toInt() > 1) "s" else ""}"
            hours > 0 -> "Hace ${hours.toInt()} hora${if (hours.toInt() > 1) "s" else ""}"
            minutes > 0 -> "Hace ${minutes.toInt()} minuto${if (minutes.toInt() > 1) "s" else ""}"
            else -> "Hace un momento"
        }
    }

    private fun getSpeciesEmoji(type: String): String {
        return when (type.lowercase()) {
            "perro", "dog" -> "🐕"
            "gato", "cat" -> "🐈"
            "ave", "bird" -> "🐦"
            "conejo", "rabbit" -> "🐰"
            else -> "🐾"
        }
    }

    private fun getSizeText(size: String): String {
        return when (size.lowercase()) {
            "small", "pequeño" -> "Pequeño"
            "medium", "mediano" -> "Mediano"
            "large", "grande" -> "Grande"
            else -> size
        }
    }
}