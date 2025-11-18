package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
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

    // NUEVAS VISTAS PARA RECYCLERVIEW
    private lateinit var rvSelectedPhotos: RecyclerView
    private val selectedPhotoUris = mutableListOf<Uri>()
    private lateinit var photoAdapter: SelectedPhotosAdapter

    private lateinit var typeDog: LinearLayout
    private lateinit var typeCat: LinearLayout
    private lateinit var typeOther: LinearLayout
    private lateinit var checkDog: TextView
    private lateinit var checkCat: TextView
    private lateinit var checkOther: TextView

    private var selectedType = "perro"
    private var currentLocation: Location? = null
    private var shelterData: Map<String, Any>? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var currentPhotoUri: Uri? = null

    companion object {
        private const val TAG = "RegisterPetActivity"
        private const val LOCATION_PERMISSION_REQUEST = 1002
        private const val LOCATION_PICKER_REQUEST_CODE = 2001
    }

    // Activity Result Launchers - PARA CÁMARA
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            Log.d(TAG, "Foto tomada exitosamente: $currentPhotoUri")
            // AGREGAMOS LA FOTO A LA LISTA
            selectedPhotoUris.add(currentPhotoUri!!)
            photoAdapter.notifyItemInserted(selectedPhotoUris.size - 1)

            // MOSTRAMOS EL RECYCLERVIEW
            rvSelectedPhotos.visibility = View.VISIBLE
            tvPhotoStatus.text = "${selectedPhotoUris.size} foto(s) seleccionada(s)"
            tvPhotoStatus.setTextColor(getColor(R.color.primary_orange))

            Toast.makeText(this, "Foto cargada correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "Error al tomar foto - success: $success, uri: $currentPhotoUri")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara necesario para tomar fotos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate iniciado")

        try {
            setContentView(R.layout.activity_register_pet)

            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            Log.d(TAG, "Firebase inicializado, usuario: ${auth.currentUser?.email}")

            initializeViews()
            setupUI()
            setupPhotosRecyclerView()
            checkShelterStatus()
            requestLocationPermission()

            Log.d(TAG, "onCreate completado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        try {
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
            rvSelectedPhotos = findViewById(R.id.rvSelectedPhotos)

            typeDog = findViewById(R.id.typeDog)
            typeCat = findViewById(R.id.typeCat)
            typeOther = findViewById(R.id.typeOther)
            checkDog = findViewById(R.id.checkDog)
            checkCat = findViewById(R.id.checkCat)
            checkOther = findViewById(R.id.checkOther)

            Log.d(TAG, "Vistas inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
            throw e
        }
    }

    // NUEVA FUNCIÓN PARA CONFIGURAR EL RECYCLERVIEW
    private fun setupPhotosRecyclerView() {
        try {
            photoAdapter = SelectedPhotosAdapter(selectedPhotoUris) { position ->
                // Callback para eliminar foto
                selectedPhotoUris.removeAt(position)
                photoAdapter.notifyItemRemoved(position)
                photoAdapter.notifyItemRangeChanged(position, selectedPhotoUris.size)

                if (selectedPhotoUris.isEmpty()) {
                    rvSelectedPhotos.visibility = View.GONE
                    tvPhotoStatus.text = "Toma una foto de la mascota para mostrarla en adopción"
                    tvPhotoStatus.setTextColor(getColor(R.color.text_secondary))
                } else {
                    tvPhotoStatus.text = "${selectedPhotoUris.size} foto(s) seleccionada(s)"
                }
            }

            rvSelectedPhotos.apply {
                layoutManager = GridLayoutManager(this@RegisterPetActivity, 3)
                adapter = photoAdapter
            }

            Log.d(TAG, "RecyclerView configurado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar RecyclerView: ${e.message}", e)
        }
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
        spinnerPetSize.setSelection(1)

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
            checkCameraPermissionAndOpen()
        }

        // Register button
        btnRegisterPet.setOnClickListener {
            validateAndRegisterPet()
        }
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
            Log.d(TAG, "Abriendo cámara con URI: $currentPhotoUri")
            takePictureLauncher.launch(currentPhotoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir cámara: ${e.message}", e)
            Toast.makeText(this, "Error al abrir cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("PetPhotos")

        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File.createTempFile(
            "PET_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun checkShelterStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Verificando estado del albergue para usuario: ${currentUser.email}")

        db.collection("affiliates")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("type", "albergue")
            .whereEqualTo("status", "approved")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Consulta completada. Documentos encontrados: ${documents.size()}")

                if (documents.isEmpty) {
                    Log.w(TAG, "Usuario no es un albergue aprobado")
                    Toast.makeText(
                        this,
                        "Solo albergues aprobados pueden registrar mascotas",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    shelterData = documents.documents[0].data
                    Log.d(TAG, "Albergue verificado: ${shelterData?.get("businessName")}")

                    shelterData?.let { data ->
                        etPetLocation.setText(data["address"] as? String ?: "")
                        selectedLatitude = (data["latitude"] as? Double) ?: -12.0464
                        selectedLongitude = (data["longitude"] as? Double) ?: -77.0428

                        currentLocation = Location("shelter").apply {
                            latitude = selectedLatitude!!
                            longitude = selectedLongitude!!
                        }

                        tvSelectedCoordinates.text = "✓ Ubicación del albergue: Lat: ${String.format("%.6f", selectedLatitude)}, Lng: ${String.format("%.6f", selectedLongitude)}"
                        tvSelectedCoordinates.setTextColor(ContextCompat.getColor(this, R.color.primary_orange))
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al verificar permisos: ${e.message}", e)
                Toast.makeText(this, "Error al verificar permisos: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun selectPetType(type: String) {
        selectedType = type
        checkDog.text = "☐"
        checkCat.text = "☐"
        checkOther.text = "☐"

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
            Log.e(TAG, "Error al obtener dirección: ${e.message}", e)
        }
    }

    private fun openLocationPicker() {
        val intent = Intent(this, LocationPickerActivity::class.java)

        if (selectedLatitude != null && selectedLongitude != null) {
            intent.putExtra("latitude", selectedLatitude!!)
            intent.putExtra("longitude", selectedLongitude!!)
        }

        startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCATION_PICKER_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                selectedLatitude = data.getDoubleExtra("latitude", 0.0)
                selectedLongitude = data.getDoubleExtra("longitude", 0.0)
                val address = data.getStringExtra("address") ?: "Ubicación seleccionada"

                etPetLocation.setText(address)
                tvSelectedCoordinates.text = "✓ Ubicación seleccionada: Lat: ${String.format("%.6f", selectedLatitude)}, Lng: ${String.format("%.6f", selectedLongitude)}"
                tvSelectedCoordinates.setTextColor(ContextCompat.getColor(this, R.color.primary_orange))

                currentLocation = Location("selected").apply {
                    latitude = selectedLatitude!!
                    longitude = selectedLongitude!!
                }

                Toast.makeText(this, "Ubicación guardada correctamente", Toast.LENGTH_SHORT).show()
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

        if (selectedPhotoUris.isEmpty()) {
            Toast.makeText(this, "Por favor toma al menos una foto de la mascota", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLatitude == null || selectedLongitude == null) {
            Toast.makeText(this, "Por favor selecciona la ubicación en el mapa", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegisterPet.isEnabled = false
        btnRegisterPet.text = "Registrando..."

        uploadPhotosAndRegisterPet(name, breed, age, size, location, description)
    }

    private fun uploadPhotosAndRegisterPet(
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

        val petId = db.collection("animals").document().id
        val photoUrls = mutableListOf<String>()
        var uploadedCount = 0
        val totalPhotos = selectedPhotoUris.size

        Log.d(TAG, "Iniciando subida de $totalPhotos fotos")

        selectedPhotoUris.forEachIndexed { index, photoUri ->
            val photoRef = storage.reference.child("animals/$petId/photo_$index.jpg")

            photoRef.putFile(photoUri)
                .addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        photoUrls.add(downloadUri.toString())
                        uploadedCount++
                        Log.d(TAG, "Foto $uploadedCount/$totalPhotos subida")

                        if (uploadedCount == totalPhotos) {
                            createAnimalDocument(
                                petId,
                                name,
                                breed,
                                age,
                                size,
                                location,
                                description,
                                photoUrls,
                                currentUser.uid
                            )
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error al obtener URL: ${e.message}", e)
                        Toast.makeText(this, "Error al obtener URL de foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        resetButton()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al subir foto: ${e.message}", e)
                    Toast.makeText(this, "Error al subir foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
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
        photoUrls: List<String>,
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
            "photoUrl" to photoUrls.firstOrNull(),
            "photoUrls" to photoUrls,
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

        Log.d(TAG, "Creando documento de animal: $petId")

        db.collection("animals")
            .document(petId)
            .set(animalData)
            .addOnSuccessListener {
                Log.d(TAG, "Mascota registrada exitosamente")
                Toast.makeText(
                    this,
                    "¡Mascota registrada exitosamente!\n$name ahora está disponible para adopción",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al registrar mascota: ${e.message}", e)
                Toast.makeText(this, "Error al registrar mascota: ${e.message}", Toast.LENGTH_SHORT).show()
                resetButton()
            }
    }

    private fun resetButton() {
        btnRegisterPet.isEnabled = true
        btnRegisterPet.text = "Registrar mascota"
    }

    inner class SelectedPhotosAdapter(
        private val photos: MutableList<Uri>,
        private val onRemoveClick: (Int) -> Unit
    ) : RecyclerView.Adapter<SelectedPhotosAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
            val btnRemove: TextView = view.findViewById(R.id.btnRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_selected_photo, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(photos[position])
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.ivPhoto)

            holder.btnRemove.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveClick(adapterPosition)
                }
            }
        }

        override fun getItemCount() = photos.size
    }
}