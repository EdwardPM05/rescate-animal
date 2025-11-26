package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var reportId: String

    private lateinit var loadingState: LinearLayout
    private lateinit var contentState: ScrollView
    private lateinit var tvReportType: TextView
    private lateinit var tvReportIcon: TextView
    private lateinit var tvReportStatus: TextView
    private lateinit var tvReportDate: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var rvPhotos: RecyclerView
    private lateinit var btnDelete: Button
    private lateinit var btnCallPhone: TextView
    private lateinit var btnOpenMap: TextView

    private var currentReport: Report? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        reportId = intent.getStringExtra("reportId") ?: ""
        if (reportId.isEmpty()) {
            Toast.makeText(this, "Error: ID de reporte no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadReportDetails()
    }

    private fun setupViews() {
        // Back Button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        try {
            loadingState = findViewById(R.id.loadingState)
            contentState = findViewById(R.id.contentState)

            tvReportType = findViewById(R.id.tvReportType)
            tvReportIcon = findViewById(R.id.tvReportIcon)
            tvReportStatus = findViewById(R.id.tvReportStatus)
            tvReportDate = findViewById(R.id.tvReportDate)
            tvDescription = findViewById(R.id.tvDescription)
            tvLocation = findViewById(R.id.tvLocation)
            tvPhone = findViewById(R.id.tvPhone)
            tvCoordinates = findViewById(R.id.tvCoordinates)
            rvPhotos = findViewById(R.id.rvPhotos)
            btnDelete = findViewById(R.id.btnDelete)
            btnCallPhone = findViewById(R.id.btnCallPhone)
            btnOpenMap = findViewById(R.id.btnOpenMap)

            // Setup RecyclerView for photos
            rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

            btnDelete.setOnClickListener {
                showDeleteConfirmation()
            }
        } catch (e: Exception) {
            android.util.Log.e("ReportDetailActivity", "Error in setupViews: ${e.message}")
            Toast.makeText(this, "Error al configurar vistas: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadReportDetails() {
        showLoading()

        db.collection("reports").document(reportId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
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
                        currentReport = report
                        displayReportDetails(report)
                        showContent()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error al cargar detalles: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Reporte no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayReportDetails(report: Report) {
        // Type and Icon
        when (report.type) {
            "danger" -> {
                tvReportType.text = "Animal en peligro"
                tvReportIcon.text = "⚠️ Reporte urgente"
            }
            "lost" -> {
                tvReportType.text = "Animal perdido"
                tvReportIcon.text = "💔 Búsqueda activa"
            }
            "abandoned" -> {
                tvReportType.text = "Animal abandonado"
                tvReportIcon.text = "🏠 Necesita hogar"
            }
            else -> {
                tvReportType.text = "Reporte"
                tvReportIcon.text = "📍 Reporte general"
            }
        }

        // Status
        when (report.status) {
            "pending" -> {
                tvReportStatus.text = "⏳ Pendiente"
                tvReportStatus.setBackgroundResource(R.drawable.status_badge_pending)
            }
            "in_progress" -> {
                tvReportStatus.text = "🔄 En proceso"
                tvReportStatus.setBackgroundResource(R.drawable.status_badge_in_progress)
            }
            "resolved" -> {
                tvReportStatus.text = "✅ Resuelto"
                tvReportStatus.setBackgroundResource(R.drawable.status_badge_resolved)
            }
            else -> {
                tvReportStatus.text = "⏳ Pendiente"
                tvReportStatus.setBackgroundResource(R.drawable.status_badge_pending)
            }
        }

        // Date
        tvReportDate.text = "Reportado ${formatDate(report.createdAt)}"

        // Description
        tvDescription.text = report.description.ifEmpty { "Sin descripción" }

        // Location
        tvLocation.text = report.address.ifEmpty { "Ubicación no disponible" }

        // Coordinates
        tvCoordinates.text = "Lat: ${report.latitude}, Lng: ${report.longitude}"

        // Phone
        tvPhone.text = report.phone.ifEmpty { "No disponible" }

        btnCallPhone.setOnClickListener {
            if (report.phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${report.phone}"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Teléfono no disponible", Toast.LENGTH_SHORT).show()
            }
        }

        // Open in Maps
        btnOpenMap.setOnClickListener {
            val uri = "geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}(${report.type})"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${report.latitude},${report.longitude}"))
                startActivity(browserIntent)
            }
        }

        // Photos
        if (report.photoUrls.isNotEmpty()) {
            findViewById<TextView>(R.id.tvNoPhotos).visibility = View.GONE
            rvPhotos.visibility = View.VISIBLE

            try {
                val adapter = ReportPhotosAdapter(report.photoUrls)
                rvPhotos.adapter = adapter
            } catch (e: Exception) {
                android.util.Log.e("ReportDetailActivity", "Error loading photos: ${e.message}")
                findViewById<TextView>(R.id.tvNoPhotos).visibility = View.VISIBLE
                rvPhotos.visibility = View.GONE
            }
        } else {
            findViewById<TextView>(R.id.tvNoPhotos).visibility = View.VISIBLE
            rvPhotos.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar reporte")
            .setMessage("¿Estás seguro de que deseas eliminar este reporte? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteReport()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteReport() {
        db.collection("reports").document(reportId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Reporte eliminado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "fecha no disponible"

        val date = java.util.Date(timestamp)
        val now = java.util.Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> {
                val format = java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
                format.format(date)
            }
            days > 0 -> "hace ${days.toInt()} día${if (days.toInt() > 1) "s" else ""}"
            hours > 0 -> "hace ${hours.toInt()} hora${if (hours.toInt() > 1) "s" else ""}"
            minutes > 0 -> "hace ${minutes.toInt()} minuto${if (minutes.toInt() > 1) "s" else ""}"
            else -> "hace un momento"
        }
    }

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        contentState.visibility = View.GONE
    }

    private fun showContent() {
        loadingState.visibility = View.GONE
        contentState.visibility = View.VISIBLE
    }
}