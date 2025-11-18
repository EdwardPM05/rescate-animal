package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rescateanimal.data.models.AnimalWithDistance

class AnimalsAdapter(
    private var animals: List<AnimalWithDistance>,
    private val onAnimalClick: (AnimalWithDistance) -> Unit
) : RecyclerView.Adapter<AnimalsAdapter.AnimalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_animal_card, parent, false)
        return AnimalViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        holder.bind(animals[position])
    }

    override fun getItemCount(): Int = animals.size

    fun updateAnimals(newAnimals: List<AnimalWithDistance>) {
        animals = newAnimals
        notifyDataSetChanged()
    }

    inner class AnimalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAnimalPhoto: ImageView = itemView.findViewById(R.id.ivAnimalPhoto)
        private val tvAnimalName: TextView = itemView.findViewById(R.id.tvAnimalName)
        private val tvAnimalType: TextView = itemView.findViewById(R.id.tvAnimalType)
        private val tvAnimalAge: TextView = itemView.findViewById(R.id.tvAnimalAge)
        private val tvAnimalBreed: TextView = itemView.findViewById(R.id.tvAnimalBreed)
        private val tvAnimalSize: TextView = itemView.findViewById(R.id.tvAnimalSize)
        private val tvAnimalLocation: TextView = itemView.findViewById(R.id.tvAnimalLocation)
        private val tvAnimalDistance: TextView = itemView.findViewById(R.id.tvAnimalDistance)
        private val layoutVaccinated: LinearLayout = itemView.findViewById(R.id.layoutVaccinated)
        private val layoutSterilized: LinearLayout = itemView.findViewById(R.id.layoutSterilized)
        private val btnViewMore: Button = itemView.findViewById(R.id.btnViewMore)

        fun bind(animalWithDistance: AnimalWithDistance) {
            val animal = animalWithDistance.animal

            // Load photo
            Glide.with(itemView.context)
                .load(animal.photoUrl)
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .centerCrop()
                .into(ivAnimalPhoto)

            // Set basic info
            tvAnimalName.text = animal.name
            tvAnimalAge.text = animal.age
            tvAnimalBreed.text = animal.breed
            tvAnimalSize.text = "Tamaño: ${animal.size}"
            tvAnimalLocation.text = animal.location

            // Set animal type with emoji
            tvAnimalType.text = when (animal.type.lowercase()) {
                "perro" -> "🐕 Perro"
                "gato" -> "🐱 Gato"
                else -> "🐹 Otro"
            }

            // Ocultar badges de salud por ahora (hasta que se agreguen al modelo)
            layoutVaccinated.visibility = View.GONE
            layoutSterilized.visibility = View.GONE

            // Distance
            if (animalWithDistance.distance >= 0) {
                tvAnimalDistance.visibility = View.VISIBLE
                tvAnimalDistance.text = if (animalWithDistance.distance < 1) {
                    "${(animalWithDistance.distance * 1000).toInt()} m"
                } else {
                    "${"%.1f".format(animalWithDistance.distance)} km"
                }
            } else {
                tvAnimalDistance.visibility = View.GONE
            }

            // Click handlers
            btnViewMore.setOnClickListener {
                onAnimalClick(animalWithDistance)
            }

            itemView.setOnClickListener {
                onAnimalClick(animalWithDistance)
            }
        }
    }
}