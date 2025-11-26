package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.rescateanimal.LocationPickerActivity // Asegúrate que esta clase exista
import java.io.ByteArrayOutputStream
import java.util.*

class AffiliateActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedAffiliateType: String = ""
    private var currentLocation: Location? = null
    private var selectedPhotos = mutableListOf<Uri>()
    private var selectedLicense: Uri? = null
    private var selectedStaffLicenses: Uri? = null

    // Para coordenadas seleccionadas del mapa
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val PICK_IMAGE_REQUEST_CODE = 2001
    private val PICK_FILE_REQUEST_CODE = 2002
    private val STAFF_FILE_REQUEST_CODE = 2003
    private val LOCATION_PICKER_REQUEST_CODE = 2004

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_affiliate)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()

        // Solo pedir ubicación si tenemos permiso
        if (hasLocationPermission()) {
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun setupUI() {
        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Affiliate type selections
        findViewById<LinearLayout>(R.id.affiliateTypeVet).setOnClickListener {
            selectAffiliateType("veterinaria")
        }

        findViewById<LinearLayout>(R.id.affiliateTypeStore).setOnClickListener {
            selectAffiliateType("tienda")
        }

        findViewById<LinearLayout>(R.id.affiliateTypeShelter).setOnClickListener {
            selectAffiliateType("albergue")
        }

        // Location picker button
        val btnSelectLocation = findViewById<Button>(R.id.btnSelectAffiliateLocation)
        btnSelectLocation.setOnClickListener {
            openLocationPicker()
        }

        // File uploads
        findViewById<LinearLayout>(R.id.licensFileUpload).setOnClickListener {
            pickFile(PICK_FILE_REQUEST_CODE)
        }

        findViewById<LinearLayout>(R.id.staffLicensesUpload).setOnClickListener {
            pickFile(STAFF_FILE_REQUEST_CODE)
        }

        findViewById<LinearLayout>(R.id.placePhotosUpload).setOnClickListener {
            pickImages()
        }

        // Submit button
        findViewById<Button>(R.id.btnSubmitAffiliate).setOnClickListener {
            submitAffiliateForm()
        }

        // Habilitar el botón desde el inicio (o puedes validar campos en tiempo real)
        findViewById<Button>(R.id.btnSubmitAffiliate).isEnabled = true
    }

    private fun selectAffiliateType(type: String) {
        selectedAffiliateType = type

        // --- LÓGICA DE FONDO Y MARCAS (CORREGIDA EN EL XML) ---
        val vetLayout = findViewById<LinearLayout>(R.id.affiliateTypeVet)
        val storeLayout = findViewById<LinearLayout>(R.id.affiliateTypeStore)
        val shelterLayout = findViewById<LinearLayout>(R.id.affiliateTypeShelter)

        vetLayout.setBackgroundResource(R.drawable.report_type_unselected)
        storeLayout.setBackgroundResource(R.drawable.report_type_unselected)
        shelterLayout.setBackgroundResource(R.drawable.report_type_unselected)

        when (type) {
            "veterinaria" -> vetLayout.setBackgroundResource(R.drawable.report_type_selected)
            "tienda" -> storeLayout.setBackgroundResource(R.drawable.report_type_selected)
            "albergue" -> shelterLayout.setBackgroundResource(R.drawable.report_type_selected)
        }

        // --- LÓGICA DE TEXTO Y MARCAS ---
        val typeDisplay = findViewById<TextView>(R.id.tvAffiliateBusinessType)
        typeDisplay.text = when (type) {
            "veterinaria" -> "🏥 Veterinaria"
            "tienda" -> "🛍️ Tienda de Mascotas"
            "albergue" -> "🏠 Albergue / Refugio"
            else -> "Selecciona un tipo"
        }
        typeDisplay.setTextColor(getColor(android.R.color.black))

        updateAffiliateTypeCheckboxes(type)

        Toast.makeText(this, "Tipo seleccionado: ${getAffiliateTypeText(type)}", Toast.LENGTH_SHORT).show()
    }

    private fun updateAffiliateTypeCheckboxes(selected: String) {
        // Usamos ContextCompat para resolver colores si R.color.primary_orange falla
        val colorSecondary = ContextCompat.getColor(this, R.color.text_secondary)
        val colorPrimary = try { ContextCompat.getColor(this, R.color.primary_orange) } catch (e: Exception) { colorSecondary }

        // Veterinaria
        val checkVet = findViewById<TextView>(R.id.checkVet)
        checkVet.text = if (selected == "veterinaria") "☑" else "☐"
        checkVet.setTextColor(if (selected == "veterinaria") colorPrimary else colorSecondary)

        // Tienda
        val checkStore = findViewById<TextView>(R.id.checkStore)
        checkStore.text = if (selected == "tienda") "☑" else "☐"
        checkStore.setTextColor(if (selected == "tienda") colorPrimary else colorSecondary)

        // Albergue
        val checkShelter = findViewById<TextView>(R.id.checkShelter)
        checkShelter.text = if (selected == "albergue") "☑" else "☐"
        checkShelter.setTextColor(if (selected == "albergue") colorPrimary else colorSecondary)
    }

    private fun openLocationPicker() {
        val intent = Intent(this, LocationPickerActivity::class.java)

        // Si ya hay coordenadas seleccionadas, pasarlas
        if (selectedLatitude != null && selectedLongitude != null) {
            intent.putExtra("latitude", selectedLatitude!!)
            intent.putExtra("longitude", selectedLongitude!!)
        } else if (currentLocation != null) {
            // Si no, usar la ubicación actual
            intent.putExtra("latitude", currentLocation!!.latitude)
            intent.putExtra("longitude", currentLocation!!.longitude)
        }

        startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE)
    }

    private fun pickImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Selecciona fotos"), PICK_IMAGE_REQUEST_CODE)
    }

    private fun pickFile(requestCode: Int) {
        val intent = Intent()
        intent.type = "*/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Selecciona un archivo"), requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST_CODE -> {
                    val clipData = data.clipData
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            selectedPhotos.add(clipData.getItemAt(i).uri)
                        }
                    } else {
                        data.data?.let { selectedPhotos.add(it) }
                    }
                    updatePhotoDisplay()
                }
                PICK_FILE_REQUEST_CODE -> {
                    selectedLicense = data.data
                    updateLicenseDisplay()
                }
                STAFF_FILE_REQUEST_CODE -> {
                    selectedStaffLicenses = data.data
                    updateStaffLicensesDisplay()
                }
                LOCATION_PICKER_REQUEST_CODE -> {
                    if (resultCode == RESULT_OK && data != null) {
                        selectedLatitude = data.getDoubleExtra("latitude", 0.0)
                        selectedLongitude = data.getDoubleExtra("longitude", 0.0)
                        val address = data.getStringExtra("address") ?: "Ubicación seleccionada"

                        // Actualizar el campo de dirección
                        val etAddress = findViewById<EditText>(R.id.etAffiliateAddress)
                        etAddress.setText(address)

                        // Mostrar las coordenadas
                        val tvCoordinates = findViewById<TextView>(R.id.tvAffiliateSelectedCoordinates)
                        tvCoordinates.text = "✓ Ubicación: Lat: ${String.format("%.6f", selectedLatitude)}, Lng: ${String.format("%.6f", selectedLongitude)}"
                        tvCoordinates.setTextColor(ContextCompat.getColor(this, R.color.primary_orange))

                        // Actualizar currentLocation
                        currentLocation = Location("selected").apply {
                            latitude = selectedLatitude!!
                            longitude = selectedLongitude!!
                        }

                        Toast.makeText(this, "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updatePhotoDisplay() {
        val display = findViewById<TextView>(R.id.tvPlacePhotosFileName)
        display.text = "${selectedPhotos.size} foto(s) seleccionada(s)"
        display.setTextColor(getColor(android.R.color.black))
    }

    private fun updateLicenseDisplay() {
        val display = findViewById<TextView>(R.id.tvLicenseFileName)
        val fileName = selectedLicense?.lastPathSegment?.split("%2F")?.last() ?: "Archivo seleccionado"
        display.text = fileName
        display.setTextColor(getColor(android.R.color.black))
    }

    private fun updateStaffLicensesDisplay() {
        val display = findViewById<TextView>(R.id.tvStaffLicensesFileName)
        val fileName = selectedStaffLicenses?.lastPathSegment?.split("%2F")?.last() ?: "Archivo seleccionado"
        display.text = fileName
        display.setTextColor(getColor(android.R.color.black))
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    updateLocationDisplay(location)
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLocationDisplay(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = buildString {
                    if (address.thoroughfare != null) append("${address.thoroughfare}, ")
                    if (address.subLocality != null) append("${address.subLocality}, ")
                    if (address.locality != null) append("${address.locality}")
                }

                val etAddress = findViewById<EditText>(R.id.etAffiliateAddress)
                if (etAddress.text.isEmpty()) {
                    etAddress.setText(addressText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun submitAffiliateForm() {
        val businessName = findViewById<EditText>(R.id.etAffiliateBusinessName).text.toString().trim()
        val contactPerson = findViewById<EditText>(R.id.etAffiliatePerson).text.toString().trim()
        val phone = findViewById<EditText>(R.id.etAffiliatePhone).text.toString().trim()
        val address = findViewById<EditText>(R.id.etAffiliateAddress).text.toString().trim()
        val description = findViewById<EditText>(R.id.etAffiliateDescription).text.toString().trim()
        val hours = findViewById<EditText>(R.id.etAffiliateHours).text.toString().trim()
        val social = findViewById<EditText>(R.id.etAffiliateSocial).text.toString().trim()

        // Validate (tu código de validación)
        if (businessName.isEmpty() || selectedAffiliateType.isEmpty() || contactPerson.isEmpty() || phone.isEmpty() || address.isEmpty() || description.isEmpty() || hours.isEmpty() || selectedPhotos.isEmpty() || selectedLicense == null || selectedStaffLicenses == null || selectedLatitude == null || selectedLongitude == null) {
            Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Upload files and create affiliate
        uploadAffiliateData(
            businessName, contactPerson, phone, address, description, hours, social
        )
    }

    /**
     * FUNCIÓN CLAVE DE OPTIMIZACIÓN: Comprime la imagen seleccionada y sube los bytes.
     */
    private fun uploadCompressedImage(
        imageUri: Uri,
        storageRef: StorageReference,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bitmap: Bitmap?
        try {
            // Obtener el Bitmap
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        } catch (e: Exception) {
            onFailure(e)
            return
        }

        val baos = ByteArrayOutputStream()
        // Compresión al 70% de calidad JPEG (CLAVE PARA VELOCIDAD!)
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data) // Subir bytes comprimidos
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            }.addOnFailureListener { e ->
                onFailure(e)
            }
    }


    /**
     * MODIFICADO: Sube la foto principal comprimida, espera la URL y guarda el documento
     * con esa URL. Las fotos restantes se suben en segundo plano.
     */
    private fun uploadAffiliateData(
        businessName: String,
        contactPerson: String,
        phone: String,
        address: String,
        description: String,
        hours: String,
        social: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val affiliateId = db.collection("affiliates").document().id
        val firstPhotoUri = selectedPhotos.firstOrNull()

        Toast.makeText(this, "Enviando solicitud y comprimiendo foto...", Toast.LENGTH_LONG).show()

        if (firstPhotoUri == null) {
            Toast.makeText(this, "Error: La foto principal es necesaria.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Subir la primera foto (COMPRIMIDA) y esperar la URL
        val firstPhotoRef = storage.reference.child("affiliates/$affiliateId/photos/main_photo")

        uploadCompressedImage(
            imageUri = firstPhotoUri,
            storageRef = firstPhotoRef,
            onSuccess = { mainPhotoUrl ->

                // 2. Crear el documento de Firestore con la URL principal
                val affiliateData = hashMapOf(
                    "id" to affiliateId,
                    "type" to selectedAffiliateType,
                    "businessName" to businessName,
                    "contactPerson" to contactPerson,
                    "phone" to phone,
                    "address" to address,
                    "description" to description,
                    "hours" to hours,
                    "socialMedia" to social,
                    "latitude" to (selectedLatitude ?: -12.0464),
                    "longitude" to (selectedLongitude ?: -77.0428),
                    "userId" to currentUser.uid,
                    "userEmail" to currentUser.email,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis(),
                    "verified" to false,
                    "mainPhotoUrl" to mainPhotoUrl // <-- ¡CAMPO CLAVE AÑADIDO/RESTAURADO!
                )

                db.collection("affiliates").document(affiliateId)
                    .set(affiliateData)
                    .addOnSuccessListener {
                        Log.d("AffiliateDebug", "SUCCESS: Documento de Firestore creado con URL.")
                        // 3. Subir el resto de los archivos en segundo plano
                        uploadRemainingFiles(affiliateId)

                        Toast.makeText(this, "¡Solicitud enviada exitosamente!\nSerá revisada en 2-3 días hábiles", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AffiliateDebug", "ERROR: al crear documento en Firestore: ${e.message}")
                        Toast.makeText(this, "Error al enviar solicitud: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            onFailure = { e ->
                Log.e("AffiliateDebug", "ERROR: al subir foto principal o obtener URL: ${e.message}")
                Toast.makeText(this, "Error al subir foto principal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun uploadRemainingFiles(affiliateId: String) {
        // Subir el resto de fotos (saltando la primera que es la principal)
        selectedPhotos.drop(1).forEachIndexed { index, uri ->
            val ref = storage.reference.child("affiliates/$affiliateId/photos/extra_photo_$index")
            ref.putFile(uri).addOnFailureListener { e ->
                Log.e("AffiliateUpload", "Error subiendo foto extra $index: ${e.message}")
            }
        }

        // Upload license
        selectedLicense?.let { uri ->
            val licenseRef = storage.reference.child("affiliates/$affiliateId/license")
            licenseRef.putFile(uri).addOnFailureListener { e ->
                Log.e("AffiliateUpload", "Error subiendo licencia: ${e.message}")
            }
        }

        // Upload staff licenses
        selectedStaffLicenses?.let { uri ->
            val staffRef = storage.reference.child("affiliates/$affiliateId/staff_licenses")
            staffRef.putFile(uri).addOnFailureListener { e ->
                Log.e("AffiliateUpload", "Error subiendo licencias de staff: ${e.message}")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        }
    }

    private fun getAffiliateTypeText(type: String): String {
        return when (type) {
            "veterinaria" -> "Veterinaria"
            "tienda" -> "Tienda de Mascotas"
            "albergue" -> "Albergue / Refugio"
            else -> "Negocio"
        }
    }
}