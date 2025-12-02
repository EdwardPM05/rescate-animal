package com.example.rescateanimal

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.rescateanimal.data.models.Affiliate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class AffiliateDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var loadingState: LinearLayout
    private lateinit var contentState: ScrollView

    // Photo Views
    private lateinit var vpPhotos: ViewPager2
    private lateinit var llNoPhotos: LinearLayout
    private lateinit var vPhotoOverlay: View
    private lateinit var tvPhotoCounter: TextView

    // Info Views
    private lateinit var ivBusinessTypeIcon: ImageView
    private lateinit var tvBusinessName: TextView
    private lateinit var tvBusinessType: TextView
    private lateinit var llStatusBadge: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var llVerifiedBadge: LinearLayout
    private lateinit var tvVerified: TextView

    // Detail Views
    private lateinit var tvDescription: TextView
    private lateinit var llContactPerson: LinearLayout
    private lateinit var tvContactPerson: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnViewMap: LinearLayout

    // Documents
    private lateinit var cvDocuments: CardView
    private lateinit var rvDocuments: RecyclerView

    // Action Buttons
    private lateinit var btnApprove: LinearLayout
    private lateinit var btnReject: LinearLayout
    private lateinit var btnDelete: LinearLayout

    private var affiliateId: String? = null
    private var affiliate: Affiliate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_affiliate_detail)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        affiliateId = intent.getStringExtra("AFFILIATE_ID")

        if (affiliateId == null) {
            Toast.makeText(this, "Error: ID no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupClickListeners()
        loadAffiliateData()
    }

    private fun initializeViews() {
        // Basic Views
        btnBack = findViewById(R.id.btnBack)
        loadingState = findViewById(R.id.loadingState)
        contentState = findViewById(R.id.contentState)

        // Photo Views
        vpPhotos = findViewById(R.id.vpPhotos)
        llNoPhotos = findViewById(R.id.llNoPhotos)
        vPhotoOverlay = findViewById(R.id.vPhotoOverlay)
        tvPhotoCounter = findViewById(R.id.tvPhotoCounter)

        // Info Views
        ivBusinessTypeIcon = findViewById(R.id.ivBusinessTypeIcon)
        tvBusinessName = findViewById(R.id.tvBusinessName)
        tvBusinessType = findViewById(R.id.tvBusinessType)
        llStatusBadge = findViewById(R.id.llStatusBadge)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvStatus = findViewById(R.id.tvStatus)
        llVerifiedBadge = findViewById(R.id.llVerifiedBadge)
        tvVerified = findViewById(R.id.tvVerified)

        // Detail Views
        tvDescription = findViewById(R.id.tvDescription)
        llContactPerson = findViewById(R.id.llContactPerson)
        tvContactPerson = findViewById(R.id.tvContactPerson)
        tvPhone = findViewById(R.id.tvPhone)
        tvEmail = findViewById(R.id.tvEmail)
        tvAddress = findViewById(R.id.tvAddress)
        btnViewMap = findViewById(R.id.btnViewMap)

        // Documents
        cvDocuments = findViewById(R.id.cvDocuments)
        rvDocuments = findViewById(R.id.rvDocuments)

        // Action Buttons
        btnApprove = findViewById(R.id.btnApprove)
        btnReject = findViewById(R.id.btnReject)
        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnViewMap.setOnClickListener {
            affiliate?.let { openMap(it) }
        }

        btnApprove.setOnClickListener {
            showApproveDialog()
        }

        btnReject.setOnClickListener {
            showRejectDialog()
        }

        btnDelete.setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun loadAffiliateData() {
        showLoading()

        affiliateId?.let { id ->
            db.collection("affiliates").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            affiliate = document.toObject(Affiliate::class.java)
                            affiliate?.let {
                                displayAffiliateData(it)
                                // Cargar fotos desde Storage en paralelo
                                loadPhotosFromStorageOptimized(id)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Afiliado no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun loadPhotosFromStorageOptimized(affiliateId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AffiliateDetail", "=== INICIO CARGA OPTIMIZADA ===")
                val startTime = System.currentTimeMillis()

                val affiliateRef = storage.reference
                    .child("affiliates")
                    .child(affiliateId)

                // 1. Obtener estructura completa (rápido, sin descargar URLs)
                val affiliateContent = try {
                    affiliateRef.listAll().await()
                } catch (e: Exception) {
                    Log.e("AffiliateDetail", "Error al listar: ${e.message}")
                    withContext(Dispatchers.Main) {
                        affiliate?.let { setupPhotosWithCategories(it) }
                    }
                    return@launch
                }

                // 2. Crear lista de tareas paralelas
                val photoTasks = mutableListOf<Deferred<PhotoWithCategory?>>()

                // Procesar archivos directos en paralelo
                affiliateContent.items.forEach { item ->
                    photoTasks.add(async {
                        try {
                            val fileName = item.name
                            val categoryName = when {
                                fileName.contains("license") && !fileName.contains("staff") -> "📄 Licencia del Negocio"
                                fileName.contains("staff") || fileName.contains("staff_licenses") -> "👥 Licencias del Personal"
                                else -> "📄 Documento"
                            }
                            val url = item.downloadUrl.await().toString()
                            PhotoWithCategory(url, categoryName)
                        } catch (e: Exception) {
                            Log.e("AffiliateDetail", "Error en ${item.name}: ${e.message}")
                            null
                        }
                    })
                }

                // Procesar carpeta photos en paralelo
                val photosFolder = affiliateContent.prefixes.find { it.name == "photos" }
                if (photosFolder != null) {
                    val photosItems = try {
                        photosFolder.listAll().await()
                    } catch (e: Exception) {
                        null
                    }

                    photosItems?.items?.forEach { item ->
                        photoTasks.add(async {
                            try {
                                val url = item.downloadUrl.await().toString()
                                PhotoWithCategory(url, "📸 Fotos del Negocio")
                            } catch (e: Exception) {
                                Log.e("AffiliateDetail", "Error en foto ${item.name}: ${e.message}")
                                null
                            }
                        })
                    }
                }

                // 3. Esperar TODAS las tareas en paralelo (awaitAll es la clave)
                val allPhotos = photoTasks.awaitAll().filterNotNull()

                val endTime = System.currentTimeMillis()
                Log.d("AffiliateDetail", "✓ Cargadas ${allPhotos.size} fotos en ${endTime - startTime}ms")

                withContext(Dispatchers.Main) {
                    if (allPhotos.isEmpty()) {
                        Log.w("AffiliateDetail", "No hay fotos en Storage")
                        affiliate?.let { setupPhotosWithCategories(it) }
                    } else {
                        displayPhotosFromStorage(allPhotos)
                    }
                }

            } catch (e: Exception) {
                Log.e("AffiliateDetail", "ERROR: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    affiliate?.let { setupPhotosWithCategories(it) }
                }
            }
        }
    }

    private fun displayPhotosFromStorage(photos: List<PhotoWithCategory>) {
        if (photos.isNotEmpty()) {
            llNoPhotos.visibility = View.GONE
            vpPhotos.visibility = View.VISIBLE
            vPhotoOverlay.visibility = View.VISIBLE
            tvPhotoCounter.visibility = View.VISIBLE

            val adapter = AffiliateDetailPhotosAdapter(photos)
            vpPhotos.adapter = adapter

            updatePhotoCounterWithCategory(0, photos)

            vpPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updatePhotoCounterWithCategory(position, photos)
                }
            })
        } else {
            llNoPhotos.visibility = View.VISIBLE
            vpPhotos.visibility = View.GONE
            vPhotoOverlay.visibility = View.GONE
            tvPhotoCounter.visibility = View.GONE
        }
    }

    private fun displayAffiliateData(affiliate: Affiliate) {
        hideLoading()

        // Business Name and Type
        tvBusinessName.text = affiliate.businessName
        tvBusinessType.text = when(affiliate.type) {
            "veterinaria" -> "🏥 Veterinaria"
            "tienda" -> "🛍️ Tienda de Mascotas"
            "albergue" -> "🏠 Albergue"
            else -> affiliate.type.replaceFirstChar { it.uppercase() }
        }

        // Type Icon
        val iconRes = when(affiliate.type) {
            "veterinaria" -> R.drawable.ic_vet
            "tienda" -> R.drawable.ic_shop
            "albergue" -> R.drawable.ic_rescue
            else -> R.drawable.ic_negocio
        }
        ivBusinessTypeIcon.setImageResource(iconRes)

        // Status Badge
        setupStatusBadge(affiliate.status)

        // Verified Badge
        setupVerifiedBadge(affiliate.verified)

        // Description
        tvDescription.text = if (affiliate.description.isNotEmpty()) {
            affiliate.description
        } else {
            "Sin descripción"
        }

        // Contact Info
        if (affiliate.contactPerson.isNotEmpty()) {
            llContactPerson.visibility = View.VISIBLE
            tvContactPerson.text = affiliate.contactPerson
        } else {
            llContactPerson.visibility = View.GONE
        }
        tvPhone.text = affiliate.phone
        tvEmail.text = affiliate.userEmail

        // Location
        tvAddress.text = affiliate.address

        // Documents
        setupDocuments(affiliate)

        // Action Buttons Visibility
        setupActionButtons(affiliate.status)
    }

    private fun setupStatusBadge(status: String) {
        when(status) {
            "pending" -> {
                tvStatus.text = "Pendiente"
                setStatusBadgeColor(llStatusBadge, "#FF9800")
                ivStatusIcon.setImageResource(R.drawable.ic_pending)
            }
            "approved" -> {
                tvStatus.text = "Aprobado"
                setStatusBadgeColor(llStatusBadge, "#4CAF50")
                ivStatusIcon.setImageResource(R.drawable.ic_check)
            }
            "rejected" -> {
                tvStatus.text = "Rechazado"
                setStatusBadgeColor(llStatusBadge, "#F44336")
                ivStatusIcon.setImageResource(R.drawable.ic_error)
            }
            else -> {
                tvStatus.text = status
                setStatusBadgeColor(llStatusBadge, "#9E9E9E")
            }
        }
    }

    private fun setupVerifiedBadge(verified: Boolean) {
        if (verified) {
            tvVerified.text = "✓ Verificado"
            setStatusBadgeColor(llVerifiedBadge, "#4CAF50")
        } else {
            tvVerified.text = "⚠ Sin verificar"
            setStatusBadgeColor(llVerifiedBadge, "#FF9800")
        }
    }

    private fun setStatusBadgeColor(view: LinearLayout, colorHex: String) {
        val drawable = GradientDrawable()
        drawable.cornerRadius = 12f
        drawable.setColor(android.graphics.Color.parseColor(colorHex))
        view.background = drawable
    }

    // MÉTODO DE RESPALDO: Fotos desde Firestore
    private fun setupPhotosWithCategories(affiliate: Affiliate) {
        val categorizedPhotos = mutableListOf<PhotoWithCategory>()

        // Agregar fotos del negocio
        affiliate.photosUrls.filter { it.isNotEmpty() }.forEach { url ->
            categorizedPhotos.add(PhotoWithCategory(url, "📸 Fotos del Negocio"))
        }

        // Agregar licencias del negocio
        affiliate.licenseUrls.filter { it.isNotEmpty() }.forEach { url ->
            categorizedPhotos.add(PhotoWithCategory(url, "📄 Licencias del Negocio"))
        }

        // Agregar licencias del personal
        affiliate.staffLicensesUrls.filter { it.isNotEmpty() }.forEach { url ->
            categorizedPhotos.add(PhotoWithCategory(url, "👥 Licencias del Personal"))
        }

        displayPhotosFromStorage(categorizedPhotos)
    }

    private fun updatePhotoCounterWithCategory(position: Int, photos: List<PhotoWithCategory>) {
        if (photos.isNotEmpty() && position < photos.size) {
            val currentPhoto = photos[position]
            tvPhotoCounter.text = "${currentPhoto.category}\n${position + 1}/${photos.size}"
        }
    }

    private fun setupDocuments(affiliate: Affiliate) {
        cvDocuments.visibility = View.GONE
    }

    private fun setupActionButtons(status: String) {
        when(status) {
            "pending" -> {
                btnApprove.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
            }
            "approved" -> {
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.VISIBLE
            }
            "rejected" -> {
                btnApprove.visibility = View.VISIBLE
                btnReject.visibility = View.GONE
            }
        }
    }

    private fun openMap(affiliate: Affiliate) {
        try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(affiliate.address)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(affiliate.address)}")
                )
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showApproveDialog() {
        AlertDialog.Builder(this)
            .setTitle("Aprobar Afiliación")
            .setMessage("¿Estás seguro de que deseas aprobar a ${affiliate?.businessName}?")
            .setPositiveButton("Aprobar") { _, _ ->
                updateAffiliateStatus("approved")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRejectDialog() {
        AlertDialog.Builder(this)
            .setTitle("Rechazar Afiliación")
            .setMessage("¿Estás seguro de que deseas rechazar a ${affiliate?.businessName}?")
            .setPositiveButton("Rechazar") { _, _ ->
                updateAffiliateStatus("rejected")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Afiliado")
            .setMessage("¿Estás seguro de que deseas eliminar a ${affiliate?.businessName}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAffiliate()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateAffiliateStatus(newStatus: String) {
        val statusText = when(newStatus) {
            "approved" -> "aprobada"
            "rejected" -> "rechazada"
            else -> newStatus
        }

        affiliateId?.let { id ->
            db.collection("affiliates").document(id)
                .update("status", newStatus)
                .addOnSuccessListener {
                    Toast.makeText(this, "Afiliación $statusText exitosamente", Toast.LENGTH_SHORT).show()
                    loadAffiliateData()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteAffiliate() {
        affiliateId?.let { id ->
            db.collection("affiliates").document(id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Afiliado eliminado exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        contentState.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingState.visibility = View.GONE
        contentState.visibility = View.VISIBLE
    }
}