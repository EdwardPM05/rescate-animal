package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescateanimal.data.models.Animal
import com.example.rescateanimal.data.models.AnimalWithDistance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore

class AdoptActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var navigationHelper: NavigationHelper

    private lateinit var etSearch: EditText
    private lateinit var seekBarDistance: SeekBar
    private lateinit var tvDistanceValue: TextView
    private lateinit var rvAnimals: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvResultCount: TextView

    private lateinit var animalsAdapter: AnimalsAdapter
    private var allAnimals = listOf<Animal>()
    private var filteredAnimals = listOf<AnimalWithDistance>()
    private var userLocation: Location? = null

    private var maxDistance = 10.0f // Radio en km
    private var currentCategory = "all" // "all", "dog", "cat", etc.
    private var advancedFilters = AdvancedFilters() // Filtros avanzados

    private val LOCATION_PERMISSION_REQUEST_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adopt)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get category from intent if provided
        currentCategory = intent.getStringExtra("category") ?: "all"

        initializeViews()
        setupUI()
        setupRecyclerView()
        checkLocationPermissionAndLoad()

        // Setup navigation
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun initializeViews() {
        etSearch = findViewById(R.id.etSearch)
        seekBarDistance = findViewById(R.id.seekBarDistance)
        tvDistanceValue = findViewById(R.id.tvDistanceValue)
        rvAnimals = findViewById(R.id.rvAnimals)
        emptyState = findViewById(R.id.emptyState)
        tvResultCount = findViewById(R.id.tvResultCount)
    }

    private fun setupUI() {
        // Distance SeekBar
        seekBarDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxDistance = if (progress == 0) 1.0f else progress.toFloat()
                tvDistanceValue.text = "${maxDistance.toInt()} km"
                if (fromUser) {
                    filterAnimalsAndUpdate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Search EditText
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterAnimalsAndUpdate()
            }
        })

        // Filter button
        findViewById<FrameLayout>(R.id.btnFilter).setOnClickListener {
            showAdvancedFilters()
        }
    }

    private fun setupRecyclerView() {
        animalsAdapter = AnimalsAdapter(
            animals = listOf(),
            onAnimalClick = { animalWithDistance ->
                // Click en la tarjeta del animal - Ver detalles
                val intent = Intent(this, AnimalDetailActivity::class.java)
                intent.putExtra("animal", animalWithDistance.animal)
                startActivity(intent)
            }
        )

        rvAnimals.apply {
            layoutManager = LinearLayoutManager(this@AdoptActivity)
            adapter = animalsAdapter
        }
    }

    private fun checkLocationPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndLoadAnimals()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndLoadAnimals()
            } else {
                // Sin permisos, cargar animales sin filtro de distancia
                loadAnimalsFromFirestore()
                Toast.makeText(
                    this,
                    "Permiso de ubicación requerido para mostrar distancias",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getCurrentLocationAndLoadAnimals() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                userLocation = location
                loadAnimalsFromFirestore()

                if (location == null) {
                    Toast.makeText(
                        this,
                        "No se pudo obtener la ubicación actual",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener {
                loadAnimalsFromFirestore()
                Toast.makeText(this, "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            loadAnimalsFromFirestore()
        }
    }

    private fun loadAnimalsFromFirestore() {
        db.collection("animals")
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { documents ->
                val animals = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Animal::class.java).copy(id = doc.id)
                    } catch (e: Exception) {
                        null // Skip malformed documents
                    }
                }

                allAnimals = animals
                filterAnimalsAndUpdate()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al cargar animales: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmptyState()
            }
    }

    private fun filterAnimalsAndUpdate() {
        val searchQuery = etSearch.text.toString().lowercase().trim()

        // Filter by all criteria
        val filtered = allAnimals.filter { animal ->
            // Filtro de categoría (desde el intent - perro/gato/otro)
            val matchesCategory = if (currentCategory == "all") {
                true
            } else {
                animal.type.lowercase() == currentCategory.lowercase()
            }

            // Filtro de tipo de mascota (desde filtros avanzados)
            val matchesPetType = advancedFilters.petType?.let { petType ->
                animal.type.lowercase() == petType.lowercase()
            } ?: true

            // Búsqueda por texto
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                animal.name.lowercase().contains(searchQuery) ||
                        animal.breed.lowercase().contains(searchQuery) ||
                        animal.location.lowercase().contains(searchQuery)
            }

            // Filtro de tamaño
            val matchesSize = advancedFilters.size?.let { size ->
                animal.size.lowercase() == size.lowercase()
            } ?: true

            // Filtro de edad con lógica mejorada
            val matchesAge = advancedFilters.ageRange?.let { ageRange ->
                matchesAgeRange(animal.age, ageRange)
            } ?: true

            // Filtro de vacunación
            val matchesVaccinated = advancedFilters.isVaccinated?.let { vaccinated ->
                animal.isVaccinated == vaccinated
            } ?: true

            // Filtro de esterilización
            val matchesSterilized = advancedFilters.isSterilized?.let { sterilized ->
                animal.isSterilized == sterilized
            } ?: true

            // Aplicar todos los filtros
            matchesCategory && matchesPetType && matchesSearch && matchesSize &&
                    matchesAge && matchesVaccinated && matchesSterilized
        }

        // Calculate distances and filter by distance
        filteredAnimals = if (userLocation != null) {
            filtered.mapNotNull { animal ->
                if (animal.latitude != 0.0 && animal.longitude != 0.0) {
                    val distance = calculateDistance(
                        userLocation!!.latitude, userLocation!!.longitude,
                        animal.latitude, animal.longitude
                    )

                    if (distance <= maxDistance) {
                        AnimalWithDistance(animal, distance)
                    } else {
                        null
                    }
                } else {
                    // Include animals without location data
                    AnimalWithDistance(animal, -1f)
                }
            }.sortedBy { it.distance }
        } else {
            // Without user location, show all filtered animals
            filtered.map { AnimalWithDistance(it, -1f) }
        }

        updateUI()
    }

    private fun matchesAgeRange(age: String, ageRange: String): Boolean {
        // Parsear edad en formato "X años Y meses" o "X años" o "Y meses"
        val yearsMatch = Regex("""(\d+)\s*año""").find(age)
        val monthsMatch = Regex("""(\d+)\s*mes""").find(age)

        val years = yearsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val months = monthsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        // Convertir todo a meses para comparación más precisa
        val totalMonths = (years * 12) + months

        return when (ageRange) {
            "cachorro" -> totalMonths <= 12      // 0-1 año (0-12 meses)
            "joven" -> totalMonths in 13..36     // 1-3 años (13-36 meses)
            "adulto" -> totalMonths in 37..84    // 3-7 años (37-84 meses)
            "senior" -> totalMonths > 84         // 7+ años (más de 84 meses)
            else -> true
        }
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // Convert to kilometers
    }

    private fun updateUI() {
        if (filteredAnimals.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            animalsAdapter.updateAnimals(filteredAnimals)

            // Actualizar contador de resultados
            val count = filteredAnimals.size
            tvResultCount.text = when {
                count == 1 -> "Encontramos 1 mascota"
                else -> "Encontramos $count mascotas"
            }
        }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        rvAnimals.visibility = View.GONE
        tvResultCount.text = "No hay mascotas disponibles"
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        rvAnimals.visibility = View.VISIBLE
    }

    private fun showAdvancedFilters() {
        val dialog = AdvancedFiltersDialog(this, advancedFilters) { filters ->
            advancedFilters = filters
            filterAnimalsAndUpdate()
        }
        dialog.show()
    }
}