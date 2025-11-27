package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var navigationHelper: NavigationHelper

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var reportTypeDanger: CardView
    private lateinit var reportTypeLost: CardView
    private lateinit var reportTypeAbandoned: CardView
    private lateinit var locationDisplayCard: CardView
    private lateinit var locationLoadingState: LinearLayout
    private lateinit var locationLoadedState: LinearLayout
    private lateinit var tvLocationAddress: TextView
    private lateinit var tvLocationCoords: TextView
    private lateinit var btnRefreshLocation: LinearLayout
    private lateinit var etDescription: EditText
    private lateinit var etPhone: EditText
    private lateinit var photoUploadCard: CardView
    private lateinit var rvSelectedPhotos: RecyclerView
    private lateinit var btnSubmitReport: Button
    private lateinit var emergencyPhone: LinearLayout

    // State variables
    private var selectedReportType = ""
    private var selectedPhotos = mutableListOf<Uri>()
    private var currentLocation: Location? = null
    private var currentLocationAddress: String = ""
    private lateinit var photoAdapter: PhotoAdapter
    private var currentPhotoUri: Uri? = null

    // Activity Result Launchers
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            selectedPhotos.add(currentPhotoUri!!)
            setupPhotoRecyclerView()
            checkFormValidity()
            showToast("Foto agregada. Puedes tomar más fotos si lo deseas")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            showToast("Permiso de cámara necesario para tomar fotos del reporte")
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCurrentLocation()
        } else {
            showToast("Permiso de ubicación necesario. No se puede crear reporte sin ubicación actual.")
            btnSubmitReport.isEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize UI
        initializeViews()
        setupUI()
        setupNavigation()
        checkFormValidity()

        // Solicitar ubicación automáticamente al iniciar
        requestCurrentLocation()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        reportTypeDanger = findViewById(R.id.reportTypeDanger)
        reportTypeLost = findViewById(R.id.reportTypeLost)
        reportTypeAbandoned = findViewById(R.id.reportTypeAbandoned)
        locationDisplayCard = findViewById(R.id.locationDisplayCard)
        locationLoadingState = findViewById(R.id.locationLoadingState)
        locationLoadedState = findViewById(R.id.locationLoadedState)
        tvLocationAddress = findViewById(R.id.tvLocationAddress)
        tvLocationCoords = findViewById(R.id.tvLocationCoords)
        btnRefreshLocation = findViewById(R.id.btnRefreshLocation)
        etDescription = findViewById(R.id.etDescription)
        etPhone = findViewById(R.id.etPhone)
        photoUploadCard = findViewById(R.id.photoUploadCard)
        rvSelectedPhotos = findViewById(R.id.rvSelectedPhotos)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)
        emergencyPhone = findViewById(R.id.emergencyPhone)
    }

    private fun setupUI() {
        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Report type selection
        reportTypeDanger.setOnClickListener { selectReportType("danger", reportTypeDanger) }
        reportTypeLost.setOnClickListener { selectReportType("lost", reportTypeLost) }
        reportTypeAbandoned.setOnClickListener { selectReportType("abandoned", reportTypeAbandoned) }

        // Refresh location button
        btnRefreshLocation.setOnClickListener {
            requestCurrentLocation()
        }

        // Photo upload - Abrir cámara
        photoUploadCard.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        // Emergency phone
        emergencyPhone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+51907377938")
            startActivity(intent)
        }

        // Submit button
        btnSubmitReport.setOnClickListener {
            if (isFormValid()) {
                submitReport()
            }
        }

        // Text watcher for description validation
        setupTextWatchers()
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkFormValidity()
            }
        }

        etDescription.addTextChangedListener(textWatcher)
        etPhone.addTextChangedListener(textWatcher)
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri)
        } catch (e: Exception) {
            showToast("Error al abrir cámara: ${e.message}")
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("ReportPhotos")

        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File.createTempFile(
            "REPORT_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun selectReportType(type: String, selectedCard: CardView) {
        // Reset all backgrounds
        reportTypeDanger.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        reportTypeLost.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        reportTypeAbandoned.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))

        // Set selected background color based on type
        val selectedColor = when(type) {
            "danger" -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
            "lost" -> ContextCompat.getColor(this, android.R.color.holo_blue_light)
            "abandoned" -> ContextCompat.getColor(this, android.R.color.holo_purple)
            else -> ContextCompat.getColor(this, android.R.color.white)
        }

        selectedCard.setCardBackgroundColor(selectedColor)
        selectedCard.cardElevation = 8f

        // Reset elevation for others
        if (selectedCard != reportTypeDanger) reportTypeDanger.cardElevation = 0f
        if (selectedCard != reportTypeLost) reportTypeLost.cardElevation = 0f
        if (selectedCard != reportTypeAbandoned) reportTypeAbandoned.cardElevation = 0f

        selectedReportType = type
        checkFormValidity()
    }

    private fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        locationLoadingState.visibility = View.VISIBLE
        locationLoadedState.visibility = View.GONE

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location

                    getAddressFromLocation(location.latitude, location.longitude)

                    tvLocationCoords.text = "Lat: ${String.format("%.6f", location.latitude)}, " +
                            "Lng: ${String.format("%.6f", location.longitude)}"

                    locationLoadingState.visibility = View.GONE
                    locationLoadedState.visibility = View.VISIBLE

                    showToast("Ubicación obtenida correctamente")
                    checkFormValidity()
                } else {
                    showToast("No se pudo obtener la ubicación. Intenta de nuevo.")
                    locationLoadingState.visibility = View.GONE
                }
            }.addOnFailureListener { e ->
                showToast("Error al obtener ubicación: ${e.message}")
                locationLoadingState.visibility = View.GONE
            }
        } catch (e: SecurityException) {
            showToast("Error de permisos de ubicación")
            locationLoadingState.visibility = View.GONE
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = buildString {
                    if (address.thoroughfare != null) append("${address.thoroughfare}, ")
                    if (address.subLocality != null) append("${address.subLocality}, ")
                    if (address.locality != null) append("${address.locality}")
                }

                currentLocationAddress = if (addressText.isNotEmpty()) {
                    addressText
                } else {
                    "Ubicación: $latitude, $longitude"
                }

                tvLocationAddress.text = currentLocationAddress
            } else {
                currentLocationAddress = "Ubicación: $latitude, $longitude"
                tvLocationAddress.text = currentLocationAddress
            }
        } catch (e: Exception) {
            currentLocationAddress = "Ubicación: $latitude, $longitude"
            tvLocationAddress.text = currentLocationAddress
        }
    }

    private fun setupPhotoRecyclerView() {
        photoAdapter = PhotoAdapter(selectedPhotos) { position ->
            selectedPhotos.removeAt(position)
            photoAdapter.notifyItemRemoved(position)
            checkFormValidity()

            if (selectedPhotos.isEmpty()) {
                rvSelectedPhotos.visibility = RecyclerView.GONE
            }
        }

        rvSelectedPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedPhotos.adapter = photoAdapter
        rvSelectedPhotos.visibility = RecyclerView.VISIBLE
    }

    private fun checkFormValidity() {
        val isValid = isFormValid()

        btnSubmitReport.isEnabled = isValid
        btnSubmitReport.setBackgroundResource(
            if (isValid) R.drawable.button_submit_enabled
            else R.drawable.button_submit_disabled
        )
    }

    private fun isFormValid(): Boolean {
        return selectedReportType.isNotEmpty() &&
                currentLocation != null &&
                etDescription.text.isNotEmpty() &&
                etDescription.text.length >= 10 &&
                selectedPhotos.isNotEmpty()
    }

    private fun submitReport() {
        if (!isFormValid()) {
            showToast("Por favor completa todos los campos obligatorios")
            return
        }

        if (currentLocation == null) {
            showToast("Debes obtener tu ubicación actual para enviar el reporte")
            return
        }

        btnSubmitReport.isEnabled = false
        btnSubmitReport.text = "Enviando..."

        uploadPhotos { photoUrls ->
            createReport(photoUrls)
        }
    }

    private fun uploadPhotos(onComplete: (List<String>) -> Unit) {
        val photoUrls = mutableListOf<String>()
        var uploadedCount = 0

        if (selectedPhotos.isEmpty()) {
            onComplete(emptyList())
            return
        }

        selectedPhotos.forEachIndexed { index, uri ->
            val fileName = "reports/${System.currentTimeMillis()}_$index.jpg"
            val imageRef = storage.reference.child(fileName)

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        photoUrls.add(downloadUrl.toString())
                        uploadedCount++

                        if (uploadedCount == selectedPhotos.size) {
                            onComplete(photoUrls)
                        }
                    }
                }
                .addOnFailureListener {
                    showToast("Error al subir foto: ${it.message}")
                    resetSubmitButton()
                }
        }
    }

    private fun createReport(photoUrls: List<String>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Error: Usuario no autenticado")
            resetSubmitButton()
            return
        }

        if (currentLocation == null) {
            showToast("Error: No hay ubicación disponible")
            resetSubmitButton()
            return
        }

        val reportData = hashMapOf(
            "userId" to currentUser.uid,
            "userEmail" to currentUser.email,
            "reportType" to selectedReportType,
            "location" to currentLocationAddress,
            "description" to etDescription.text.toString(),
            "contactPhone" to etPhone.text.toString(),
            "photoUrls" to photoUrls,
            "latitude" to currentLocation!!.latitude,
            "longitude" to currentLocation!!.longitude,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis(),
            "reviewedAt" to null,
            "reviewedBy" to null
        )

        db.collection("reports")
            .add(reportData)
            .addOnSuccessListener { documentReference ->
                showToast("¡Reporte enviado exitosamente!")
                showSuccessDialog(documentReference.id)
            }
            .addOnFailureListener { e ->
                showToast("Error al enviar reporte: ${e.message}")
                resetSubmitButton()
            }
    }

    private fun showSuccessDialog(reportId: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("✅ Reporte Enviado")
            .setMessage("Tu reporte ha sido enviado correctamente.\n\n" +
                    "📍 Ubicación verificada por GPS\n" +
                    "ID: ${reportId.take(8)}\n\n" +
                    "Nuestro equipo lo revisará en las próximas 2 horas.")
            .setPositiveButton("Entendido") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun resetSubmitButton() {
        btnSubmitReport.isEnabled = true
        btnSubmitReport.text = "Enviar reporte"
        checkFormValidity()
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

class PhotoAdapter(
    private val photos: MutableList<Uri>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivPhoto)
        val removeButton: TextView = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PhotoViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = photos[position]
        holder.imageView.setImageURI(uri)
        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount() = photos.size
}