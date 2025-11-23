package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Share button
        findViewById<TextView>(R.id.btnShare).setOnClickListener {
            shareAnimal()
        }

        // Contact shelter button
        findViewById<Button>(R.id.btnContactShelter).setOnClickListener {
            contactShelter()
        }
    }

    private fun loadAnimalData() {

        android.util.Log.d("AnimalDetail", "Animal: ${animal.name}")
        android.util.Log.d("AnimalDetail", "isVaccinated: ${animal.isVaccinated}")
        android.util.Log.d("AnimalDetail", "isSterilized: ${animal.isSterilized}")
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
                "${(distance * 1000).toInt()} metros de ti"
            } else {
                "${"%.1f".format(distance)} km de ti"
            }
            findViewById<TextView>(R.id.tvDistance).text = distanceText
            findViewById<TextView>(R.id.tvDistance).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.tvDistance).visibility = View.GONE
        }

        // Type
        val typeText = when (animal.type.lowercase()) {
            "perro" -> "🐕 Perro"
            "gato" -> "🐱 Gato"
            else -> "🐹 Otra mascota"
        }
        findViewById<TextView>(R.id.tvAnimalType).text = typeText

        // Health status
        val vaccineIcon = findViewById<LinearLayout>(R.id.iconVaccinated)
        val sterilizedIcon = findViewById<LinearLayout>(R.id.iconSterilized)

        // Health status - Usar isVaccinated e isSterilized
        if (animal.isVaccinated) {
            vaccineIcon.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvVaccinatedStatus).text = "Vacunado"
            findViewById<TextView>(R.id.tvVaccinatedStatus).setTextColor(getColor(R.color.success_color))
        } else {
            vaccineIcon.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvVaccinatedStatus).text = "No vacunado"
            findViewById<TextView>(R.id.tvVaccinatedStatus).setTextColor(getColor(R.color.warning_color))
        }

        if (animal.isSterilized) {
            sterilizedIcon.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvSterilizedStatus).text = "Esterilizado"
            findViewById<TextView>(R.id.tvSterilizedStatus).setTextColor(getColor(R.color.success_color))
        } else {
            sterilizedIcon.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvSterilizedStatus).text = "No esterilizado"
            findViewById<TextView>(R.id.tvSterilizedStatus).setTextColor(getColor(R.color.warning_color))
        }

        // Description
        findViewById<TextView>(R.id.tvDescription).text = if (animal.description.isNotEmpty()) {
            animal.description
        } else {
            "Sin descripción disponible"
        }

        // Shelter info - Por ahora ocultamos hasta tener esta data
        findViewById<LinearLayout>(R.id.layoutShelterInfo).visibility = View.GONE
    }

    private fun contactShelter() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para contactar", Toast.LENGTH_SHORT).show()
            // Redirigir al login
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        // Por ahora mostrar mensaje genérico
        // TODO: Implementar contacto real con el albergue
        showContactOptions()
    }

    private fun showContactOptions() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Contactar con el albergue")
        builder.setMessage("¿Estás interesado en adoptar a ${animal.name}?\n\nSe enviará tu información de contacto al albergue para que se comuniquen contigo.")
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
        // TODO: Implementar envío de solicitud a Firebase
        Toast.makeText(
            this,
            "¡Solicitud enviada! El albergue se pondrá en contacto contigo pronto",
            Toast.LENGTH_LONG
        ).show()

        // Opcional: Volver a la pantalla anterior
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
            
            Descarga RescateAnimal para más información sobre adopciones.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Adopta a ${animal.name}")
        startActivity(Intent.createChooser(shareIntent, "Compartir a ${animal.name}"))
    }
}