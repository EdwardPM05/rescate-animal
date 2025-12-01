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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminReportsActivity : AppCompatActivity() {

    private lateinit var navigationHelper: NavigationHelper
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var rvReports: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingState: LinearLayout
    private lateinit var tvReportCount: TextView
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var chipGroupPageSize: ChipGroup
    private lateinit var adapter: AdminReportsAdapter

    // Listas de reportes
    private val allReports = mutableListOf<Report>() // Todos los reportes sin filtrar
    private val filteredReports = mutableListOf<Report>() // Reportes filtrados por estado
    private val displayedReports = mutableListOf<Report>() // Reportes mostrados con paginación

    // Variables de filtrado y paginación
    private var currentFilter = "all" // all, pending, in_progress, resolved
    private var pageSize = 10 // 10, 20, 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_reports)

        setupViews()
        setupNavigation()
        setupFilters()
        loadAllReports()
    }

    private fun setupViews() {
        rvReports = findViewById(R.id.rvAdminReports)
        emptyState = findViewById(R.id.emptyState)
        loadingState = findViewById(R.id.loadingState)
        tvReportCount = findViewById(R.id.tvReportCount)
        chipGroupStatus = findViewById(R.id.chipGroupStatus)
        chipGroupPageSize = findViewById(R.id.chipGroupPageSize)

        // Setup RecyclerView
        rvReports.layoutManager = LinearLayoutManager(this)
        adapter = AdminReportsAdapter(
            displayedReports,
            onDeleteClick = { report -> showDeleteConfirmation(report) },
            onItemClick = { report -> showReportDetails(report) },
            onStatusClick = { report -> showStatusChangeDialog(report) }
        )
        rvReports.adapter = adapter
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun setupFilters() {
        // Filtros por estado
        findViewById<Chip>(R.id.chipAll).setOnClickListener {
            currentFilter = "all"
            applyFilters()
        }

        findViewById<Chip>(R.id.chipPending).setOnClickListener {
            currentFilter = "pending"
            applyFilters()
        }

        findViewById<Chip>(R.id.chipInProgress).setOnClickListener {
            currentFilter = "in_progress"
            applyFilters()
        }

        findViewById<Chip>(R.id.chipResolved).setOnClickListener {
            currentFilter = "resolved"
            applyFilters()
        }

        // Selector de tamaño de página
        findViewById<Chip>(R.id.chip10).setOnClickListener {
            pageSize = 10
            applyFilters()
        }

        findViewById<Chip>(R.id.chip20).setOnClickListener {
            pageSize = 20
            applyFilters()
        }

        findViewById<Chip>(R.id.chip50).setOnClickListener {
            pageSize = 50
            applyFilters()
        }
    }

    private fun loadAllReports() {
        showLoading()

        firestore.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                allReports.clear()

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
                        allReports.add(report)
                    } catch (e: Exception) {
                        android.util.Log.e("AdminReportsActivity", "Error parsing report: ${e.message}")
                    }
                }

                applyFilters()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AdminReportsActivity", "Error loading reports: ${e.message}")
                Toast.makeText(this, "Error al cargar reportes: ${e.message}", Toast.LENGTH_LONG).show()
                showEmpty()
            }
    }

    /**
     * Aplica los filtros de estado y paginación
     */
    private fun applyFilters() {
        // 1. Filtrar por estado
        filteredReports.clear()
        filteredReports.addAll(
            when (currentFilter) {
                "all" -> allReports
                "pending" -> allReports.filter { it.status == "pending" }
                "in_progress" -> allReports.filter { it.status == "in_progress" }
                "resolved" -> allReports.filter { it.status == "resolved" }
                else -> allReports
            }
        )

        // 2. Aplicar paginación (tomar solo los primeros pageSize elementos)
        displayedReports.clear()
        displayedReports.addAll(filteredReports.take(pageSize))

        // 3. Actualizar contador
        updateReportCount()

        // 4. Actualizar vista
        adapter.notifyDataSetChanged()
        showContent()
    }

    /**
     * Actualiza el contador de reportes
     */
    private fun updateReportCount() {
        val totalFiltered = filteredReports.size
        val displayed = displayedReports.size

        val countText = if (totalFiltered == displayed) {
            "$totalFiltered ${if (totalFiltered == 1) "reporte encontrado" else "reportes encontrados"}"
        } else {
            "Mostrando $displayed de $totalFiltered reportes"
        }

        tvReportCount.text = countText
    }

    private fun showDeleteConfirmation(report: Report) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar reporte")
            .setMessage("¿Estás seguro de eliminar este reporte? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteReport(report)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteReport(report: Report) {
        firestore.collection("reports").document(report.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Reporte eliminado exitosamente", Toast.LENGTH_SHORT).show()

                // Remover de todas las listas
                allReports.remove(report)
                filteredReports.remove(report)
                displayedReports.remove(report)

                adapter.notifyDataSetChanged()
                updateReportCount()

                if (displayedReports.isEmpty()) {
                    showEmpty()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showStatusChangeDialog(report: Report) {
        val statuses = arrayOf("Pendiente", "En proceso", "Resuelto")
        val statusValues = arrayOf("pending", "in_progress", "resolved")

        val currentIndex = statusValues.indexOf(report.status)

        AlertDialog.Builder(this)
            .setTitle("Cambiar estado del reporte")
            .setSingleChoiceItems(statuses, currentIndex) { dialog, which ->
                val newStatus = statusValues[which]
                updateReportStatus(report, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateReportStatus(report: Report, newStatus: String) {
        firestore.collection("reports").document(report.id)
            .update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
                report.status = newStatus

                // Re-aplicar filtros para actualizar la lista
                applyFilters()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReportDetails(report: Report) {
        val intent = Intent(this, AdminReportDetailActivity::class.java)
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
        if (displayedReports.isEmpty()) {
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