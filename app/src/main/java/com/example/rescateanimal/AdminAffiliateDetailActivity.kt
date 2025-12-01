package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View // IMPORTANTE: View genérica para evitar crash
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.rescateanimal.data.models.Affiliate
import com.google.firebase.firestore.FirebaseFirestore

class AdminAffiliateDetailActivity : AppCompatActivity() {

    private lateinit var affiliate: Affiliate
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_affiliate_detail)

        // 1. Recibir Objeto
        affiliate = intent.getSerializableExtra("AFFILIATE_DATA") as? Affiliate ?: run {
            Toast.makeText(this, "Error: Datos no recibidos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupButtons()
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvDetailBusinessName).text = affiliate.businessName
        findViewById<TextView>(R.id.tvDetailType).text = affiliate.type.uppercase()
        findViewById<TextView>(R.id.tvDetailDescription).text = affiliate.description
        findViewById<TextView>(R.id.tvDetailAddress).text = affiliate.address
        findViewById<TextView>(R.id.tvDetailPerson).text = "Encargado: ${affiliate.contactPerson}"
        findViewById<TextView>(R.id.tvDetailPhone).text = affiliate.phone

        val ivHeader = findViewById<ImageView>(R.id.ivDetailHeader)
        if (affiliate.mainPhotoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(affiliate.mainPhotoUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .into(ivHeader)
        }
    }

    private fun setupButtons() {
        // Mapa
        findViewById<View>(R.id.btnViewMap).setOnClickListener {
            val uri = "geo:${affiliate.latitude},${affiliate.longitude}?q=${affiliate.latitude},${affiliate.longitude}(${affiliate.businessName})"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            try { startActivity(intent) } catch (e: Exception) { }
        }

        // Documentos
        findViewById<View>(R.id.btnViewLicense).setOnClickListener {
            openDocument(affiliate.licenseUrl, "No hay licencia adjunta")
        }
        findViewById<View>(R.id.btnViewStaffLicense).setOnClickListener {
            openDocument(affiliate.staffLicenseUrl, "No hay documentos de staff")
        }

        // Acciones
        findViewById<View>(R.id.btnApprove).setOnClickListener {
            showConfirmationDialog("Aprobar", "approved")
        }
        findViewById<View>(R.id.btnReject).setOnClickListener {
            showConfirmationDialog("Rechazar", "rejected")
        }
    }

    private fun openDocument(url: String, errorMsg: String) {
        if (url.isNotEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(this, "No se puede abrir el documento", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmationDialog(action: String, newStatus: String) {
        AlertDialog.Builder(this)
            .setTitle("$action Solicitud")
            .setMessage("¿Estás seguro de continuar?")
            .setPositiveButton("Sí") { _, _ -> updateStatus(newStatus) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateStatus(status: String) {
        val roleUpdate = when(affiliate.type) {
            "veterinaria" -> RoleManager.DB_ROLE_VET
            "albergue" -> RoleManager.DB_ROLE_SHELTER
            "tienda" -> RoleManager.DB_ROLE_STORE
            else -> RoleManager.DB_ROLE_USER
        }

        db.runBatch { batch ->
            val affiliateRef = db.collection("affiliates").document(affiliate.id)

            // 1. Actualizar estado
            batch.update(affiliateRef, "status", status)

            // 2. Si es aprobado -> verified = true y actualizar rol de usuario
            if (status == "approved") {
                batch.update(affiliateRef, "verified", true) // <--- CRÍTICO
                val userRef = db.collection("users").document(affiliate.userId)
                batch.update(userRef, "role", roleUpdate)
            } else if (status == "rejected") {
                batch.update(affiliateRef, "verified", false)
            }

        }.addOnSuccessListener {
            Toast.makeText(this, "Solicitud procesada correctamente", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}