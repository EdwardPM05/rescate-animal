package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rescateanimal.data.models.AnimalWithDistance

class AnimalsAdapter(
    private var animals: List<AnimalWithDistance>,
    private val onAdoptClick: (AnimalWithDistance) -> Unit
) : RecyclerView.Adapter<AnimalsAdapter.AnimalViewHolder>() {

    class AnimalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAnimalPhoto: ImageView = itemView.findViewById(R.id.ivAnimalPhoto)
        val tvAnimalName: TextView = itemView.findViewById(R.id.tvAnimalName)
        val tvAnimalDetails: TextView = itemView.findViewById(R.id.tvAnimalDetails)
        val tvAnimalLocation: TextView = itemView.findViewById(R.id.tvAnimalLocation)
        val tvAnimalDistance: TextView = itemView.findViewById(R.id.tvAnimalDistance)
        val btnAdopt: Button = itemView.findViewById(R.id.btnAdopt)
        val btnViewMore: Button = itemView.findViewById(R.id.btnViewMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_animal_card, parent, false)
        return AnimalViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        val animalWithDistance = animals[position]
        val animal = animalWithDistance.animal
        val distance = animalWithDistance.distance

        // Animal name
        holder.tvAnimalName.text = animal.name

        // Animal details (breed, age)
        val details = buildString {
            if (animal.breed.isNotEmpty()) append("${animal.breed} ")
            if (animal.size.isNotEmpty()) append("${animal.size} ")
            if (animal.age.isNotEmpty()) append("• ${animal.age}")
        }
        holder.tvAnimalDetails.text = details.ifEmpty { "Información no disponible" }

        // Location
        holder.tvAnimalLocation.text = animal.location.ifEmpty { "Ubicación no especificada" }

        // Distance
        if (distance >= 0) {
            holder.tvAnimalDistance.text = "• ${String.format("%.1f", distance)} km"
            holder.tvAnimalDistance.visibility = View.VISIBLE
        } else {
            holder.tvAnimalDistance.visibility = View.GONE
        }

        // Animal photo
        if (animal.photoUrl.isNotEmpty()) {
            // Using Glide for image loading (you'll need to add Glide dependency)
            loadImageWithGlide(holder.ivAnimalPhoto, animal.photoUrl)
        } else {
            // Default image based on animal type
            val defaultImage = when (animal.type.lowercase()) {
                "perro" -> R.drawable.default_dog_image
                "gato" -> R.drawable.default_cat_image
                else -> R.drawable.default_pet_image
            }
            holder.ivAnimalPhoto.setImageResource(defaultImage)
        }

        // Button clicks
        holder.btnAdopt.setOnClickListener {
            onAdoptClick(animalWithDistance)
        }

        holder.btnViewMore.setOnClickListener {
            // TODO: Navigate to animal details screen
            // For now, show basic info
            showAnimalDetailsDialog(holder.itemView.context, animal)
        }

        // Card click
        holder.itemView.setOnClickListener {
            showAnimalDetailsDialog(holder.itemView.context, animal)
        }
    }

    override fun getItemCount(): Int = animals.size

    fun updateAnimals(newAnimals: List<AnimalWithDistance>) {
        animals = newAnimals
        notifyDataSetChanged()
    }

    private fun loadImageWithGlide(imageView: ImageView, url: String) {
        try {
            // If you have Glide dependency
            Glide.with(imageView.context)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.placeholder_animal)
                .error(R.drawable.placeholder_animal)
                .into(imageView)
        } catch (e: Exception) {
            // Fallback if Glide is not available
            imageView.setImageResource(R.drawable.placeholder_animal)
        }
    }

    private fun showAnimalDetailsDialog(context: android.content.Context, animal: com.example.rescateanimal.data.models.Animal) {
        val message = buildString {
            append("🐾 Nombre: ${animal.name}\n")
            if (animal.breed.isNotEmpty()) append("🎯 Raza: ${animal.breed}\n")
            if (animal.age.isNotEmpty()) append("📅 Edad: ${animal.age}\n")
            if (animal.size.isNotEmpty()) append("📏 Tamaño: ${animal.size}\n")
            if (animal.gender.isNotEmpty()) append("⚥ Sexo: ${animal.gender}\n")
            append("💉 Vacunado: ${if (animal.isVaccinated) "Sí" else "No"}\n")
            append("✂️ Esterilizado: ${if (animal.isSterilized) "Sí" else "No"}\n")
            if (animal.personality.isNotEmpty()) append("😊 Personalidad: ${animal.personality}\n")
            append("👶 Bueno con niños: ${if (animal.goodWithKids) "Sí" else "No"}\n")
            append("🐕 Bueno con mascotas: ${if (animal.goodWithPets) "Sí" else "No"}\n")
            if (animal.specialNeeds.isNotEmpty()) append("⚠️ Necesidades especiales: ${animal.specialNeeds}\n")
            if (animal.description.isNotEmpty()) append("\n📝 Descripción:\n${animal.description}\n")
            if (animal.rescueCenter.isNotEmpty()) append("\n🏠 Centro: ${animal.rescueCenter}")
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("Detalles de ${animal.name}")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Adoptar") { _, _ ->
                // TODO: Start adoption process
                android.widget.Toast.makeText(context, "Iniciando adopción de ${animal.name}", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}