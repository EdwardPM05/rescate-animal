package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var reportId: String

    private lateinit var loadingState: LinearLayout
    private lateinit var contentState: ScrollView
    private lateinit var ivReportTypeIcon: ImageView
    private lateinit var tvReportType: TextView
    private lateinit var tvReportSubtitle: TextView
    private lateinit var llStatusBadge: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvReportStatus: TextView
    private lateinit var tvReportDate: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var vpPhotos: ViewPager2
    private lateinit var tvPhotoCounter: TextView
    private lateinit var llNoPhotos: LinearLayout
    private lateinit var vPhotoOverlay: View
    private lateinit var btnDelete: LinearLayout
    private lateinit var btnOpenMap: LinearLayout

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
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        try {
            loadingState = findViewById(R.id.loadingState)
            contentState = findViewById(R.id.contentState)

            ivReportTypeIcon = findViewById(R.id.ivReportTypeIcon)
            tvReportType = findViewById(R.id.tvReportType)
            tvReportSubtitle = findViewById(R.id.tvReportSubtitle)
            llStatusBadge = findViewById(R.id.llStatusBadge)
            ivStatusIcon = findViewById(R.id.ivStatusIcon)
            tvReportStatus = findViewById(R.id.tvReportStatus)
            tvReportDate = findViewById(R.id.tvReportDate)
            tvDescription = findViewById(R.id.tvDescription)
            tvLocation = findViewById(R.id.tvLocation)
            tvCoordinates = findViewById(R.id.tvCoordinates)
            vpPhotos = findViewById(R.id.vpPhotos)
            tvPhotoCounter = findViewById(R.id.tvPhotoCounter)
            llNoPhotos = findViewById(R.id.llNoPhotos)
            vPhotoOverlay = findViewById(R.id.vPhotoOverlay)
            btnDelete = findViewById(R.id.btnDelete)
            btnOpenMap = findViewById(R.id.btnOpenMap)

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
                ivReportTypeIcon.setImageResource(R.drawable.ic_warning)
                tvReportType.text = "Animal en peligro"
                tvReportSubtitle.text = "Reporte urgente"
            }
            "lost" -> {
                ivReportTypeIcon.setImageResource(R.drawable.ic_lost_pet)
                tvReportType.text = "Animal perdido"
                tvReportSubtitle.text = "Búsqueda activa"
            }
            "abandoned" -> {
                ivReportTypeIcon.setImageResource(R.drawable.ic_house)
                tvReportType.text = "Animal abandonado"
                tvReportSubtitle.text = "Necesita hogar"
            }
            else -> {
                ivReportTypeIcon.setImageResource(R.drawable.ic_pin)
                tvReportType.text = "Reporte"
                tvReportSubtitle.text = "Reporte general"
            }
        }

        // Status
        when (report.status) {
            "pending" -> {
                ivStatusIcon.setImageResource(R.drawable.ic_pending)
                tvReportStatus.text = "Pendiente"
                llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_pending)
            }
            "in_progress" -> {
                ivStatusIcon.setImageResource(R.drawable.ic_in_progress)
                tvReportStatus.text = "En proceso"
                llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_in_progress)
            }
            "resolved" -> {
                ivStatusIcon.setImageResource(R.drawable.ic_check)
                tvReportStatus.text = "Resuelto"
                llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_resolved)
            }
            else -> {
                ivStatusIcon.setImageResource(R.drawable.ic_pending)
                tvReportStatus.text = "Pendiente"
                llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_pending)
            }
        }

        // Date
        tvReportDate.text = formatDate(report.createdAt)

        // Description
        tvDescription.text = report.description.ifEmpty { "Sin descripción" }

        // Location
        tvLocation.text = report.address.ifEmpty { "Ubicación no disponible" }

        // Coordinates
        tvCoordinates.text = "Lat: ${report.latitude}, Lng: ${report.longitude}"

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
            llNoPhotos.visibility = View.GONE
            vpPhotos.visibility = View.VISIBLE
            vPhotoOverlay.visibility = View.VISIBLE
            tvPhotoCounter.visibility = if (report.photoUrls.size > 1) View.VISIBLE else View.GONE

            try {
                val adapter = ReportPhotosAdapter(report.photoUrls)
                vpPhotos.adapter = adapter

                // Update counter on page change
                if (report.photoUrls.size > 1) {
                    tvPhotoCounter.text = "1/${report.photoUrls.size}"
                    vpPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            tvPhotoCounter.text = "${position + 1}/${report.photoUrls.size}"
                        }
                    })
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportDetailActivity", "Error loading photos: ${e.message}")
                llNoPhotos.visibility = View.VISIBLE
                vpPhotos.visibility = View.GONE
                vPhotoOverlay.visibility = View.GONE
                tvPhotoCounter.visibility = View.GONE
            }
        } else {
            llNoPhotos.visibility = View.VISIBLE
            vpPhotos.visibility = View.GONE
            vPhotoOverlay.visibility = View.GONE
            tvPhotoCounter.visibility = View.GONE
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
        if (timestamp == 0L) return "Fecha no disponible"

        val date = java.util.Date(timestamp)
        val now = java.util.Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> {
                val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "ES"))
                format.format(date)
            }
            days > 0 -> "Hace ${days.toInt()} día${if (days.toInt() > 1) "s" else ""}"
            hours > 0 -> "Hace ${hours.toInt()} hora${if (hours.toInt() > 1) "s" else ""}"
            minutes > 0 -> "Hace ${minutes.toInt()} minuto${if (minutes.toInt() > 1) "s" else ""}"
            else -> "Hace un momento"
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