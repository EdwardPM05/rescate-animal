package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        val vIconBackground: View = itemView.findViewById(R.id.vIconBackground)
        val ivPetIcon: ImageView = itemView.findViewById(R.id.ivPetIcon)
        val ivPetTypeIcon: ImageView = itemView.findViewById(R.id.ivPetTypeIcon)
        val tvPetName: TextView = itemView.findViewById(R.id.tvPetName)
        val tvPetInfo: TextView = itemView.findViewById(R.id.tvPetInfo)
        val tvPetDate: TextView = itemView.findViewById(R.id.tvPetDate)
        val tvPetLocation: TextView = itemView.findViewById(R.id.tvPetLocation)
        val llStatusBadge: LinearLayout = itemView.findViewById(R.id.llStatusBadge)
        val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        val tvPetStatus: TextView = itemView.findViewById(R.id.tvPetStatus)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteAdoption)

        fun bind(animal: Animal) {
            // Pet name
            tvPetName.text = animal.name.ifEmpty { "Sin nombre" }

            // Load photo or show icon
            if (animal.photoUrl.isNotEmpty()) {
                // Show photo
                ivPetPhoto.visibility = View.VISIBLE
                vIconBackground.visibility = View.GONE
                ivPetIcon.visibility = View.GONE

                android.util.Log.d("MyAdoptionsAdapter", "Cargando foto: ${animal.photoUrl}")

                Glide.with(itemView.context)
                    .load(animal.photoUrl)
                    .transform(CenterCrop(), RoundedCorners(24))
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .into(ivPetPhoto)
            } else {
                // Show icon
                ivPetPhoto.visibility = View.GONE
                vIconBackground.visibility = View.VISIBLE
                ivPetIcon.visibility = View.VISIBLE
            }

            // Set icon based on pet type
            when (animal.type.lowercase()) {
                "perro", "dog" -> {
                    ivPetIcon.setImageResource(R.drawable.ic_perro)
                    ivPetTypeIcon.setImageResource(R.drawable.ic_perro)
                    vIconBackground.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.dog_light)
                }
                "gato", "cat" -> {
                    ivPetIcon.setImageResource(R.drawable.ic_gato)
                    ivPetTypeIcon.setImageResource(R.drawable.ic_gato)
                    vIconBackground.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.cat_light)
                }
                else -> {
                    ivPetIcon.setImageResource(R.drawable.ic_otros)
                    ivPetTypeIcon.setImageResource(R.drawable.ic_otros)
                    vIconBackground.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.other_light)
                }
            }

            // Pet info (type, breed, age, size)
            val infoText = buildString {
                append(animal.type.replaceFirstChar { it.uppercase() })
                if (animal.breed.isNotEmpty()) {
                    append(" • ${animal.breed}")
                }
                if (animal.age.isNotEmpty()) {
                    append(" • ${animal.age}")
                }
                if (animal.size.isNotEmpty()) {
                    append(" • ${getSizeText(animal.size)}")
                }
            }
            tvPetInfo.text = infoText

            // Format date
            tvPetDate.text = formatDate(animal.createdAt)

            // Location (first part of address)
            val locationText = animal.location.split(",").take(2).joinToString(", ").ifEmpty { "Ubicación no disponible" }
            tvPetLocation.text = locationText

            // Status
            when (animal.status) {
                "available" -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_check)
                    tvPetStatus.text = "Disponible"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_available)
                }
                "adopted" -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_adoptado)
                    tvPetStatus.text = "Adoptado"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_adopted)
                }
                "pending" -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_pending)
                    tvPetStatus.text = "Pendiente"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_pending)
                }
                else -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_check)
                    tvPetStatus.text = "Disponible"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_available)
                }
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

    private fun getSizeText(size: String): String {
        return when (size.lowercase()) {
            "small", "pequeño" -> "Pequeño"
            "medium", "mediano" -> "Mediano"
            "large", "grande" -> "Grande"
            else -> size
        }
    }
}