package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var reportTypeDanger: LinearLayout
    private lateinit var reportTypeLost: LinearLayout
    private lateinit var reportTypeAbandoned: LinearLayout
    private lateinit var etLocation: EditText
    private lateinit var btnCurrentLocation: TextView
    private lateinit var etDescription: EditText
    private lateinit var etPhone: EditText
    private lateinit var photoUploadCard: LinearLayout
    private lateinit var rvSelectedPhotos: RecyclerView
    private lateinit var btnSubmitReport: Button
    private lateinit var emergencyPhone: TextView

    // State variables
    private var selectedReportType = ""
    private var selectedPhotos = mutableListOf<Uri>()
    private var currentLocation: Location? = null
    private lateinit var photoAdapter: PhotoAdapter
    private var currentPhotoUri: Uri? = null

    // Activity Result Launchers - CAMBIADO A CÁMARA
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
            showToast("Permiso de ubicación necesario para usar ubicación actual")
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
    }

    private fun initializeViews() {
        reportTypeDanger = findViewById(R.id.reportTypeDanger)
        reportTypeLost = findViewById(R.id.reportTypeLost)
        reportTypeAbandoned = findViewById(R.id.reportTypeAbandoned)
        etLocation = findViewById(R.id.etLocation)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)
        etDescription = findViewById(R.id.etDescription)
        etPhone = findViewById(R.id.etPhone)
        photoUploadCard = findViewById(R.id.photoUploadCard)
        rvSelectedPhotos = findViewById(R.id.rvSelectedPhotos)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)
        emergencyPhone = findViewById(R.id.emergencyPhone)
    }

    private fun setupUI() {
        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Report type selection
        reportTypeDanger.setOnClickListener { selectReportType("danger", reportTypeDanger) }
        reportTypeLost.setOnClickListener { selectReportType("lost", reportTypeLost) }
        reportTypeAbandoned.setOnClickListener { selectReportType("abandoned", reportTypeAbandoned) }

        // Current location button
        btnCurrentLocation.setOnClickListener {
            requestCurrentLocation()
        }

        // Photo upload - CAMBIADO PARA ABRIR CÁMARA
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

        // Text watchers for form validation
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

        etLocation.addTextChangedListener(textWatcher)
        etDescription.addTextChangedListener(textWatcher)
    }

    // NUEVA FUNCIÓN PARA VERIFICAR PERMISO DE CÁMARA
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

    // NUEVA FUNCIÓN PARA ABRIR LA CÁMARA
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

    // NUEVA FUNCIÓN PARA CREAR ARCHIVO DE IMAGEN
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

    private fun selectReportType(type: String, selectedLayout: LinearLayout) {
        // Reset all backgrounds
        reportTypeDanger.setBackgroundResource(R.drawable.report_type_unselected)
        reportTypeLost.setBackgroundResource(R.drawable.report_type_unselected)
        reportTypeAbandoned.setBackgroundResource(R.drawable.report_type_unselected)

        // Set selected background
        selectedLayout.setBackgroundResource(R.drawable.report_type_selected)
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
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    etLocation.setText("Ubicación actual: ${location.latitude}, ${location.longitude}")
                    showToast("Ubicación obtenida correctamente")
                } else {
                    showToast("No se pudo obtener la ubicación actual")
                }
            }.addOnFailureListener {
                showToast("Error al obtener ubicación: ${it.message}")
            }
        } catch (e: SecurityException) {
            showToast("Error de permisos de ubicación")
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
                etLocation.text.isNotEmpty() &&
                etDescription.text.isNotEmpty() &&
                etDescription.text.length >= 10 &&
                selectedPhotos.isNotEmpty()
    }

    private fun submitReport() {
        if (!isFormValid()) {
            showToast("Por favor completa todos los campos obligatorios")
            return
        }

        btnSubmitReport.isEnabled = false
        btnSubmitReport.text = "Enviando..."

        // Upload photos first, then create report
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

        val reportData = hashMapOf(
            "userId" to currentUser.uid,
            "userEmail" to currentUser.email,
            "reportType" to selectedReportType,
            "location" to etLocation.text.toString(),
            "description" to etDescription.text.toString(),
            "contactPhone" to etPhone.text.toString(),
            "photoUrls" to photoUrls,
            "latitude" to currentLocation?.latitude,
            "longitude" to currentLocation?.longitude,
            "status" to "pending",
            "createdAt" to System.currentTimeMillis(),
            "reviewedAt" to null,
            "reviewedBy" to null
        )

        db.collection("reports")
            .add(reportData)
            .addOnSuccessListener { documentReference ->
                showToast("¡Reporte enviado exitosamente!")

                // Show success dialog and navigate back
                showSuccessDialog(documentReference.id)
            }
            .addOnFailureListener { e ->
                showToast("Error al enviar reporte: ${e.message}")
                resetSubmitButton()
            }
    }

    private fun showSuccessDialog(reportId: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Reporte Enviado")
            .setMessage("Tu reporte ha sido enviado correctamente.\n\nID: ${reportId.take(8)}\n\nNuestro equipo lo revisará en las próximas 2 horas.")
            .setPositiveButton("Entendido") { _, _ ->
                // Navigate back to main activity
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

// Adapter for photo RecyclerView
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