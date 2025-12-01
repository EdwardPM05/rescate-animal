package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescateanimal.data.models.Animal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * PartnerMainActivity - Main de Partners
 * Vista principal para Albergues y Veterinarias afiliadas
 * Muestra "Mis Adopciones" - los animales que el partner ha registrado
 */
class PartnerMainActivity : AppCompatActivity() {

    private lateinit var roleManager: RoleManager
    private lateinit var navigationHelper: NavigationHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var rvAdoptions: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingState: LinearLayout
    private lateinit var adapter: MyAdoptionsAdapter

    private val adoptionsList = mutableListOf<Animal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        roleManager = RoleManager(this)

        // Verificar que el rol sea PARTNER
        if (roleManager.getCurrentRole() != RoleManager.ROLE_PARTNER) {
            redirectToCorrectActivity()
            return
        }

        initViews()
        setupRecyclerView()
        setupNavigation()
        loadMyAdoptions()
    }

    override fun onResume() {
        super.onResume()
        if (roleManager.getCurrentRole() != RoleManager.ROLE_PARTNER) {
            redirectToCorrectActivity()
        } else {
            // Recargar las adopciones al volver a la actividad
            loadMyAdoptions()
        }
    }

    private fun redirectToCorrectActivity() {
        val intent = Intent(this, RoleDispatcherActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        // Título
        findViewById<TextView>(R.id.tvPartnerWelcome)?.apply {
            text = "Mis Adopciones"
        }

        // Obtener referencias a las vistas
        rvAdoptions = findViewById(R.id.rvMyAdoptions)
        emptyState = findViewById(R.id.emptyState)
        loadingState = findViewById(R.id.loadingState)
    }

    private fun setupRecyclerView() {
        // Setup RecyclerView
        rvAdoptions.layoutManager = LinearLayoutManager(this)
        adapter = MyAdoptionsAdapter(
            adoptionsList,
            onDeleteClick = { animal -> showDeleteConfirmation(animal) },
            onItemClick = { animal -> showAdoptionDetails(animal) }
        )
        rvAdoptions.adapter = adapter
    }

    private fun loadMyAdoptions() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading()

        db.collection("animals")
            .whereEqualTo("shelterId", currentUser.uid)
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { documents ->
                adoptionsList.clear()

                for (document in documents) {
                    try {
                        val animal = document.toObject(Animal::class.java).copy(id = document.id)
                        adoptionsList.add(animal)
                    } catch (e: Exception) {
                        android.util.Log.e("PartnerMainActivity", "Error parsing animal: ${e.message}")
                    }
                }

                // Ordenar por fecha manualmente (más recientes primero)
                adoptionsList.sortByDescending { it.createdAt }

                adapter.notifyDataSetChanged()
                showContent()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PartnerMainActivity", "Error loading adoptions: ${e.message}")
                Toast.makeText(this, "Error al cargar publicaciones: ${e.message}", Toast.LENGTH_LONG).show()
                showEmpty()
            }
    }

    private fun showDeleteConfirmation(animal: Animal) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar publicación")
            .setMessage("¿Estás seguro de que deseas eliminar esta publicación de adopción? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAdoption(animal)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAdoption(animal: Animal) {
        db.collection("animals").document(animal.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Publicación eliminada exitosamente", Toast.LENGTH_SHORT).show()
                adoptionsList.remove(animal)
                adapter.notifyDataSetChanged()

                if (adoptionsList.isEmpty()) {
                    showEmpty()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAdoptionDetails(animal: Animal) {
        val intent = Intent(this, AdoptionDetailActivity::class.java)
        intent.putExtra("animalId", animal.id)
        startActivity(intent)
    }

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        rvAdoptions.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun showContent() {
        loadingState.visibility = View.GONE
        if (adoptionsList.isEmpty()) {
            showEmpty()
        } else {
            rvAdoptions.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun showEmpty() {
        loadingState.visibility = View.GONE
        rvAdoptions.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}