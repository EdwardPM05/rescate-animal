package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminReportDetailActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var reportId: String
    private var report: Report? = null

    // Views
    private lateinit var loadingState: LinearLayout
    private lateinit var contentState: ScrollView
    private lateinit var vpPhotos: ViewPager2
    private lateinit var llNoPhotos: LinearLayout
    private lateinit var vPhotoOverlay: View
    private lateinit var tvPhotoCounter: TextView
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
    private lateinit var btnOpenMap: LinearLayout
    private lateinit var btnChangeStatus: LinearLayout
    private lateinit var btnDelete: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_report_detail)

        reportId = intent.getStringExtra("reportId") ?: run {
            Toast.makeText(this, "ID de reporte inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadReportDetails()
    }

    private fun initViews() {
        loadingState = findViewById(R.id.loadingState)
        contentState = findViewById(R.id.contentState)
        vpPhotos = findViewById(R.id.vpPhotos)
        llNoPhotos = findViewById(R.id.llNoPhotos)
        vPhotoOverlay = findViewById(R.id.vPhotoOverlay)
        tvPhotoCounter = findViewById(R.id.tvPhotoCounter)
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
        btnOpenMap = findViewById(R.id.btnOpenMap)
        btnChangeStatus = findViewById(R.id.btnChangeStatus)
        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnOpenMap.setOnClickListener {
            openMap()
        }

        btnChangeStatus.setOnClickListener {
            showStatusChangeDialog()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadReportDetails() {
        loadingState.visibility = View.VISIBLE
        contentState.visibility = View.GONE

        firestore.collection("reports").document(reportId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    report = Report(
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

                    displayReportDetails()
                } else {
                    Toast.makeText(this, "Reporte no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun displayReportDetails() {
        val currentReport = report ?: return

        // Photos
        if (currentReport.photoUrls.isNotEmpty()) {
            vpPhotos.visibility = View.VISIBLE
            llNoPhotos.visibility = View.GONE
            vPhotoOverlay.visibility = View.VISIBLE
            tvPhotoCounter.visibility = View.VISIBLE

            val adapter = ReportPhotosAdapter(currentReport.photoUrls)
            vpPhotos.adapter = adapter

            vpPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tvPhotoCounter.text = "${position + 1}/${currentReport.photoUrls.size}"
                }
            })

            tvPhotoCounter.text = "1/${currentReport.photoUrls.size}"
        } else {
            vpPhotos.visibility = View.GONE
            llNoPhotos.visibility = View.VISIBLE
            vPhotoOverlay.visibility = View.GONE
            tvPhotoCounter.visibility = View.GONE
        }

        // Type
        when (currentReport.type) {
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
        }

        // Status
        updateStatusBadge()

        // Date
        tvReportDate.text = formatDate(currentReport.createdAt)

        // Description
        tvDescription.text = currentReport.description

        // Location
        tvLocation.text = currentReport.address
        tvCoordinates.text = "Lat: ${currentReport.latitude}, Lng: ${currentReport.longitude}"

        loadingState.visibility = View.GONE
        contentState.visibility = View.VISIBLE
    }

    private fun updateStatusBadge() {
        val currentReport = report ?: return

        when (currentReport.status) {
            "pending" -> {
                ivStatusIcon.setImageResource(R.drawable.ic_pending)
                tvReportStatus.text = "Pendiente"
                llStatusBadge.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, R.color.status_pending)
            }
            "in_progress" -> {
                ivStatusIcon.setImageResource(R.drawable.ic_in_progress)
                tvReportStatus.text = "En proceso"
                llStatusBadge.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, R.color.status_in_progress)
            }
            "resolved" -> {
                ivStatusIcon.setImageResource(R.drawable.ic_check)
                tvReportStatus.text = "Resuelto"
                llStatusBadge.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, R.color.status_resolved)
            }
        }
    }

    private fun showStatusChangeDialog() {
        val currentReport = report ?: return

        val statuses = arrayOf("Pendiente", "En proceso", "Resuelto")
        val statusValues = arrayOf("pending", "in_progress", "resolved")
        val currentIndex = statusValues.indexOf(currentReport.status)

        AlertDialog.Builder(this)
            .setTitle("Cambiar estado")
            .setSingleChoiceItems(statuses, currentIndex) { dialog, which ->
                updateStatus(statusValues[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateStatus(newStatus: String) {
        firestore.collection("reports").document(reportId)
            .update(mapOf(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                // Actualizar el estado del reporte
                report?.status = newStatus
                updateStatusBadge()
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar reporte")
            .setMessage("¿Eliminar este reporte? No se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteReport()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteReport() {
        firestore.collection("reports").document(reportId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Reporte eliminado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openMap() {
        val currentReport = report ?: return

        val uri = Uri.parse("geo:${currentReport.latitude},${currentReport.longitude}?q=${currentReport.latitude},${currentReport.longitude}(${currentReport.type})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=${currentReport.latitude},${currentReport.longitude}"))
            startActivity(browserIntent)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - date.time
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            days > 7 -> SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale("es", "ES")).format(date)
            days > 0 -> "Hace ${days.toInt()} día${if (days > 1) "s" else ""}"
            else -> {
                val hours = diff / (1000 * 60 * 60)
                if (hours > 0) "Hace ${hours.toInt()} hora${if (hours > 1) "s" else ""}"
                else "Hace un momento"
            }
        }
    }
}