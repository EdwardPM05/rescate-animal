package com.example.rescateanimal

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupListeners()
        loadExistingData()
    }

    private fun initializeViews() {
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

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadExistingData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Cargar datos básicos del usuario
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        user?.let {
                            etFullName.setText(it.displayName)
                            // Otros campos se llenarán con datos de perfil extendido
                        }
                    }
                }

            // Cargar datos del perfil extendido
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
    }

    private fun saveProfileData() {
        if (!validateRequiredFields()) {
            return
        }

        val currentUser = auth.currentUser ?: return

        val selectedHouseTypeId = rgHouseType.checkedRadioButtonId
        val houseType = when (selectedHouseTypeId) {
            R.id.rbHouse -> "Casa"
            R.id.rbApartment -> "Departamento"
            else -> ""
        }

        val profileData = mapOf(
            "userId" to currentUser.uid,
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
            "profileCompleted" to true,
            "updatedAt" to System.currentTimeMillis()
        )

        // Guardar en Firestore
        db.collection("user_profiles").document(currentUser.uid)
            .set(profileData)
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                finish()
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

        // Validar DNI (8 dígitos)
        val dni = etDni.text.toString().trim()
        if (dni.length != 8 || !dni.all { it.isDigit() }) {
            etDni.error = "DNI debe tener 8 dígitos"
            etDni.requestFocus()
            return false
        }

        // Validar tipo de vivienda seleccionado
        if (rgHouseType.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Selecciona el tipo de vivienda", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar referencia personal
        if (etPersonalReference.text.toString().trim().isEmpty()) {
            etPersonalReference.error = "Referencia personal es obligatoria"
            etPersonalReference.requestFocus()
            return false
        }

        return true
    }
}