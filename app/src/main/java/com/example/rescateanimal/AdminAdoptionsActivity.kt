package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescateanimal.data.models.Animal
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminAdoptionsActivity : AppCompatActivity() {

    private lateinit var navigationHelper: NavigationHelper
    private lateinit var roleManager: RoleManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminAnimalsAdapter
    private lateinit var loadingState: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var chipGroupAnimalType: ChipGroup
    private lateinit var chipGroupPageSize: ChipGroup

    private val db = FirebaseFirestore.getInstance()
    private val adoptionsList = mutableListOf<Animal>()
    private val allAdoptionsList = mutableListOf<Animal>() // Lista completa sin filtrar
    private val TAG = "AdminAdoptionsActivity"

    private var currentFilter = "all" // all, perro, gato, otros
    private var currentPageSize = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_adoptions)

        // Verificar rol de admin
        roleManager = RoleManager(this)
        if (roleManager.getCurrentRole() != RoleManager.ROLE_ADMIN) {
            Toast.makeText(this, "Acceso denegado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupNavigation()
        setupRecyclerView()
        setupFilters()
        loadAllAdoptions()
    }

    override fun onResume() {
        super.onResume()
        if (roleManager.getCurrentRole() != RoleManager.ROLE_ADMIN) {
            finish()
        } else {
            loadAllAdoptions()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvMyAdoptions)
        loadingState = findViewById(R.id.loadingState)
        emptyState = findViewById(R.id.emptyState)
        chipGroupAnimalType = findViewById(R.id.chipGroupAnimalType)
        chipGroupPageSize = findViewById(R.id.chipGroupPageSize)

        // Actualizar textos para admin
        findViewById<TextView>(R.id.tvPartnerWelcome)?.text = "Gestión de Adopciones"
        findViewById<TextView>(R.id.tvPartnerSubtitle)?.text = "Todas las publicaciones de adopción"
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = AdminAnimalsAdapter(
            adoptionsList,
            onItemClick = { animal -> openAdoptionDetail(animal) },
            onDeleteClick = { animal -> showDeleteConfirmation(animal) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFilters() {
        // Filtro por tipo de animal
        chipGroupAnimalType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chipAll -> {
                        currentFilter = "all"
                        applyFilters()
                    }
                    R.id.chipDog -> {
                        currentFilter = "perro"
                        applyFilters()
                    }
                    R.id.chipCat -> {
                        currentFilter = "gato"
                        applyFilters()
                    }
                    R.id.chipOther -> {
                        currentFilter = "otros"
                        applyFilters()
                    }
                }
            }
        }

        // Filtro por cantidad de items
        chipGroupPageSize.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentPageSize = when (checkedIds[0]) {
                    R.id.chip10 -> 10
                    R.id.chip20 -> 20
                    R.id.chip50 -> 50
                    else -> 10
                }
                applyFilters()
            }
        }
    }

    private fun loadAllAdoptions() {
        showLoading()

        db.collection("animals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                allAdoptionsList.clear()

                for (document in documents) {
                    try {
                        val animal = document.toObject(Animal::class.java).copy(id = document.id)
                        allAdoptionsList.add(animal)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing animal: ${e.message}")
                    }
                }

                Log.d(TAG, "Loaded ${allAdoptionsList.size} animals")
                applyFilters()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading adoptions: ${e.message}")
                Toast.makeText(this, "Error al cargar adopciones", Toast.LENGTH_SHORT).show()
                showEmpty()
            }
    }

    private fun applyFilters() {
        adoptionsList.clear()

        // Filtrar por tipo
        val filtered = when (currentFilter) {
            "all" -> allAdoptionsList
            "perro" -> allAdoptionsList.filter {
                it.type.lowercase() == "perro" || it.type.lowercase() == "dog"
            }
            "gato" -> allAdoptionsList.filter {
                it.type.lowercase() == "gato" || it.type.lowercase() == "cat"
            }
            "otros" -> allAdoptionsList.filter {
                val type = it.type.lowercase()
                type != "perro" && type != "dog" && type != "gato" && type != "cat"
            }
            else -> allAdoptionsList
        }

        // Aplicar límite de página
        adoptionsList.addAll(filtered.take(currentPageSize))

        adapter.notifyDataSetChanged()
        updateUI()
    }

    private fun openAdoptionDetail(animal: Animal) {
        val intent = Intent(this, AdoptionDetailActivity::class.java)
        intent.putExtra("animalId", animal.id)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(animal: Animal) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar publicación")
            .setMessage("¿Estás seguro de eliminar la publicación de ${animal.name}?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAnimal(animal)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAnimal(animal: Animal) {
        db.collection("animals").document(animal.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Publicación eliminada", Toast.LENGTH_SHORT).show()
                allAdoptionsList.remove(animal)
                applyFilters()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting animal: ${e.message}")
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        if (adoptionsList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            loadingState.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            loadingState.visibility = View.GONE
        }
    }

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun showEmpty() {
        loadingState.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}