package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class RegisterPetActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var etPetName: EditText
    private lateinit var etPetBreed: EditText
    private lateinit var etPetAge: EditText
    private lateinit var spinnerPetSize: Spinner
    private lateinit var etPetLocation: EditText
    private lateinit var cbVaccinated: CheckBox
    private lateinit var cbSterilized: CheckBox
    private lateinit var etPetDescription: EditText
    private lateinit var photoUploadContainer: LinearLayout
    private lateinit var tvPhotoStatus: TextView
    private lateinit var btnRegisterPet: Button
    private lateinit var tvSelectedCoordinates: TextView

    private lateinit var typeDog: LinearLayout
    private lateinit var typeCat: LinearLayout
    private lateinit var typeOther: LinearLayout
    private lateinit var checkDog: TextView
    private lateinit var checkCat: TextView
    private lateinit var checkOther: TextView

    private var selectedType = "perro"
    private var selectedPhotoUri: Uri? = null
    private var currentLocation: Location? = null
    private var shelterData: Map<String, Any>? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val LOCATION_PERMISSION_REQUEST = 1002
        private const val LOCATION_PICKER_REQUEST_CODE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_pet)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initializeViews()
        setupUI()
        checkShelterStatus()
        requestLocationPermission()
    }

    private fun initializeViews() {
        etPetName = findViewById(R.id.etPetName)
        etPetBreed = findViewById(R.id.etPetBreed)
        etPetAge = findViewById(R.id.etPetAge)
        spinnerPetSize = findViewById(R.id.spinnerPetSize)
        etPetLocation = findViewById(R.id.etPetLocation)
        cbVaccinated = findViewById(R.id.cbVaccinated)
        cbSterilized = findViewById(R.id.cbSterilized)
        etPetDescription = findViewById(R.id.etPetDescription)
        photoUploadContainer = findViewById(R.id.photoUploadContainer)
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus)
        btnRegisterPet = findViewById(R.id.btnRegisterPet)
        tvSelectedCoordinates = findViewById(R.id.tvSelectedCoordinates)

        typeDog = findViewById(R.id.typeDog)
        typeCat = findViewById(R.id.typeCat)
        typeOther = findViewById(R.id.typeOther)
        checkDog = findViewById(R.id.checkDog)
        checkCat = findViewById(R.id.checkCat)
        checkOther = findViewById(R.id.checkOther)
    }

    private fun setupUI() {
        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Setup size spinner
        val sizeOptions = arrayOf("Pequeño", "Mediano", "Grande")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sizeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPetSize.adapter = adapter
        spinnerPetSize.setSelection(1) // Default: Mediano

        // Pet type selection
        typeDog.setOnClickListener { selectPetType("perro") }
        typeCat.setOnClickListener { selectPetType("gato") }
        typeOther.setOnClickListener { selectPetType("otro") }

        // Location picker button
        val btnSelectLocation = findViewById<Button>(R.id.btnSelectLocation)
        btnSelectLocation.setOnClickListener {
            openLocationPicker()
        }

        // Photo upload
        photoUploadContainer.setOnClickListener {
            openImagePicker()
        }

        // Register button
        btnRegisterPet.setOnClickListener {
            validateAndRegisterPet()
        }
    }

    private fun checkShelterStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check if user has an approved shelter affiliation
        db.collection("affiliates")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("type", "albergue")
            .whereEqualTo("status", "approved")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(
                        this,
                        "Solo albergues aprobados pueden registrar mascotas",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    shelterData = documents.documents[0].data
                    // Pre-fill location with shelter location
                    shelterData?.let { data ->
                        etPetLocation.setText(data["address"] as? String ?: "")
                        // Guardar las coordenadas del albergue como valores iniciales
                        selectedLatitude = (data["latitude"] as? Double) ?: -12.0464
                        selectedLongitude = (data["longitude"] as? Double) ?: -77.0428

                        currentLocation = Location("shelter").apply {
                            latitude = selectedLatitude!!
                            longitude = selectedLongitude!!
                        }

                        // Actualizar el texto de coordenadas
                        tvSelectedCoordinates.text = "✓ Ubicación del albergue: Lat: ${String.format("%.6f", selectedLatitude)}, Lng: ${String.format("%.6f", selectedLongitude)}"
                        tvSelectedCoordinates.setTextColor(ContextCompat.getColor(this, R.color.primary_orange))
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar permisos", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun selectPetType(type: String) {
        selectedType = type

        // Reset all checkboxes
        checkDog.text = "☐"
        checkCat.text = "☐"
        checkOther.text = "☐"

        // Mark selected
        when (type) {
            "perro" -> checkDog.text = "☑"
            "gato" -> checkCat.text = "☑"
            "otro" -> checkOther.text = "☑"
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && currentLocation == null) {
                currentLocation = location
                updateLocationDisplay(location)
            }
        }
    }

    private fun updateLocationDisplay(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val locationText = address.locality ?: address.subAdminArea ?: "Lima"
                if (etPetLocation.text.isEmpty()) {
                    etPetLocation.setText(locationText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Selecciona una foto"), PICK_IMAGE_REQUEST)
    }

    private fun openLocationPicker() {
        val intent = Intent(this, LocationPickerActivity::class.java)

        // Si ya hay coordenadas seleccionadas, pasarlas
        if (selectedLatitude != null && selectedLongitude != null) {
            intent.putExtra("latitude", selectedLatitude!!)
            intent.putExtra("longitude", selectedLongitude!!)
        }

        startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PICK_IMAGE_REQUEST -> {
                if (resultCode == RESULT_OK && data != null && data.data != null) {
                    selectedPhotoUri = data.data
                    tvPhotoStatus.text = "✓ Foto seleccionada"
                    tvPhotoStatus.setTextColor(getColor(R.color.primary_orange))
                }
            }
            LOCATION_PICKER_REQUEST_CODE -> {
                if (resultCode == RESULT_OK && data != null) {
                    selectedLatitude = data.getDoubleExtra("latitude", 0.0)
                    selectedLongitude = data.getDoubleExtra("longitude", 0.0)
                    val address = data.getStringExtra("address") ?: "Ubicación seleccionada"

                    // Actualizar la UI
                    etPetLocation.setText(address)
                    tvSelectedCoordinates.text = "✓ Ubicación seleccionada: Lat: ${String.format("%.6f", selectedLatitude)}, Lng: ${String.format("%.6f", selectedLongitude)}"
                    tvSelectedCoordinates.setTextColor(ContextCompat.getColor(this, R.color.primary_orange))

                    // Actualizar currentLocation para mantener compatibilidad
                    currentLocation = Location("selected").apply {
                        latitude = selectedLatitude!!
                        longitude = selectedLongitude!!
                    }

                    Toast.makeText(this, "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateAndRegisterPet() {
        val name = etPetName.text.toString().trim()
        val breed = etPetBreed.text.toString().trim()
        val age = etPetAge.text.toString().trim()
        val size = spinnerPetSize.selectedItem.toString().lowercase()
        val location = etPetLocation.text.toString().trim()
        val description = etPetDescription.text.toString().trim()

        // Validations
        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre de la mascota", Toast.LENGTH_SHORT).show()
            return
        }

        if (breed.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa la raza", Toast.LENGTH_SHORT).show()
            return
        }

        if (age.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa la edad", Toast.LENGTH_SHORT).show()
            return
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa la ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa una descripción", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPhotoUri == null) {
            Toast.makeText(this, "Por favor selecciona una foto", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLatitude == null || selectedLongitude == null) {
            Toast.makeText(this, "Por favor selecciona la ubicación en el mapa", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        btnRegisterPet.isEnabled = false
        btnRegisterPet.text = "Registrando..."

        // Upload photo and register pet
        uploadPhotoAndRegisterPet(name, breed, age, size, location, description)
    }

    private fun uploadPhotoAndRegisterPet(
        name: String,
        breed: String,
        age: String,
        size: String,
        location: String,
        description: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }

        // Generate unique ID for the pet
        val petId = db.collection("animals").document().id

        // Upload photo to Firebase Storage
        val photoRef = storage.reference.child("animals/$petId/photo.jpg")
        val uploadTask = photoRef.putFile(selectedPhotoUri!!)

        uploadTask.addOnSuccessListener {
            // Get download URL
            photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                // Create animal document
                createAnimalDocument(
                    petId,
                    name,
                    breed,
                    age,
                    size,
                    location,
                    description,
                    downloadUri.toString(),
                    currentUser.uid
                )
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener URL de foto: ${e.message}", Toast.LENGTH_SHORT).show()
                resetButton()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al subir foto: ${e.message}", Toast.LENGTH_SHORT).show()
            resetButton()
        }
    }

    private fun createAnimalDocument(
        petId: String,
        name: String,
        breed: String,
        age: String,
        size: String,
        location: String,
        description: String,
        photoUrl: String,
        userId: String
    ) {
        val animalData = hashMapOf(
            "id" to petId,
            "name" to name,
            "type" to selectedType,
            "breed" to breed,
            "age" to age,
            "size" to size,
            "location" to location,
            "latitude" to (selectedLatitude ?: -12.0464),
            "longitude" to (selectedLongitude ?: -77.0428),
            "photoUrl" to photoUrl,
            "status" to "available",
            "isVaccinated" to cbVaccinated.isChecked,
            "isSterilized" to cbSterilized.isChecked,
            "description" to description,
            "shelterId" to userId,
            "shelterName" to (shelterData?.get("businessName") as? String ?: ""),
            "shelterPhone" to (shelterData?.get("phone") as? String ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("animals")
            .document(petId)
            .set(animalData)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "¡Mascota registrada exitosamente!\n$name ahora está disponible para adopción",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al registrar mascota: ${e.message}", Toast.LENGTH_SHORT).show()
                resetButton()
            }
    }

    private fun resetButton() {
        btnRegisterPet.isEnabled = true
        btnRegisterPet.text = "Registrar mascota en adopción"
    }
}