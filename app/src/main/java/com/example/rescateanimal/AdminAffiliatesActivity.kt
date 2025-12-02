package com.example.rescateanimal

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
import com.example.rescateanimal.data.models.Affiliate

class AdminAffiliatesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminAffiliatesAdapter
    private lateinit var loadingState: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var tvAffiliateCount: TextView

    private val allAffiliatesList = mutableListOf<Affiliate>()
    private val filteredAffiliatesList = mutableListOf<Affiliate>()

    private var currentStatusFilter = "all" // all, pending, approved, rejected
    private var currentTypeFilter = "all" // all, veterinaria, tienda, albergue
    private var currentPageSize = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_affiliates)

        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        recyclerView = findViewById(R.id.rvAdminAffiliates)
        loadingState = findViewById(R.id.loadingState)
        emptyState = findViewById(R.id.emptyState)
        tvAffiliateCount = findViewById(R.id.tvAffiliateCount)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminAffiliatesAdapter(filteredAffiliatesList,
            onEditClick = { affiliate -> showReviewDialog(affiliate) },
            onDeleteClick = { affiliate -> confirmDeleteAffiliate(affiliate) }
        )
        recyclerView.adapter = adapter

        setupFilters()
        loadAllAffiliates()
        setupNavigation()
    }

    private fun setupFilters() {
        // Filtro de Estado
        val chipGroupStatus: ChipGroup = findViewById(R.id.chipGroupStatusFilter)
        chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedId = checkedIds[0]
                currentStatusFilter = when (checkedId) {
                    R.id.chipPending -> "pending"
                    R.id.chipApproved -> "approved"
                    R.id.chipRejected -> "rejected"
                    else -> "all"
                }
                applyFilters()
            }
        }

        // Filtro de Tipo
        val chipGroupType: ChipGroup = findViewById(R.id.chipGroupTypeFilter)
        chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedId = checkedIds[0]
                currentTypeFilter = when (checkedId) {
                    R.id.chipVeterinaria -> "veterinaria"
                    R.id.chipTienda -> "tienda"
                    R.id.chipAlbergue -> "albergue"
                    else -> "all"
                }
                applyFilters()
            }
        }

        // Selector de Tamaño de Página
        val chipGroupPageSize: ChipGroup = findViewById(R.id.chipGroupPageSize)
        chipGroupPageSize.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedId = checkedIds[0]
                currentPageSize = when (checkedId) {
                    R.id.chip20 -> 20
                    R.id.chip50 -> 50
                    else -> 10
                }
                applyFilters()
            }
        }
    }

    private fun loadAllAffiliates() {
        showLoading()

        db.collection("affiliates")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                allAffiliatesList.clear()
                for (document in documents) {
                    try {
                        val affiliate = document.toObject(Affiliate::class.java)
                        allAffiliatesList.add(affiliate)
                    } catch (e: Exception) {
                        // Log error pero continuar
                    }
                }
                applyFilters()
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Error cargando datos: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun applyFilters() {
        filteredAffiliatesList.clear()

        var tempList = allAffiliatesList.toList()

        // Aplicar filtro de estado
        if (currentStatusFilter != "all") {
            tempList = tempList.filter { it.status == currentStatusFilter }
        }

        // Aplicar filtro de tipo
        if (currentTypeFilter != "all") {
            tempList = tempList.filter { it.type == currentTypeFilter }
        }

        // Limitar resultados según tamaño de página
        tempList = tempList.take(currentPageSize)

        filteredAffiliatesList.addAll(tempList)

        // Actualizar UI
        updateUI()
    }

    private fun updateUI() {
        hideLoading()

        if (filteredAffiliatesList.isEmpty()) {
            showEmptyState()
        } else {
            showList()
        }

        // Actualizar contador
        val totalCount = allAffiliatesList.size
        val filteredCount = filteredAffiliatesList.size

        if (totalCount == filteredCount) {
            tvAffiliateCount.text = "$totalCount afiliados registrados"
        } else {
            tvAffiliateCount.text = "Mostrando $filteredCount de $totalCount afiliados"
        }

        adapter.notifyDataSetChanged()
    }

    private fun showLoading() {
        loadingState.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingState.visibility = View.GONE
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showList() {
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun showReviewDialog(affiliate: Affiliate) {
        val statusText = when(affiliate.status) {
            "pending" -> "Pendiente"
            "approved" -> "Aprobado"
            "rejected" -> "Rechazado"
            else -> affiliate.status
        }

        val verifiedText = if (affiliate.verified) "Verificado ✓" else "Sin verificar ⚠"

        val contactInfo = if (affiliate.contactPerson.isNotEmpty()) {
            "Contacto: ${affiliate.contactPerson} · ${affiliate.phone}\n"
        } else {
            "Teléfono: ${affiliate.phone}\n"
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Revisar Solicitud")
        builder.setMessage(
            "Negocio: ${affiliate.businessName}\n" +
                    "Tipo: ${affiliate.type}\n" +
                    "Solicitante: ${affiliate.userEmail}\n" +
                    contactInfo +
                    "Dirección: ${affiliate.address}\n" +
                    "Estado actual: $statusText\n" +
                    "Verificación: $verifiedText\n" +
                    "Descripción: ${affiliate.description}\n\n" +
                    "¿Qué deseas hacer con esta solicitud?"
        )

        builder.setPositiveButton("Aprobar") { _, _ ->
            updateAffiliateStatus(affiliate.id, "approved")
        }

        builder.setNegativeButton("Rechazar") { _, _ ->
            updateAffiliateStatus(affiliate.id, "rejected")
        }

        builder.setNeutralButton("Marcar Pendiente") { _, _ ->
            updateAffiliateStatus(affiliate.id, "pending")
        }

        builder.show()
    }

    private fun confirmDeleteAffiliate(affiliate: Affiliate) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Afiliado")
            .setMessage("¿Estás seguro de que deseas eliminar a ${affiliate.businessName}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAffiliate(affiliate.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAffiliate(docId: String) {
        db.collection("affiliates").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Afiliado eliminado", Toast.LENGTH_SHORT).show()
                loadAllAffiliates()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAffiliateStatus(docId: String, newStatus: String) {
        val statusTextSpanish = when(newStatus) {
            "approved" -> "aprobado"
            "rejected" -> "rechazado"
            "pending" -> "marcado como pendiente"
            else -> newStatus
        }

        db.collection("affiliates").document(docId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud $statusTextSpanish", Toast.LENGTH_SHORT).show()
                loadAllAffiliates()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavigation() {
        val navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}