package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescateanimal.data.models.Animal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdoptionDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var animalId: String

    private lateinit var loadingState: LinearLayout
    private lateinit var contentState: ScrollView
    private lateinit var tvPetName: TextView
    private lateinit var tvPetSpecies: TextView
    private lateinit var tvPetBreed: TextView
    private lateinit var tvPetAge: TextView
    private lateinit var tvPetSize: TextView
    private lateinit var tvPetDate: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvVaccinated: TextView
    private lateinit var tvSterilized: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvPhone: TextView
    private lateinit var rvPhotos: RecyclerView
    private lateinit var btnDelete: Button
    private lateinit var btnCallPhone: TextView
    private lateinit var btnWhatsApp: TextView

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
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        try {
            loadingState = findViewById(R.id.loadingState)
            contentState = findViewById(R.id.contentState)

            tvPetName = findViewById(R.id.tvPetName)
            tvPetSpecies = findViewById(R.id.tvPetSpecies)
            tvPetBreed = findViewById(R.id.tvPetBreed)
            tvPetAge = findViewById(R.id.tvPetAge)
            tvPetSize = findViewById(R.id.tvPetSize)
            tvPetDate = findViewById(R.id.tvPetDate)
            tvDescription = findViewById(R.id.tvDescription)
            tvVaccinated = findViewById(R.id.tvVaccinated)
            tvSterilized = findViewById(R.id.tvSterilized)
            tvLocation = findViewById(R.id.tvLocation)
            tvPhone = findViewById(R.id.tvPhone)
            rvPhotos = findViewById(R.id.rvPhotos)
            btnDelete = findViewById(R.id.btnDelete)
            btnCallPhone = findViewById(R.id.btnCallPhone)
            btnWhatsApp = findViewById(R.id.btnWhatsApp)

            // Setup RecyclerView for photos
            rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

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
        // Basic Info
        tvPetName.text = animal.name
        tvPetSpecies.text = "${getSpeciesEmoji(animal.type)} ${animal.type.capitalize()}"
        tvPetBreed.text = animal.breed.ifEmpty { "No especificada" }
        tvPetAge.text = animal.age.ifEmpty { "No especificada" }
        tvPetSize.text = when (animal.size.lowercase()) {
            "small", "pequeño" -> "Pequeño"
            "medium", "mediano" -> "Mediano"
            "large", "grande" -> "Grande"
            else -> "No especificado"
        }

        // Date
        tvPetDate.text = formatDate(animal.createdAt)

        // Description
        tvDescription.text = animal.description.ifEmpty { "Sin descripción" }

        // Health Info
        tvVaccinated.text = if (animal.isVaccinated) "✅ Sí" else "❌ No"
        tvSterilized.text = if (animal.isSterilized) "✅ Sí" else "❌ No"

        // Location
        tvLocation.text = animal.location.ifEmpty { "Ubicación no disponible" }

        // Phone
        tvPhone.text = animal.shelterPhone.ifEmpty { "No disponible" }

        btnCallPhone.setOnClickListener {
            if (animal.shelterPhone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${animal.shelterPhone}"))
                startActivity(intent)
            }
        }

        btnWhatsApp.setOnClickListener {
            if (animal.shelterPhone.isNotEmpty()) {
                val phone = animal.shelterPhone.replace("+", "").replace(" ", "")
                val message = "Hola, estoy interesado en adoptar a ${animal.name}"
                val uri = "https://wa.me/$phone?text=${Uri.encode(message)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Photos
        if (animal.photoUrl.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNoPhotos).visibility = View.GONE
            rvPhotos.visibility = View.VISIBLE

            try {
                val adapter = AnimalPhotosAdapter(listOf(animal.photoUrl))
                rvPhotos.adapter = adapter
            } catch (e: Exception) {
                android.util.Log.e("AdoptionDetailActivity", "Error loading photos: ${e.message}")
                findViewById<TextView>(R.id.tvNoPhotos).visibility = View.VISIBLE
                rvPhotos.visibility = View.GONE
            }
        } else {
            findViewById<TextView>(R.id.tvNoPhotos).visibility = View.VISIBLE
            rvPhotos.visibility = View.GONE
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
        val format = java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy 'a las' HH:mm", java.util.Locale("es", "ES"))
        return format.format(date)
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

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        contentState.visibility = View.GONE
    }

    private fun showContent() {
        loadingState.visibility = View.GONE
        contentState.visibility = View.VISIBLE
    }
}