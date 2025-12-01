package com.example.rescateanimal

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AffiliateActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Business Type Cards
    private lateinit var affiliateTypeVet: CardView
    private lateinit var affiliateTypeStore: CardView
    private lateinit var affiliateTypeShelter: CardView

    // Input Fields
    private lateinit var etBusinessName: EditText
    private lateinit var tvBusinessType: TextView
    private lateinit var etPerson: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var tvSelectedCoordinates: TextView
    private lateinit var etDescription: EditText
    private lateinit var etHours: EditText
    private lateinit var etSocial: EditText

    // Document Upload Cards
    private lateinit var licensFileUpload: CardView
    private lateinit var staffLicensesUpload: CardView
    private lateinit var placePhotosUpload: CardView
    private lateinit var tvLicenseFileName: TextView
    private lateinit var tvStaffLicensesFileName: TextView
    private lateinit var tvPlacePhotosFileName: TextView

    // RecyclerViews para mostrar miniaturas de cada tipo
    private lateinit var rvLicensePhotos: RecyclerView
    private lateinit var rvStaffLicensesPhotos: RecyclerView
    private lateinit var rvPlacePhotos: RecyclerView

    private lateinit var licensePhotoAdapter: PhotoAdapter
    private lateinit var staffLicensesPhotoAdapter: PhotoAdapter
    private lateinit var placePhotosAdapter: PhotoAdapter

    // Submit Button
    private lateinit var btnSubmitAffiliate: CardView
    private lateinit var btnSelectLocation: CardView

    // State Variables
    private var selectedType: String = ""
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    // Listas separadas para cada tipo de archivo
    private var licenseFileUris: MutableList<Uri> = mutableListOf()
    private var staffLicensesUris: MutableList<Uri> = mutableListOf()
    private var placePhotosUris: MutableList<Uri> = mutableListOf()

    // File Pickers actualizados para soportar múltiples archivos
    private val pickLicenseFile = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            licenseFileUris.clear()
            licenseFileUris.addAll(uris)
            tvLicenseFileName.text = "✓ ${uris.size} archivo(s) seleccionado(s)"
            tvLicenseFileName.setTextColor(getColor(R.color.primary_orange))
            setupLicenseRecyclerView()
            checkFormValidity()
        }
    }

    private val pickStaffLicenses = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            staffLicensesUris.clear()
            staffLicensesUris.addAll(uris)
            tvStaffLicensesFileName.text = "✓ ${uris.size} archivo(s) seleccionado(s)"
            tvStaffLicensesFileName.setTextColor(getColor(R.color.primary_orange))
            setupStaffLicensesRecyclerView()
            checkFormValidity()
        }
    }

    private val pickPlacePhotos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            placePhotosUris.clear()
            placePhotosUris.addAll(uris)
            tvPlacePhotosFileName.text = "✓ ${uris.size} foto(s) seleccionada(s)"
            tvPlacePhotosFileName.setTextColor(getColor(R.color.primary_orange))
            setupPlacePhotosRecyclerView()
            checkFormValidity()
        }
    }

    private val selectLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                selectedLatitude = data.getDoubleExtra("latitude", 0.0)
                selectedLongitude = data.getDoubleExtra("longitude", 0.0)
                val address = data.getStringExtra("address") ?: "Ubicación seleccionada"

                etAddress.setText(address)
                tvSelectedCoordinates.text = "📍 Lat: ${"%.6f".format(selectedLatitude)}, Lng: ${"%.6f".format(selectedLongitude)}"
                tvSelectedCoordinates.setTextColor(getColor(R.color.primary_orange))
                checkFormValidity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_affiliate)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // Back Button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Business Type Cards
        affiliateTypeVet = findViewById(R.id.affiliateTypeVet)
        affiliateTypeStore = findViewById(R.id.affiliateTypeStore)
        affiliateTypeShelter = findViewById(R.id.affiliateTypeShelter)

        // Input Fields
        etBusinessName = findViewById(R.id.etAffiliateBusinessName)
        tvBusinessType = findViewById(R.id.tvAffiliateBusinessType)
        etPerson = findViewById(R.id.etAffiliatePerson)
        etPhone = findViewById(R.id.etAffiliatePhone)
        etAddress = findViewById(R.id.etAffiliateAddress)
        tvSelectedCoordinates = findViewById(R.id.tvAffiliateSelectedCoordinates)
        etDescription = findViewById(R.id.etAffiliateDescription)
        etHours = findViewById(R.id.etAffiliateHours)
        etSocial = findViewById(R.id.etAffiliateSocial)

        // Document Upload Cards
        licensFileUpload = findViewById(R.id.licensFileUpload)
        staffLicensesUpload = findViewById(R.id.staffLicensesUpload)
        placePhotosUpload = findViewById(R.id.placePhotosUpload)
        tvLicenseFileName = findViewById(R.id.tvLicenseFileName)
        tvStaffLicensesFileName = findViewById(R.id.tvStaffLicensesFileName)
        tvPlacePhotosFileName = findViewById(R.id.tvPlacePhotosFileName)

        // RecyclerViews
        rvLicensePhotos = findViewById(R.id.rvLicensePhotos)
        rvStaffLicensesPhotos = findViewById(R.id.rvStaffLicensesPhotos)
        rvPlacePhotos = findViewById(R.id.rvPlacePhotos)

        // Buttons
        btnSubmitAffiliate = findViewById(R.id.btnSubmitAffiliate)
        btnSelectLocation = findViewById(R.id.btnSelectAffiliateLocation)
    }

    private fun setupListeners() {
        // Business Type Selection
        affiliateTypeVet.setOnClickListener {
            selectBusinessType("veterinaria", affiliateTypeVet)
        }

        affiliateTypeStore.setOnClickListener {
            selectBusinessType("tienda", affiliateTypeStore)
        }

        affiliateTypeShelter.setOnClickListener {
            selectBusinessType("albergue", affiliateTypeShelter)
        }

        // Location Selection
        btnSelectLocation.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            selectLocationLauncher.launch(intent)
        }

        // Document Uploads - Ahora soportan múltiples archivos
        licensFileUpload.setOnClickListener {
            pickLicenseFile.launch("image/*")
        }

        staffLicensesUpload.setOnClickListener {
            pickStaffLicenses.launch("image/*")
        }

        placePhotosUpload.setOnClickListener {
            pickPlacePhotos.launch("image/*")
        }

        // Submit Button
        btnSubmitAffiliate.setOnClickListener {
            submitAffiliateRequest()
        }

        // Text Change Listeners
        setupTextWatchers()
    }

    private fun setupTextWatchers() {
        val textWatcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                checkFormValidity()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        etBusinessName.addTextChangedListener(textWatcher)
        etPerson.addTextChangedListener(textWatcher)
        etPhone.addTextChangedListener(textWatcher)
        etDescription.addTextChangedListener(textWatcher)
        etHours.addTextChangedListener(textWatcher)
    }

    private fun setupLicenseRecyclerView() {
        licensePhotoAdapter = PhotoAdapter(licenseFileUris) { position ->
            licenseFileUris.removeAt(position)
            licensePhotoAdapter.notifyItemRemoved(position)

            if (licenseFileUris.isNotEmpty()) {
                tvLicenseFileName.text = "✓ ${licenseFileUris.size} archivo(s) seleccionado(s)"
            } else {
                tvLicenseFileName.text = "Subir licencia de funcionamiento"
                tvLicenseFileName.setTextColor(getColor(R.color.text_secondary))
                rvLicensePhotos.visibility = RecyclerView.GONE
            }
            checkFormValidity()
        }

        rvLicensePhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvLicensePhotos.adapter = licensePhotoAdapter
        rvLicensePhotos.visibility = RecyclerView.VISIBLE
    }

    private fun setupStaffLicensesRecyclerView() {
        staffLicensesPhotoAdapter = PhotoAdapter(staffLicensesUris) { position ->
            staffLicensesUris.removeAt(position)
            staffLicensesPhotoAdapter.notifyItemRemoved(position)

            if (staffLicensesUris.isNotEmpty()) {
                tvStaffLicensesFileName.text = "✓ ${staffLicensesUris.size} archivo(s) seleccionado(s)"
            } else {
                tvStaffLicensesFileName.text = "Subir licencias de profesionales"
                tvStaffLicensesFileName.setTextColor(getColor(R.color.text_secondary))
                rvStaffLicensesPhotos.visibility = RecyclerView.GONE
            }
            checkFormValidity()
        }

        rvStaffLicensesPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvStaffLicensesPhotos.adapter = staffLicensesPhotoAdapter
        rvStaffLicensesPhotos.visibility = RecyclerView.VISIBLE
    }

    private fun setupPlacePhotosRecyclerView() {
        placePhotosAdapter = PhotoAdapter(placePhotosUris) { position ->
            placePhotosUris.removeAt(position)
            placePhotosAdapter.notifyItemRemoved(position)

            if (placePhotosUris.isNotEmpty()) {
                tvPlacePhotosFileName.text = "✓ ${placePhotosUris.size} foto(s) seleccionada(s)"
            } else {
                tvPlacePhotosFileName.text = "Subir fotos del establecimiento"
                tvPlacePhotosFileName.setTextColor(getColor(R.color.text_secondary))
                rvPlacePhotos.visibility = RecyclerView.GONE
            }
            checkFormValidity()
        }

        rvPlacePhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPlacePhotos.adapter = placePhotosAdapter
        rvPlacePhotos.visibility = RecyclerView.VISIBLE
    }

    private fun selectBusinessType(type: String, selectedCard: CardView) {
        // Reset all cards to default colors
        affiliateTypeVet.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bg_vet_unselected))
        affiliateTypeStore.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bg_store_unselected))
        affiliateTypeShelter.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bg_shelter_unselected))

        // Reset elevations
        affiliateTypeVet.cardElevation = 0f
        affiliateTypeStore.cardElevation = 0f
        affiliateTypeShelter.cardElevation = 0f

        // Set selected card with color and elevation
        val selectedColor = when(type) {
            "veterinaria" -> ContextCompat.getColor(this, R.color.bg_vet_selected)
            "tienda" -> ContextCompat.getColor(this, R.color.bg_store_selected)
            "albergue" -> ContextCompat.getColor(this, R.color.bg_shelter_selected)
            else -> ContextCompat.getColor(this, android.R.color.white)
        }

        selectedCard.setCardBackgroundColor(selectedColor)
        selectedCard.cardElevation = 8f

        selectedType = type

        val typeText = when (type) {
            "veterinaria" -> "✓ Veterinaria"
            "tienda" -> "✓ Tienda de Mascotas"
            "albergue" -> "✓ Albergue / Refugio"
            else -> "Selecciona un tipo arriba"
        }
        tvBusinessType.text = typeText
        tvBusinessType.setTextColor(getColor(R.color.text_primary))

        checkFormValidity()
    }

    private fun checkFormValidity() {
        val isValid = selectedType.isNotEmpty() &&
                etBusinessName.text.toString().trim().isNotEmpty() &&
                etPerson.text.toString().trim().isNotEmpty() &&
                etPhone.text.toString().trim().isNotEmpty() &&
                selectedLatitude != 0.0 && selectedLongitude != 0.0 &&
                licenseFileUris.isNotEmpty() &&
                staffLicensesUris.isNotEmpty() &&
                placePhotosUris.isNotEmpty() &&
                etDescription.text.toString().trim().isNotEmpty() &&
                etHours.text.toString().trim().isNotEmpty()

        btnSubmitAffiliate.isClickable = isValid
        btnSubmitAffiliate.isFocusable = isValid
        btnSubmitAffiliate.setCardBackgroundColor(
            if (isValid) getColor(R.color.primary_orange)
            else getColor(android.R.color.darker_gray)
        )
    }

    private fun submitAffiliateRequest() {
        if (!checkFormValidityBoolean()) {
            Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_LONG).show()
            return
        }

        btnSubmitAffiliate.isClickable = false
        Toast.makeText(this, "Enviando solicitud...", Toast.LENGTH_SHORT).show()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: No se pudo identificar al usuario", Toast.LENGTH_SHORT).show()
            return
        }

        uploadDocuments(currentUser.uid)
    }

    private fun checkFormValidityBoolean(): Boolean {
        return selectedType.isNotEmpty() &&
                etBusinessName.text.toString().trim().isNotEmpty() &&
                etPerson.text.toString().trim().isNotEmpty() &&
                etPhone.text.toString().trim().isNotEmpty() &&
                selectedLatitude != 0.0 && selectedLongitude != 0.0 &&
                licenseFileUris.isNotEmpty() &&
                staffLicensesUris.isNotEmpty() &&
                placePhotosUris.isNotEmpty() &&
                etDescription.text.toString().trim().isNotEmpty() &&
                etHours.text.toString().trim().isNotEmpty()
    }

    private fun uploadDocuments(userId: String) {
        val storageRef = storage.reference.child("affiliates/$userId")
        val uploads = mutableListOf<String>()
        val totalUploads = licenseFileUris.size + staffLicensesUris.size + placePhotosUris.size

        // Upload license files
        licenseFileUris.forEachIndexed { index, uri ->
            val licenseRef = storageRef.child("license_${System.currentTimeMillis()}_$index")
            licenseRef.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                licenseRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                uploads.add(downloadUri.toString())
                checkUploadsComplete(userId, uploads, totalUploads)
            }.addOnFailureListener {
                Toast.makeText(this, "Error al subir licencia", Toast.LENGTH_SHORT).show()
                btnSubmitAffiliate.isClickable = true
            }
        }

        // Upload staff licenses
        staffLicensesUris.forEachIndexed { index, uri ->
            val staffRef = storageRef.child("staff_licenses_${System.currentTimeMillis()}_$index")
            staffRef.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                staffRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                uploads.add(downloadUri.toString())
                checkUploadsComplete(userId, uploads, totalUploads)
            }.addOnFailureListener {
                Toast.makeText(this, "Error al subir licencias del personal", Toast.LENGTH_SHORT).show()
                btnSubmitAffiliate.isClickable = true
            }
        }

        // Upload place photos
        placePhotosUris.forEachIndexed { index, uri ->
            val photoRef = storageRef.child("photo_${System.currentTimeMillis()}_$index")
            photoRef.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                photoRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                uploads.add(downloadUri.toString())
                checkUploadsComplete(userId, uploads, totalUploads)
            }.addOnFailureListener {
                Toast.makeText(this, "Error al subir foto ${index + 1}", Toast.LENGTH_SHORT).show()
                btnSubmitAffiliate.isClickable = true
            }
        }
    }

    private fun checkUploadsComplete(userId: String, uploads: MutableList<String>, totalUploads: Int) {
        if (uploads.size == totalUploads) {
            createAffiliateRequest(userId, uploads)
        }
    }

    private fun createAffiliateRequest(userId: String, documentUrls: List<String>) {
        val affiliateData = hashMapOf(
            "userId" to userId,
            "type" to selectedType,
            "businessName" to etBusinessName.text.toString().trim(),
            "contactPerson" to etPerson.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "address" to etAddress.text.toString().trim(),
            "latitude" to selectedLatitude,
            "longitude" to selectedLongitude,
            "description" to etDescription.text.toString().trim(),
            "hours" to etHours.text.toString().trim(),
            "socialMedia" to etSocial.text.toString().trim(),
            "documentUrls" to documentUrls,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("affiliates")
            .add(affiliateData)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Solicitud enviada exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al enviar solicitud: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSubmitAffiliate.isClickable = true
            }
    }
}