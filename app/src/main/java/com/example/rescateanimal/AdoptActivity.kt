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
import androidx.recyclerview.widget.GridLayoutManager
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

    private lateinit var tabPerros: LinearLayout
    private lateinit var tabGatos: LinearLayout
    private lateinit var tabOtros: LinearLayout
    private lateinit var tvTabPerros: TextView
    private lateinit var tvTabGatos: TextView
    private lateinit var tvTabOtros: TextView

    private lateinit var animalsAdapter: AnimalsAdapter
    private var allAnimals = listOf<Animal>()
    private var filteredAnimals = listOf<AnimalWithDistance>()
    private var userLocation: Location? = null

    private var currentCategory = "perro" // Filtro activo
    private var maxDistance = 10.0f // Radio en km

    private val LOCATION_PERMISSION_REQUEST_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adopt)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        tabPerros = findViewById(R.id.tabPerros)
        tabGatos = findViewById(R.id.tabGatos)
        tabOtros = findViewById(R.id.tabOtros)

        tvTabPerros = findViewById(R.id.tvTabPerros)
        tvTabGatos = findViewById(R.id.tvTabGatos)
        tvTabOtros = findViewById(R.id.tvTabOtros)
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

        // Category Tabs
        tabPerros.setOnClickListener { selectCategory("perro") }
        tabGatos.setOnClickListener { selectCategory("gato") }
        tabOtros.setOnClickListener { selectCategory("otro") }

        // Filter button
        findViewById<TextView>(R.id.btnFilter).setOnClickListener {
            showAdvancedFilters()
        }

        // Seleccionar "Perros" por defecto
        selectCategory("perro")
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
            layoutManager = GridLayoutManager(this@AdoptActivity, 2)
            adapter = animalsAdapter
        }
    }

    private fun selectCategory(category: String) {
        currentCategory = category

        // Reset todas las pestañas a estado no seleccionado
        tabPerros.setBackgroundResource(R.drawable.tab_unselected)
        tabGatos.setBackgroundResource(R.drawable.tab_unselected)
        tabOtros.setBackgroundResource(R.drawable.tab_unselected)

        tvTabPerros.setTextColor(getColor(R.color.text_secondary))
        tvTabGatos.setTextColor(getColor(R.color.text_secondary))
        tvTabOtros.setTextColor(getColor(R.color.text_secondary))

        // Aplicar estilo seleccionado
        when (category) {
            "perro" -> {
                tabPerros.setBackgroundResource(R.drawable.tab_selected)
                tvTabPerros.setTextColor(getColor(android.R.color.white))
            }
            "gato" -> {
                tabGatos.setBackgroundResource(R.drawable.tab_selected)
                tvTabGatos.setTextColor(getColor(android.R.color.white))
            }
            "otro" -> {
                tabOtros.setBackgroundResource(R.drawable.tab_selected)
                tvTabOtros.setTextColor(getColor(android.R.color.white))
            }
        }

        filterAnimalsAndUpdate()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndLoadAnimals()
            } else {
                // Sin permisos, cargar animales sin filtro de distancia
                loadAnimalsFromFirestore()
                Toast.makeText(this, "Permiso de ubicación requerido para mostrar distancias", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getCurrentLocationAndLoadAnimals() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                userLocation = location
                loadAnimalsFromFirestore()

                if (location == null) {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Error al cargar animales: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun filterAnimalsAndUpdate() {
        val searchQuery = etSearch.text.toString().lowercase().trim()

        // Filter by category and search
        val filtered = allAnimals.filter { animal ->
            val matchesCategory = animal.type.lowercase() == currentCategory
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                animal.name.lowercase().contains(searchQuery) ||
                        animal.breed.lowercase().contains(searchQuery) ||
                        animal.location.lowercase().contains(searchQuery)
            }
            matchesCategory && matchesSearch
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
                    } else null
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

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
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
            val categoryName = when(currentCategory) {
                "perro" -> "perros"
                "gato" -> "gatos"
                else -> "mascotas"
            }
            tvResultCount.text = "Encontramos ${filteredAnimals.size} $categoryName para ti"
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
        // TODO: Mostrar diálogo con filtros avanzados (edad, tamaño, vacunado, etc.)
        Toast.makeText(this, "Filtros avanzados - Próximamente", Toast.LENGTH_SHORT).show()
    }
}