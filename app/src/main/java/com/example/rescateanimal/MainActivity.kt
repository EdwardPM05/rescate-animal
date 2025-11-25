package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
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
import com.example.rescateanimal.data.models.Animal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NavigationHelper(this).setupBottomNavigation()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        loadUserData()
        checkAffiliateStatus()
        loadNewAffiliates()
        loadLostAnimalsReports()
        loadRecentAdoptionAnimals()

        // Setup navigation
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

        // Click en foto de perfil -> ir a ProfileActivity
        ivProfileHeader.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        // Cargar nombre del usuario
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val displayName = document.getString("displayName") ?: currentUser.email?.split("@")?.get(0) ?: "Usuario"
                tvWelcomeUser.text = "Hola, $displayName!"
            }

        // Cargar foto de perfil
        db.collection("user_profiles").document(currentUser.uid)
            .get()
            .addOnSuccessListener { profileDoc ->
                val photoUrl = profileDoc.getString("profilePhotoUrl")
                loadProfileImage(photoUrl)
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
                    // NO TIENE NEGOCIO - Mostrar banner de afiliación
                    showAffiliateBanner()
                } else {
                    val affiliateDoc = documents.documents[0]
                    val status = affiliateDoc.getString("status")
                    val type = affiliateDoc.getString("type")

                    when {
                        // VETERINARIA O ALBERGUE APROBADO - Mostrar "Registra Mascotas"
                        status == "approved" && (type == "veterinaria" || type == "albergue") -> {
                            showRegisterPetsBanner()
                        }

                        // TIENDA U OTRO NEGOCIO - Ocultar toda la sección
                        status == "approved" -> {
                            hideAffiliateSection()
                        }

                        // PENDIENTE O RECHAZADO - Ocultar por ahora
                        else -> {
                            hideAffiliateSection()
                        }
                    }
                }
            }
            .addOnFailureListener {
                // En caso de error, mostrar banner por defecto
                showAffiliateBanner()
            }
    }

    // Mostrar banner para afiliar negocio
    private fun showAffiliateBanner() {
        sectionAffiliateTitle.text = "Afilia tu Local"
        sectionAffiliateTitle.visibility = View.VISIBLE
        bannerAffiliateYourBusiness.visibility = View.VISIBLE
        bannerImage.setImageResource(R.drawable.banner_affiliate)

        bannerAffiliateYourBusiness.setOnClickListener {
            startActivity(Intent(this, AffiliateActivity::class.java))
        }
    }

    // Mostrar banner para registrar mascotas
    private fun showRegisterPetsBanner() {
        sectionAffiliateTitle.text = "Registra Mascotas"
        sectionAffiliateTitle.visibility = View.VISIBLE
        bannerAffiliateYourBusiness.visibility = View.VISIBLE
        bannerImage.setImageResource(R.drawable.banner_register_pets)

        bannerAffiliateYourBusiness.setOnClickListener {
            startActivity(Intent(this, RegisterPetActivity::class.java))
        }
    }

    // Ocultar toda la sección
    private fun hideAffiliateSection() {
        sectionAffiliateTitle.visibility = View.GONE
        bannerAffiliateYourBusiness.visibility = View.GONE
    }

    // ========== SECCIÓN 1: NUEVAS CLÍNICAS Y ALBERGUES ==========
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
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val documentId = document.id
                    val type = document.getString("type") ?: ""
                    val businessName = document.getString("businessName") ?: "Negocio"
                    val address = document.getString("address") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0

                    val cardView = createAffiliateCard(documentId, type, businessName, address, phone, latitude, longitude)
                    containerAffiliates.addView(cardView)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar afiliados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createAffiliateCard(
        documentId: String,
        type: String,
        businessName: String,
        address: String,
        phone: String,
        lat: Double,
        lng: Double
    ): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_affiliate_card, containerAffiliates, false)

        val ivPhoto = view.findViewById<ImageView>(R.id.ivAffiliatePhoto)
        val tvName = view.findViewById<TextView>(R.id.tvAffiliateName)
        val tvType = view.findViewById<TextView>(R.id.tvAffiliateType)
        val tvAddress = view.findViewById<TextView>(R.id.tvAffiliateAddress)

        tvName.text = businessName
        tvType.text = getAffiliateTypeText(type)
        tvAddress.text = address

        // Cargar la primera foto del afiliado desde Firebase Storage
        loadAffiliateFirstPhoto(documentId, ivPhoto)

        // Click -> Abrir mapa en esa ubicación
        view.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("latitude", lat)
            intent.putExtra("longitude", lng)
            intent.putExtra("businessName", businessName)
            startActivity(intent)
        }

        return view
    }

    private fun loadAffiliateFirstPhoto(documentId: String, imageView: ImageView) {
        val storage = FirebaseStorage.getInstance()
        val affiliateRef = storage.reference.child("affiliates/$documentId/photos")

        affiliateRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    // Tomar la primera foto
                    listResult.items[0].downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(this)
                            .load(uri.toString())
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .centerCrop()
                            .into(imageView)
                    }.addOnFailureListener {
                        imageView.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else {
                    // Si no hay fotos, usar placeholder
                    imageView.setImageResource(R.drawable.ic_image_placeholder)
                }
            }
            .addOnFailureListener {
                imageView.setImageResource(R.drawable.ic_image_placeholder)
            }
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
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val description = document.getString("description") ?: "Animal perdido"
                    val location = document.getString("location") ?: "Ubicación desconocida"
                    val photoUrls = (document.get("photoUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val photoUrl = photoUrls.firstOrNull() ?: ""
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0

                    // Extraer tipo de animal de la descripción
                    val animalType = extractAnimalType(description)

                    val cardView = createLostAnimalCard(animalType, location, photoUrl, latitude, longitude)
                    containerLostAnimals.addView(cardView)
                }
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

        // Click -> Abrir mapa en ubicación del reporte
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
            }
    }

    private fun createAdoptionAnimalCard(animal: Animal): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_adoption_animal_card, containerAdoptionAnimals, false)

        val ivPhoto = view.findViewById<ImageView>(R.id.ivAdoptionAnimalPhoto)
        val tvName = view.findViewById<TextView>(R.id.tvAdoptionAnimalName)
        val tvInfo = view.findViewById<TextView>(R.id.tvAdoptionAnimalInfo)

        tvName.text = animal.name

        // Construir información adicional (tipo + edad + raza)
        val info = buildString {
            // Tipo de animal (Perro, Gato, etc.)
            if (animal.type.isNotEmpty()) {
                append(animal.type)
            }

            // Edad
            if (animal.age.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(animal.age)
            }

            // Raza
            if (animal.breed.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(animal.breed)
            }
        }

        tvInfo.text = if (info.isNotEmpty()) info else "Mascota en adopción"

        // Cargar foto
        Glide.with(this)
            .load(animal.photoUrl)
            .placeholder(R.drawable.placeholder_pet)
            .error(R.drawable.placeholder_pet)
            .centerCrop()
            .into(ivPhoto)

        // Click -> Abrir detalle del animal
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