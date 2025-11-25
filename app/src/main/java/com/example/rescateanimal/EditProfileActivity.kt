package com.example.rescateanimal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var ivProfilePhoto: ImageView
    private lateinit var btnChangePhoto: TextView
    private lateinit var etFullName: EditText
    private lateinit var etDni: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var etAddress: EditText
    private lateinit var rgHouseType: RadioGroup
    private lateinit var cbHasYard: CheckBox
    private lateinit var etHouseholdMembers: EditText
    private lateinit var cbHasPets: CheckBox
    private lateinit var cbCurrentPets: CheckBox
    private lateinit var etPetExperience: EditText
    private lateinit var etPersonalReference: EditText
    private lateinit var etVetReference: EditText

    private var selectedImageUri: Uri? = null
    private var profilePhotoUrl: String? = null
    private var selectedAvatarUrl: String? = null

    // Lista de avatares cargados desde Firebase
    private val avatarUrls = mutableListOf<String>()

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
    }

    // Activity Result Launchers
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                ivProfilePhoto.setImageBitmap(it)
                selectedImageUri = getImageUri(it)
                selectedAvatarUrl = null // Clear avatar selection
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                selectedAvatarUrl = null // Clear avatar selection
                Glide.with(this)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(ivProfilePhoto)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        initializeViews()
        setupListeners()
        loadAvatarsFromFirebase() // Cargar avatares primero
        loadExistingData()
    }

    private fun initializeViews() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        etFullName = findViewById(R.id.etFullName)
        etDni = findViewById(R.id.etDni)
        etPhone = findViewById(R.id.etPhone)
        etBirthDate = findViewById(R.id.etBirthDate)
        etAddress = findViewById(R.id.etAddress)
        rgHouseType = findViewById(R.id.rgHouseType)
        cbHasYard = findViewById(R.id.cbHasYard)
        etHouseholdMembers = findViewById(R.id.etHouseholdMembers)
        cbHasPets = findViewById(R.id.cbHasPets)
        cbCurrentPets = findViewById(R.id.cbCurrentPets)
        etPetExperience = findViewById(R.id.etPetExperience)
        etPersonalReference = findViewById(R.id.etPersonalReference)
        etVetReference = findViewById(R.id.etVetReference)
    }

    private fun setupListeners() {
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnChangePhoto.setOnClickListener {
            showPhotoOptionsDialog()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadAvatarsFromFirebase() {
        val avatarRef = storage.reference.child("icon_perfil")

        avatarRef.listAll()
            .addOnSuccessListener { listResult ->
                avatarUrls.clear()
                val downloadTasks = listResult.items.map { item ->
                    item.downloadUrl
                }

                // Esperar a que se descarguen todas las URLs
                var completedTasks = 0
                downloadTasks.forEach { task ->
                    task.addOnSuccessListener { uri ->
                        avatarUrls.add(uri.toString())
                        completedTasks++

                        // Cuando todas las URLs estén listas, ordenar alfabéticamente
                        if (completedTasks == downloadTasks.size) {
                            avatarUrls.sort()
                        }
                    }.addOnFailureListener { e ->
                        completedTasks++
                        Toast.makeText(this, "Error al cargar avatar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar avatares: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf(
            "Elegir avatar del sistema",
            "Tomar foto",
            "Elegir de galería"
        )

        AlertDialog.Builder(this)
            .setTitle("Foto de perfil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAvatarSelectionDialog()
                    1 -> checkCameraPermissionAndTakePhoto()
                    2 -> checkStoragePermissionAndPickImage()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAvatarSelectionDialog() {
        if (avatarUrls.isEmpty()) {
            Toast.makeText(this, "Cargando avatares, intenta nuevamente...", Toast.LENGTH_SHORT).show()
            loadAvatarsFromFirebase()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_avatar_selection, null)
        val gridLayout = dialogView.findViewById<GridLayout>(R.id.avatarGrid)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Elige un avatar")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        // Crear ImageViews para cada avatar desde Firebase
        avatarUrls.forEach { avatarUrl ->
            val imageView = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 200
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(16, 16, 16, 16)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(8, 8, 8, 8)
                background = ContextCompat.getDrawable(context, R.drawable.profile_circle_background)

                // Cargar imagen con Glide
                Glide.with(context)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(this)

                setOnClickListener {
                    selectAvatar(avatarUrl)
                    dialog.dismiss()
                }
            }
            gridLayout.addView(imageView)
        }

        dialog.show()
    }

    private fun selectAvatar(avatarUrl: String) {
        selectedAvatarUrl = avatarUrl
        selectedImageUri = null // Clear custom image selection
        profilePhotoUrl = avatarUrl

        Glide.with(this)
            .load(avatarUrl)
            .circleCrop()
            .into(ivProfilePhoto)

        Toast.makeText(this, "Avatar seleccionado", Toast.LENGTH_SHORT).show()
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun checkStoragePermissionAndPickImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                openGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                openGallery()
            }
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        } else {
            Toast.makeText(this, "No se puede abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(pickPhotoIntent)
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "Profile_${System.currentTimeMillis()}",
            null
        )
        return Uri.parse(path)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadExistingData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        user?.let {
                            etFullName.setText(it.displayName)
                        }
                    }
                }

            db.collection("user_profiles").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        populateFieldsFromFirestore(document.data!!)
                    }
                }
        }
    }

    private fun populateFieldsFromFirestore(data: Map<String, Any>) {
        etDni.setText(data["dni"] as? String ?: "")
        etPhone.setText(data["phone"] as? String ?: "")
        etBirthDate.setText(data["birthDate"] as? String ?: "")
        etAddress.setText(data["address"] as? String ?: "")

        val houseType = data["houseType"] as? String
        when (houseType) {
            "Casa" -> findViewById<RadioButton>(R.id.rbHouse).isChecked = true
            "Departamento" -> findViewById<RadioButton>(R.id.rbApartment).isChecked = true
        }

        cbHasYard.isChecked = data["hasYard"] as? Boolean ?: false
        etHouseholdMembers.setText(data["householdMembers"]?.toString() ?: "")
        cbHasPets.isChecked = data["hasPets"] as? Boolean ?: false
        cbCurrentPets.isChecked = data["currentPets"] as? Boolean ?: false
        etPetExperience.setText(data["petExperience"] as? String ?: "")
        etPersonalReference.setText(data["personalReference"] as? String ?: "")
        etVetReference.setText(data["vetReference"] as? String ?: "")

        // Cargar foto de perfil
        profilePhotoUrl = data["profilePhotoUrl"] as? String

        if (profilePhotoUrl != null && profilePhotoUrl!!.isNotEmpty()) {
            // Cargar cualquier URL (ya sea avatar del sistema o foto personalizada)
            Glide.with(this)
                .load(profilePhotoUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivProfilePhoto)
        }
    }

    private fun saveProfileData() {
        if (!validateRequiredFields()) {
            return
        }

        val currentUser = auth.currentUser ?: return

        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Guardando perfil...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        // Determinar qué tipo de foto guardar
        when {
            selectedAvatarUrl != null -> {
                // Usuario seleccionó un avatar del sistema desde Firebase
                saveProfileDataToFirestore(currentUser.uid, selectedAvatarUrl!!, progressDialog)
            }
            selectedImageUri != null -> {
                // Usuario tomó foto o eligió de galería
                uploadProfilePhoto(currentUser.uid) { photoUrl ->
                    saveProfileDataToFirestore(currentUser.uid, photoUrl, progressDialog)
                }
            }
            else -> {
                // Mantener foto existente
                val photoUrl = profilePhotoUrl ?: ""
                saveProfileDataToFirestore(currentUser.uid, photoUrl, progressDialog)
            }
        }
    }

    private fun uploadProfilePhoto(userId: String, onSuccess: (String) -> Unit) {
        val storageRef = storage.reference.child("profile_photos/$userId.jpg")

        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al subir foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    onSuccess("") // Fallback vacío
                }
        } ?: onSuccess("")
    }

    private fun saveProfileDataToFirestore(
        userId: String,
        photoUrl: String,
        progressDialog: AlertDialog
    ) {
        val selectedHouseTypeId = rgHouseType.checkedRadioButtonId
        val houseType = when (selectedHouseTypeId) {
            R.id.rbHouse -> "Casa"
            R.id.rbApartment -> "Departamento"
            else -> ""
        }

        val profileData = mapOf(
            "userId" to userId,
            "fullName" to etFullName.text.toString().trim(),
            "dni" to etDni.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "birthDate" to etBirthDate.text.toString().trim(),
            "address" to etAddress.text.toString().trim(),
            "houseType" to houseType,
            "hasYard" to cbHasYard.isChecked,
            "householdMembers" to (etHouseholdMembers.text.toString().trim().toIntOrNull() ?: 1),
            "hasPets" to cbHasPets.isChecked,
            "currentPets" to cbCurrentPets.isChecked,
            "petExperience" to etPetExperience.text.toString().trim(),
            "personalReference" to etPersonalReference.text.toString().trim(),
            "vetReference" to etVetReference.text.toString().trim(),
            "profilePhotoUrl" to photoUrl,
            "profileCompleted" to true,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("user_profiles").document(userId)
            .set(profileData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateRequiredFields(): Boolean {
        val requiredFields = listOf(
            etFullName to "Nombre completo",
            etDni to "DNI",
            etPhone to "Teléfono",
            etBirthDate to "Fecha de nacimiento",
            etAddress to "Dirección"
        )

        for ((field, fieldName) in requiredFields) {
            if (field.text.toString().trim().isEmpty()) {
                field.error = "$fieldName es obligatorio"
                field.requestFocus()
                return false
            }
        }

        val dni = etDni.text.toString().trim()
        if (dni.length != 8 || !dni.all { it.isDigit() }) {
            etDni.error = "DNI debe tener 8 dígitos"
            etDni.requestFocus()
            return false
        }

        if (rgHouseType.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Selecciona el tipo de vivienda", Toast.LENGTH_SHORT).show()
            return false
        }

        if (etPersonalReference.text.toString().trim().isEmpty()) {
            etPersonalReference.error = "Referencia personal es obligatoria"
            etPersonalReference.requestFocus()
            return false
        }

        return true
    }
}