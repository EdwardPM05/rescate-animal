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

class MyAdoptionsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvAdoptions: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingState: LinearLayout
    private lateinit var adapter: MyAdoptionsAdapter

    private val adoptionsList = mutableListOf<Animal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_adoptions)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupViews()
        loadMyAdoptions()
    }

    private fun setupViews() {
        // Back Button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        rvAdoptions = findViewById(R.id.rvMyAdoptions)
        emptyState = findViewById(R.id.emptyState)
        loadingState = findViewById(R.id.loadingState)

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
                        android.util.Log.e("MyAdoptionsActivity", "Error parsing animal: ${e.message}")
                    }
                }

                // Ordenar por fecha manualmente
                adoptionsList.sortByDescending { it.createdAt }

                adapter.notifyDataSetChanged()
                showContent()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MyAdoptionsActivity", "Error loading adoptions: ${e.message}")
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
}