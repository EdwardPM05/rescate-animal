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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MyReportsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvReports: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingState: LinearLayout
    private lateinit var adapter: MyReportsAdapter

    private val reportsList = mutableListOf<Report>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reports)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupViews()
        loadMyReports()
    }

    private fun setupViews() {
        // Back Button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        rvReports = findViewById(R.id.rvMyReports)
        emptyState = findViewById(R.id.emptyState)
        loadingState = findViewById(R.id.loadingState)

        // Setup RecyclerView
        rvReports.layoutManager = LinearLayoutManager(this)
        adapter = MyReportsAdapter(
            reportsList,
            onDeleteClick = { report -> showDeleteConfirmation(report) },
            onItemClick = { report -> showReportDetails(report) }
        )
        rvReports.adapter = adapter
    }

    private fun loadMyReports() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading()

        db.collection("reports")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                reportsList.clear()

                for (document in documents) {
                    try {
                        val report = Report(
                            id = document.id,
                            userId = document.getString("userId") ?: "",
                            type = document.getString("reportType") ?: document.getString("type") ?: "",
                            description = document.getString("description") ?: "",
                            phone = document.getString("contactPhone") ?: document.getString("phone") ?: "",
                            latitude = document.getDouble("latitude") ?: 0.0,
                            longitude = document.getDouble("longitude") ?: 0.0,
                            address = document.getString("location") ?: document.getString("address") ?: "",
                            photoUrls = document.get("photoUrls") as? List<String> ?: listOf(),
                            status = document.getString("status") ?: "pending",
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                        reportsList.add(report)
                    } catch (e: Exception) {
                        android.util.Log.e("MyReportsActivity", "Error parsing report: ${e.message}")
                    }
                }

                // Ordenar por fecha manualmente
                reportsList.sortByDescending { it.createdAt }

                adapter.notifyDataSetChanged()
                showContent()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MyReportsActivity", "Error loading reports: ${e.message}")
                Toast.makeText(this, "Error al cargar reportes: ${e.message}", Toast.LENGTH_LONG).show()
                showEmpty()
            }
    }

    private fun showDeleteConfirmation(report: Report) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar reporte")
            .setMessage("¿Estás seguro de que deseas eliminar este reporte? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteReport(report)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteReport(report: Report) {
        db.collection("reports").document(report.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Reporte eliminado exitosamente", Toast.LENGTH_SHORT).show()
                reportsList.remove(report)
                adapter.notifyDataSetChanged()

                if (reportsList.isEmpty()) {
                    showEmpty()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReportDetails(report: Report) {
        val intent = Intent(this, ReportDetailActivity::class.java)
        intent.putExtra("reportId", report.id)
        startActivity(intent)
    }

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        rvReports.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun showContent() {
        loadingState.visibility = View.GONE
        if (reportsList.isEmpty()) {
            showEmpty()
        } else {
            rvReports.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun showEmpty() {
        loadingState.visibility = View.GONE
        rvReports.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}