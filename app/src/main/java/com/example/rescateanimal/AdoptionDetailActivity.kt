package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.rescateanimal.data.models.Animal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdoptionDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var animalId: String

    private lateinit var loadingState: LinearLayout
    private lateinit var contentState: ScrollView
    private lateinit var ivPetTypeIcon: ImageView
    private lateinit var tvPetName: TextView
    private lateinit var tvPetSubtitle: TextView
    private lateinit var llSpeciesBadge: LinearLayout
    private lateinit var ivSpeciesIcon: ImageView
    private lateinit var tvPetSpecies: TextView
    private lateinit var tvPetDate: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvPetBreed: TextView
    private lateinit var tvPetAge: TextView
    private lateinit var tvPetSize: TextView
    private lateinit var tvHealthStatus: TextView
    private lateinit var tvVaccinated: TextView
    private lateinit var tvSterilized: TextView
    private lateinit var tvLocation: TextView
    private lateinit var vpPhotos: ViewPager2
    private lateinit var tvPhotoCounter: TextView
    private lateinit var llNoPhotos: LinearLayout
    private lateinit var vPhotoOverlay: View
    private lateinit var btnDelete: LinearLayout

    private var currentAnimal: Animal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adoption_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        animalId = intent.getStringExtra("animalId") ?: ""
        if (animalId.isEmpty()) {
            Toast.makeText(this, "Error: ID de mascota no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadAnimalDetails()
    }

    private fun setupViews() {
        // Back Button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        try {
            loadingState = findViewById(R.id.loadingState)
            contentState = findViewById(R.id.contentState)

            ivPetTypeIcon = findViewById(R.id.ivPetTypeIcon)
            tvPetName = findViewById(R.id.tvPetName)
            tvPetSubtitle = findViewById(R.id.tvPetSubtitle)
            llSpeciesBadge = findViewById(R.id.llSpeciesBadge)
            ivSpeciesIcon = findViewById(R.id.ivSpeciesIcon)
            tvPetSpecies = findViewById(R.id.tvPetSpecies)
            tvPetDate = findViewById(R.id.tvPetDate)
            tvDescription = findViewById(R.id.tvDescription)
            tvPetBreed = findViewById(R.id.tvPetBreed)
            tvPetAge = findViewById(R.id.tvPetAge)
            tvPetSize = findViewById(R.id.tvPetSize)
            tvHealthStatus = findViewById(R.id.tvHealthStatus)
            tvVaccinated = findViewById(R.id.tvVaccinated)
            tvSterilized = findViewById(R.id.tvSterilized)
            tvLocation = findViewById(R.id.tvLocation)
            vpPhotos = findViewById(R.id.vpPhotos)
            tvPhotoCounter = findViewById(R.id.tvPhotoCounter)
            llNoPhotos = findViewById(R.id.llNoPhotos)
            vPhotoOverlay = findViewById(R.id.vPhotoOverlay)
            btnDelete = findViewById(R.id.btnDelete)

            btnDelete.setOnClickListener {
                showDeleteConfirmation()
            }
        } catch (e: Exception) {
            android.util.Log.e("AdoptionDetailActivity", "Error in setupViews: ${e.message}")
            Toast.makeText(this, "Error al configurar vistas: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadAnimalDetails() {
        showLoading()

        db.collection("animals").document(animalId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val animal = document.toObject(Animal::class.java)?.copy(id = document.id)
                        if (animal != null) {
                            currentAnimal = animal
                            displayAnimalDetails(animal)
                            showContent()
                        } else {
                            Toast.makeText(this, "Error al cargar detalles", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error al cargar detalles: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Mascota no encontrada", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayAnimalDetails(animal: Animal) {
        // Name and Type Icon
        tvPetName.text = animal.name
        tvPetSubtitle.text = "Buscando hogar"

        // Set icon based on species
        when (animal.type.lowercase()) {
            "perro", "dog" -> {
                ivPetTypeIcon.setImageResource(R.drawable.ic_perro)
                ivSpeciesIcon.setImageResource(R.drawable.ic_perro)
            }
            "gato", "cat" -> {
                ivPetTypeIcon.setImageResource(R.drawable.ic_gato)
                ivSpeciesIcon.setImageResource(R.drawable.ic_gato)
            }
            "ave", "bird" -> {
                ivPetTypeIcon.setImageResource(R.drawable.ic_bird)
                ivSpeciesIcon.setImageResource(R.drawable.ic_bird)
            }
            "conejo", "rabbit" -> {
                ivPetTypeIcon.setImageResource(R.drawable.ic_rabbit)
                ivSpeciesIcon.setImageResource(R.drawable.ic_rabbit)
            }
            else -> {
                ivPetTypeIcon.setImageResource(R.drawable.ic_otros)
                ivSpeciesIcon.setImageResource(R.drawable.ic_otros)
            }
        }

        // Species Badge
        tvPetSpecies.text = animal.type.capitalize()
        llSpeciesBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)

        // Date
        tvPetDate.text = formatDate(animal.createdAt)

        // Description
        tvDescription.text = animal.description.ifEmpty { "Sin descripción" }

        // Basic Info
        tvPetBreed.text = animal.breed.ifEmpty { "No especificada" }
        tvPetAge.text = animal.age.ifEmpty { "No especificada" }
        tvPetSize.text = when (animal.size.lowercase()) {
            "small", "pequeño" -> "Pequeño"
            "medium", "mediano" -> "Mediano"
            "large", "grande" -> "Grande"
            else -> "No especificado"
        }

        // Health Info
        tvHealthStatus.text = "Saludable"
        tvVaccinated.text = if (animal.isVaccinated) "Sí" else "No"
        tvSterilized.text = if (animal.isSterilized) "Sí" else "No"

        // Location
        tvLocation.text = animal.location.ifEmpty { "Ubicación no disponible" }

        // Photos - ViewPager2 igual que ReportDetail
        if (animal.photoUrl.isNotEmpty()) {
            llNoPhotos.visibility = View.GONE
            vpPhotos.visibility = View.VISIBLE
            vPhotoOverlay.visibility = View.VISIBLE

            // Since Animal has single photoUrl, create a list with it
            val photoUrls = listOf(animal.photoUrl)
            tvPhotoCounter.visibility = if (photoUrls.size > 1) View.VISIBLE else View.GONE

            try {
                val adapter = AnimalDetailPhotosAdapter(photoUrls)
                vpPhotos.adapter = adapter

                // Update counter on page change
                if (photoUrls.size > 1) {
                    tvPhotoCounter.text = "1/${photoUrls.size}"
                    vpPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            tvPhotoCounter.text = "${position + 1}/${photoUrls.size}"
                        }
                    })
                }
            } catch (e: Exception) {
                android.util.Log.e("AdoptionDetailActivity", "Error loading photos: ${e.message}")
                llNoPhotos.visibility = View.VISIBLE
                vpPhotos.visibility = View.GONE
                vPhotoOverlay.visibility = View.GONE
                tvPhotoCounter.visibility = View.GONE
            }
        } else {
            llNoPhotos.visibility = View.VISIBLE
            vpPhotos.visibility = View.GONE
            vPhotoOverlay.visibility = View.GONE
            tvPhotoCounter.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar publicación")
            .setMessage("¿Estás seguro de que deseas eliminar esta publicación de adopción? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAnimal()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAnimal() {
        db.collection("animals").document(animalId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Publicación eliminada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Fecha no disponible"

        val date = java.util.Date(timestamp)
        val now = java.util.Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> {
                val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "ES"))
                format.format(date)
            }
            days > 0 -> "Hace ${days.toInt()} día${if (days.toInt() > 1) "s" else ""}"
            hours > 0 -> "Hace ${hours.toInt()} hora${if (hours.toInt() > 1) "s" else ""}"
            minutes > 0 -> "Hace ${minutes.toInt()} minuto${if (minutes.toInt() > 1) "s" else ""}"
            else -> "Hace un momento"
        }
    }

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        contentState.visibility = View.GONE
    }

    private fun showContent() {
        loadingState.visibility = View.GONE
        contentState.visibility = View.VISIBLE
    }
}