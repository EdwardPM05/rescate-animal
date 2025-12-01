package com.example.rescateanimal

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
// IMPORTANTE: Esta línea arregla los errores "Unresolved reference: Affiliate"
import com.example.rescateanimal.data.models.Affiliate

class AdminAffiliatesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminAffiliatesAdapter
    private val affiliatesList = mutableListOf<Affiliate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_affiliates)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.rvAdminAffiliates)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminAffiliatesAdapter(affiliatesList) { affiliate ->
            showReviewDialog(affiliate)
        }
        recyclerView.adapter = adapter

        loadPendingAffiliates()
        setupNavigation()
    }

    private fun loadPendingAffiliates() {
        db.collection("affiliates")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                affiliatesList.clear()
                for (document in documents) {
                    val affiliate = document.toObject(Affiliate::class.java)
                    affiliatesList.add(affiliate)
                }
                adapter.notifyDataSetChanged()

                if (affiliatesList.isEmpty()) {
                    Toast.makeText(this, "No hay solicitudes pendientes", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReviewDialog(affiliate: Affiliate) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Revisar Solicitud")
        builder.setMessage(
            "Negocio: ${affiliate.businessName}\n" +
                    "Tipo: ${affiliate.type}\n" +
                    "Solicitante: ${affiliate.userEmail}\n" +
                    "Descripción: ${affiliate.description}\n\n" +
                    "¿Qué deseas hacer con esta solicitud?"
        )

        builder.setPositiveButton("Aprobar") { _, _ ->
            updateAffiliateStatus(affiliate.id, "approved")
        }

        builder.setNegativeButton("Rechazar") { _, _ ->
            updateAffiliateStatus(affiliate.id, "rejected")
        }

        builder.setNeutralButton("Cancelar", null)
        builder.show()
    }

    private fun updateAffiliateStatus(docId: String, newStatus: String) {
        db.collection("affiliates").document(docId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud $newStatus", Toast.LENGTH_SHORT).show()
                loadPendingAffiliates()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavigation() {
        val navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}