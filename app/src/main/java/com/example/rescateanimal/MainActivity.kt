package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

// --- Asegúrate que todas estas clases existan en tu proyecto ---
import com.example.rescateanimal.data.models.Animal
import com.example.rescateanimal.LoginActivity
import com.example.rescateanimal.ProfileActivity
import com.example.rescateanimal.MapActivity
import com.example.rescateanimal.AnimalDetailActivity
import com.example.rescateanimal.NavigationHelper
import com.example.rescateanimal.RegisterPetActivity
import com.example.rescateanimal.AffiliateActivity
import android.location.Location
import com.example.rescateanimal.LocationPickerActivity // Necesaria para el Intent

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var navigationHelper: NavigationHelper

    private lateinit var ivProfileHeader: ImageView
    private lateinit var tvWelcomeUser: TextView
    private lateinit var containerAffiliates: LinearLayout
    private lateinit var containerLostAnimals: LinearLayout
    private lateinit var containerAdoptionAnimals: LinearLayout
    private lateinit var bannerAffiliateYourBusiness: CardView
    private lateinit var sectionAffiliateTitle: TextView
    private lateinit var bannerImage: ImageView

    // VARIABLES PARA LOADING (Placeholder animado)
    private lateinit var loadingPlaceholder: View
    private lateinit var contentScrollView: View
    private lateinit var ivAnimalLoading: ImageView

    private var tasksCompleted = 0
    private val totalTasks = 5 // UserData, AffiliateStatus, NewAffiliates, LostAnimals, AdoptionAnimals

    // LISTA DE RECURSOS PARA EL CARRUSEL DE CARGA (Usando tus imágenes)
    private val animalPlaceholders = listOf(
        R.drawable.loading_perro_real,
        R.drawable.loading_gato_real,
        R.drawable.loading_hamster_real
    )
    private var currentAnimalIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NavigationHelper(this).setupBottomNavigation()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()

        // 1. Iniciar las 5 tareas de carga (todas asíncronas)
        loadUserData()
        checkAffiliateStatus()
        loadNewAffiliates() // OPTIMIZADO
        loadLostAnimalsReports() // Se optimizará cuando RegisterPetActivity y ReportActivity se corrijan
        loadRecentAdoptionAnimals() // Se optimizará cuando RegisterPetActivity y ReportActivity se corrijan

        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun initializeViews() {
        ivProfileHeader = findViewById(R.id.ivProfileHeader)
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
        containerAffiliates = findViewById(R.id.containerAffiliates)
        containerLostAnimals = findViewById(R.id.containerLostAnimals)
        containerAdoptionAnimals = findViewById(R.id.containerAdoptionAnimals)
        bannerAffiliateYourBusiness = findViewById(R.id.bannerAffiliateYourBusiness)
        sectionAffiliateTitle = findViewById(R.id.sectionAffiliateTitle)
        bannerImage = findViewById(R.id.bannerImage)

        // VISTAS DE LOADING
        loadingPlaceholder = findViewById(R.id.loadingPlaceholder)
        contentScrollView = findViewById(R.id.contentScrollView)
        ivAnimalLoading = findViewById(R.id.ivAnimalLoading)

        // Iniciar el carrusel de perritos
        startLoadingAnimation()

        ivProfileHeader.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    // =======================================================
    // LÓGICA DE CARGA Y ANIMACIÓN (Placeholder)
    // =======================================================

    private fun startLoadingAnimation() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                // Rota las imágenes de animales (carrusel)
                currentAnimalIndex = (currentAnimalIndex + 1) % animalPlaceholders.size
                ivAnimalLoading.setImageResource(animalPlaceholders[currentAnimalIndex])

                if (tasksCompleted < totalTasks) {
                    handler.postDelayed(this, 800) // Cambia cada 0.8 segundos
                }
            }
        }
        handler.post(runnable)
    }

    private fun taskCompleted() {
        tasksCompleted++
        // Si las 5 tareas completaron, ocultar el placeholder y mostrar el contenido
        if (tasksCompleted >= totalTasks) {
            showContent()
        }
    }

    private fun showContent() {
        // Ocultar placeholder con fade-out
        loadingPlaceholder.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                loadingPlaceholder.visibility = View.GONE
            }

        // Mostrar contenido principal con fade-in
        contentScrollView.alpha = 0f
        contentScrollView.visibility = View.VISIBLE
        contentScrollView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    // =======================================================
    // FUNCIONES DE CARGA DE DATOS (Cada función llama a taskCompleted)
    // =======================================================

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val displayName = document.getString("displayName") ?: currentUser.email?.split("@")?.get(0) ?: "Usuario"
                tvWelcomeUser.text = "Hola, $displayName!"

                db.collection("user_profiles").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { profileDoc ->
                        val photoUrl = profileDoc.getString("profilePhotoUrl")
                        loadProfileImage(photoUrl)
                        taskCompleted() // Tarea 1 completada
                    }
                    .addOnFailureListener {
                        taskCompleted() // Tarea 1 completada
                    }
            }
            .addOnFailureListener {
                taskCompleted() // Tarea 1 completada
            }
    }

    private fun loadProfileImage(photoUrl: String?) {
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                )
                .into(ivProfileHeader)
        }
    }

    private fun checkAffiliateStatus() {
        val currentUser = auth.currentUser ?: return

        db.collection("affiliates")
            .whereEqualTo("userId", currentUser.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showAffiliateBanner()
                } else {
                    val affiliateDoc = documents.documents[0]
                    val status = affiliateDoc.getString("status")
                    val type = affiliateDoc.getString("type")

                    when {
                        status == "approved" && (type == "veterinaria" || type == "albergue") -> {
                            showRegisterPetsBanner()
                        }
                        status == "approved" -> {
                            hideAffiliateSection()
                        }
                        else -> {
                            hideAffiliateSection()
                        }
                    }
                }
                taskCompleted() // Tarea 2 completada
            }
            .addOnFailureListener {
                showAffiliateBanner()
                taskCompleted() // Tarea 2 completada
            }
    }

    private fun showAffiliateBanner() {
        sectionAffiliateTitle.text = "Afilia tu Local"
        sectionAffiliateTitle.visibility = View.VISIBLE
        bannerAffiliateYourBusiness.visibility = View.VISIBLE
        bannerImage.setImageResource(R.drawable.banner_affiliate)

        bannerAffiliateYourBusiness.setOnClickListener {
            startActivity(Intent(this, AffiliateActivity::class.java))
        }
    }

    private fun showRegisterPetsBanner() {
        sectionAffiliateTitle.text = "Registra Mascotas"
        sectionAffiliateTitle.visibility = View.VISIBLE
        bannerAffiliateYourBusiness.visibility = View.VISIBLE
        bannerImage.setImageResource(R.drawable.banner_register_pets)

        bannerAffiliateYourBusiness.setOnClickListener {
            startActivity(Intent(this, RegisterPetActivity::class.java))
        }
    }

    private fun hideAffiliateSection() {
        sectionAffiliateTitle.visibility = View.GONE
        bannerAffiliateYourBusiness.visibility = View.GONE
    }

    // ========== SECCIÓN 1: NUEVAS CLÍNICAS Y ALBERGUES (OPTIMIZADA) ==========
    private fun loadNewAffiliates() {
        db.collection("affiliates")
            .whereEqualTo("status", "approved")
            .whereEqualTo("verified", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                containerAffiliates.removeAllViews()

                if (documents.isEmpty) {
                    showEmptyState(containerAffiliates, "No hay negocios nuevos")
                    taskCompleted() // Tarea 3 completada
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val documentId = document.id
                    val type = document.getString("type") ?: ""
                    val businessName = document.getString("businessName") ?: "Negocio"
                    val address = document.getString("address") ?: ""
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0

                    // ⚠️ CLAVE: Lee el campo mainPhotoUrl. Si es null o no existe, será "" y Glide usará el placeholder.
                    // Si tu amigo agregó otro campo, debes cambiar "mainPhotoUrl" al nombre de ese campo.
                    val photoUrl = document.getString("mainPhotoUrl") ?: ""

                    val cardView = createAffiliateCard(documentId, type, businessName, address, latitude, longitude, photoUrl)
                    containerAffiliates.addView(cardView)
                }
                taskCompleted() // Tarea 3 completada
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar afiliados: ${e.message}", Toast.LENGTH_SHORT).show()
                taskCompleted() // Tarea 3 completada
            }
    }

    private fun createAffiliateCard(
        documentId: String,
        type: String,
        businessName: String,
        address: String,
        lat: Double,
        lng: Double,
        photoUrl: String // Campo añadido para la URL
    ): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_affiliate_card, containerAffiliates, false)

        val ivPhoto = view.findViewById<ImageView>(R.id.ivAffiliatePhoto)
        val tvName = view.findViewById<TextView>(R.id.tvAffiliateName)
        val tvType = view.findViewById<TextView>(R.id.tvAffiliateType)
        val tvAddress = view.findViewById<TextView>(R.id.tvAffiliateAddress)

        tvName.text = businessName
        tvType.text = getAffiliateTypeText(type)
        tvAddress.text = address

        // Carga de imagen con Placeholder: Si photoUrl es inválido/vacío, usará ic_image_placeholder
        Glide.with(this)
            .load(photoUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder) // Asegúrate que esta imagen exista
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
            )
            .into(ivPhoto)

        view.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("latitude", lat)
            intent.putExtra("longitude", lng)
            intent.putExtra("businessName", businessName)
            startActivity(intent)
        }

        return view
    }

    private fun getAffiliateTypeText(type: String): String {
        return when (type) {
            "veterinaria" -> "🏥 Veterinaria"
            "tienda" -> "🛍️ Tienda"
            "albergue" -> "🏠 Albergue"
            else -> "📍 Negocio"
        }
    }

    // ========== SECCIÓN 2: ANIMALES PERDIDOS (URGENTE) ==========
    private fun loadLostAnimalsReports() {
        db.collection("reports")
            .whereEqualTo("reportType", "lost")
            .whereEqualTo("status", "pending")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                containerLostAnimals.removeAllViews()

                if (documents.isEmpty) {
                    showEmptyState(containerLostAnimals, "No hay animales perdidos reportados")
                    taskCompleted() // Tarea 4 completada
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val description = document.getString("description") ?: "Animal perdido"
                    val location = document.getString("location") ?: "Ubicación desconocida"
                    val photoUrls = (document.get("photoUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val photoUrl = photoUrls.firstOrNull() ?: ""
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0

                    val animalType = extractAnimalType(description)

                    val cardView = createLostAnimalCard(animalType, location, photoUrl, latitude, longitude)
                    containerLostAnimals.addView(cardView)
                }
                taskCompleted() // Tarea 4 completada
            }
            .addOnFailureListener {
                taskCompleted() // Tarea 4 completada
            }
    }

    private fun extractAnimalType(description: String): String {
        val lower = description.lowercase()
        return when {
            lower.contains("perro") || lower.contains("dog") -> "Perro"
            lower.contains("gato") || lower.contains("cat") -> "Gato"
            else -> "Mascota"
        }
    }

    private fun createLostAnimalCard(animalType: String, location: String, photoUrl: String, lat: Double, lng: Double): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_lost_animal_card, containerLostAnimals, false)

        val ivPhoto = view.findViewById<ImageView>(R.id.ivLostAnimalPhoto)
        val tvAnimalName = view.findViewById<TextView>(R.id.tvLostAnimalName)
        val tvLocation = view.findViewById<TextView>(R.id.tvLostAnimalLocation)

        tvAnimalName.text = "$animalType Perdido"
        tvLocation.text = location

        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.placeholder_pet)
                .error(R.drawable.placeholder_pet)
                .centerCrop()
                .into(ivPhoto)
        }

        view.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("latitude", lat)
            intent.putExtra("longitude", lng)
            intent.putExtra("reportType", "lost")
            startActivity(intent)
        }

        return view
    }

    // ========== SECCIÓN 3: RECIÉN EN ADOPCIÓN ==========
    private fun loadRecentAdoptionAnimals() {
        db.collection("animals")
            .whereEqualTo("status", "available")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                containerAdoptionAnimals.removeAllViews()

                if (documents.isEmpty) {
                    showEmptyState(containerAdoptionAnimals, "No hay mascotas en adopción")
                    taskCompleted() // Tarea 5 completada
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    try {
                        val animal = document.toObject(Animal::class.java).copy(id = document.id)
                        val cardView = createAdoptionAnimalCard(animal)
                        containerAdoptionAnimals.addView(cardView)
                    } catch (e: Exception) {
                        // Skip malformed documents
                    }
                }
                taskCompleted() // Tarea 5 completada
            }
            .addOnFailureListener {
                taskCompleted() // Tarea 5 completada
            }
    }

    private fun createAdoptionAnimalCard(animal: Animal): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_adoption_animal_card, containerAdoptionAnimals, false)

        val ivPhoto = view.findViewById<ImageView>(R.id.ivAdoptionAnimalPhoto)
        val tvName = view.findViewById<TextView>(R.id.tvAdoptionAnimalName)
        val tvInfo = view.findViewById<TextView>(R.id.tvAdoptionAnimalInfo)

        tvName.text = animal.name

        val info = buildString {
            if (animal.type.isNotEmpty()) {
                append(animal.type)
            }
            if (animal.age.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(animal.age)
            }
            if (animal.breed.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(animal.breed)
            }
        }

        tvInfo.text = if (info.isNotEmpty()) info else "Mascota en adopción"

        Glide.with(this)
            .load(animal.photoUrl)
            .placeholder(R.drawable.placeholder_pet)
            .error(R.drawable.placeholder_pet)
            .centerCrop()
            .into(ivPhoto)

        view.setOnClickListener {
            val intent = Intent(this, AnimalDetailActivity::class.java)
            intent.putExtra("animal", animal)
            startActivity(intent)
        }

        return view
    }

    private fun showEmptyState(container: LinearLayout, message: String) {
        val textView = TextView(this)
        textView.text = message
        textView.setTextColor(getColor(R.color.text_secondary))
        textView.textSize = 14f
        textView.setPadding(16, 32, 16, 32)
        container.addView(textView)
    }
}