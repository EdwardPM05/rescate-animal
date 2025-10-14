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
    private var startAdoption: Boolean = false

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
        startAdoption = intent.getBooleanExtra("startAdoption", false)

        setupUI()
        loadAnimalData()

        if (startAdoption) {
            // Auto-scroll o mostrar diálogo de adopción
            showAdoptionDialog()
        }
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

        // Adopt button
        findViewById<Button>(R.id.btnAdoptNow).setOnClickListener {
            showAdoptionDialog()
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
        val typeText = when (animal.type) {
            "perro" -> "🐕 Perro"
            "gato" -> "🐈 Gato"
            else -> "🐾 Otra mascota"
        }
        findViewById<TextView>(R.id.tvAnimalType).text = typeText

        // Health status
        val vaccineIcon = findViewById<LinearLayout>(R.id.iconVaccinated)
        val sterilizedIcon = findViewById<LinearLayout>(R.id.iconSterilized)

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
        findViewById<TextView>(R.id.tvDescription).text = animal.description

        // Shelter info
        if (animal.shelterName.isNotEmpty()) {
            findViewById<TextView>(R.id.tvShelterName).text = animal.shelterName
            findViewById<LinearLayout>(R.id.layoutShelterInfo).visibility = View.VISIBLE
        } else {
            findViewById<LinearLayout>(R.id.layoutShelterInfo).visibility = View.GONE
        }
    }

    private fun showAdoptionDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Solicitar Adopción")
        builder.setMessage("¿Estás seguro que quieres solicitar la adopción de ${animal.name}?\n\nSe contactará al albergue con tus datos.")
        builder.setPositiveButton("Sí, adoptar") { dialog, _ ->
            requestAdoption()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun requestAdoption() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para adoptar", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Create adoption request in Firebase
        // For now, just contact the shelter
        Toast.makeText(
            this,
            "¡Solicitud enviada! El albergue se pondrá en contacto contigo",
            Toast.LENGTH_LONG
        ).show()

        // Optionally contact shelter directly
        contactShelter()
    }

    private fun contactShelter() {
        if (animal.shelterPhone.isEmpty()) {
            Toast.makeText(this, "No hay teléfono de contacto disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Open WhatsApp or phone dialer
        val message = "Hola, estoy interesado en adoptar a ${animal.name}. ¿Podemos hablar?"
        val encodedMessage = Uri.encode(message)

        // Try WhatsApp first
        try {
            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            val phone = animal.shelterPhone.replace("+", "").replace(" ", "")
            whatsappIntent.data = Uri.parse("https://wa.me/$phone?text=$encodedMessage")
            startActivity(whatsappIntent)
        } catch (e: Exception) {
            // Fallback to phone dialer
            try {
                val phoneIntent = Intent(Intent.ACTION_DIAL)
                phoneIntent.data = Uri.parse("tel:${animal.shelterPhone}")
                startActivity(phoneIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se puede abrir la aplicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareAnimal() {
        val shareText = """
            ¡Ayuda a ${animal.name} a encontrar un hogar!
            
            🐾 ${animal.breed}
            📍 ${animal.location}
            ⏰ ${animal.age}
            
            ${animal.description}
            
            Descarga RescateAnimal para más información.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, "Compartir ${animal.name}"))
    }
}