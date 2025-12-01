package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.rescateanimal.data.models.Animal
import com.google.firebase.auth.FirebaseAuth

class AnimalDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var animal: Animal
    private var distance: Float = -1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animal_detail)

        auth = FirebaseAuth.getInstance()

        // Get animal data from intent
        animal = intent.getParcelableExtra("animal") ?: run {
            Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        distance = intent.getFloatExtra("distance", -1f)

        setupUI()
        loadAnimalData()
    }

    private fun setupUI() {
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Share button
        findViewById<ImageView>(R.id.btnShare).setOnClickListener {
            shareAnimal()
        }

        // Contact shelter button
        findViewById<Button>(R.id.btnContactShelter).setOnClickListener {
            contactShelter()
        }
    }

    private fun loadAnimalData() {
        // Load photo
        Glide.with(this)
            .load(animal.photoUrl)
            .placeholder(R.drawable.placeholder_pet)
            .error(R.drawable.placeholder_pet)
            .into(findViewById<ImageView>(R.id.ivAnimalPhoto))

        // Basic info
        findViewById<TextView>(R.id.tvAnimalName).text = animal.name
        findViewById<TextView>(R.id.tvAnimalBreed).text = animal.breed
        findViewById<TextView>(R.id.tvAnimalAge).text = animal.age
        findViewById<TextView>(R.id.tvAnimalSize).text = animal.size.capitalize()
        findViewById<TextView>(R.id.tvAnimalLocation).text = animal.location

        // Distance
        if (distance >= 0) {
            val distanceText = if (distance < 1) {
                "${(distance * 1000).toInt()} m de ti"
            } else {
                "${"%.1f".format(distance)} km de ti"
            }
            findViewById<TextView>(R.id.tvDistance).text = distanceText
            findViewById<TextView>(R.id.tvDistance).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.tvDistance).visibility = View.GONE
        }

        // Set animal type icon
        val ivAnimalTypeIcon = findViewById<ImageView>(R.id.ivAnimalTypeIcon)
        when (animal.type.lowercase()) {
            "perro" -> ivAnimalTypeIcon.setImageResource(R.drawable.ic_perro)
            "gato" -> ivAnimalTypeIcon.setImageResource(R.drawable.ic_gato)
            else -> ivAnimalTypeIcon.setImageResource(R.drawable.ic_otros)
        }

        // Health status
        val vaccineIcon = findViewById<android.widget.LinearLayout>(R.id.iconVaccinated)
        val sterilizedIcon = findViewById<android.widget.LinearLayout>(R.id.iconSterilized)
        val tvVaccinatedStatus = findViewById<TextView>(R.id.tvVaccinatedStatus)
        val tvSterilizedStatus = findViewById<TextView>(R.id.tvSterilizedStatus)

        // Vacunación
        if (animal.isVaccinated) {
            tvVaccinatedStatus.text = "Vacunado"
            tvVaccinatedStatus.setTextColor(getColor(R.color.success_color))
        } else {
            tvVaccinatedStatus.text = "No vacunado"
            tvVaccinatedStatus.setTextColor(getColor(R.color.warning_color))
        }

        // Esterilización
        if (animal.isSterilized) {
            tvSterilizedStatus.text = "Esterilizado"
            tvSterilizedStatus.setTextColor(getColor(R.color.success_color))
        } else {
            tvSterilizedStatus.text = "No esterilizado"
            tvSterilizedStatus.setTextColor(getColor(R.color.warning_color))
        }

        // Description
        findViewById<TextView>(R.id.tvDescription).text = if (animal.description.isNotEmpty()) {
            animal.description
        } else {
            "Sin descripción disponible"
        }

        // Shelter/Vet info
        setupShelterInfo()
    }

    private fun setupShelterInfo() {
        val cardShelterInfo = findViewById<CardView>(R.id.cardShelterInfo)
        val ivShelterIcon = findViewById<ImageView>(R.id.ivShelterIcon)
        val tvShelterLabel = findViewById<TextView>(R.id.tvShelterLabel)
        val tvShelterName = findViewById<TextView>(R.id.tvShelterName)

        // Determinar si es veterinaria o albergue basándose en el nombre
        val shelterName = animal.shelterName ?: ""
        val isVet = shelterName.contains("veterinaria", ignoreCase = true) ||
                shelterName.contains("clínica", ignoreCase = true) ||
                shelterName.contains("vet", ignoreCase = true)

        if (isVet) {
            // Usar icono de veterinaria (asumiendo que tienes ic_vet.png)
            ivShelterIcon.setImageResource(R.drawable.ic_vet)
            tvShelterLabel.text = "Veterinaria"
        } else {
            // Usar icono de albergue/refugio
            ivShelterIcon.setImageResource(R.drawable.ic_rescue)
            tvShelterLabel.text = "Albergue"
        }

        // Mostrar nombre del albergue/veterinaria si existe
        if (shelterName.isNotEmpty()) {
            tvShelterName.text = shelterName
            cardShelterInfo.visibility = View.VISIBLE
        } else {
            cardShelterInfo.visibility = View.GONE
        }
    }

    private fun contactShelter() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para contactar", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        showContactOptions()
    }

    private fun showContactOptions() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Contactar")
        builder.setMessage("¿Estás interesado en adoptar a ${animal.name}?\n\nSe enviará tu información de contacto al albergue.")
        builder.setPositiveButton("Sí, estoy interesado") { dialog, _ ->
            sendAdoptionRequest()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun sendAdoptionRequest() {
        Toast.makeText(
            this,
            "¡Solicitud enviada! El albergue se pondrá en contacto contigo pronto",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun shareAnimal() {
        val shareText = """
            ¡Ayuda a ${animal.name} a encontrar un hogar!
            
            🐾 Raza: ${animal.breed}
            📍 Ubicación: ${animal.location}
            ⏰ Edad: ${animal.age}
            📏 Tamaño: ${animal.size}
            
            ${animal.description}
            
            Descarga RescateAnimal para más información.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Adopta a ${animal.name}")
        startActivity(Intent.createChooser(shareIntent, "Compartir a ${animal.name}"))
    }
}