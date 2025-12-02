package com.example.rescateanimal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore

class AdminUsersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUsersAdapter
    private lateinit var tvUserCount: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingState: LinearLayout

    private val allUsersList = mutableListOf<User>()
    private val filteredUsersList = mutableListOf<User>()
    private lateinit var navigationHelper: NavigationHelper

    private var currentFilter = "all"
    private var currentPageSize = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)

        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        recyclerView = findViewById(R.id.rvAdminUsers)
        tvUserCount = findViewById(R.id.tvUserCount)
        emptyState = findViewById(R.id.emptyState)
        loadingState = findViewById(R.id.loadingState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminUsersAdapter(filteredUsersList) { user ->
            showUserManagementDialog(user)
        }
        recyclerView.adapter = adapter

        setupNavigation()
        setupFilters()
        setupPageSize()
        loadUsers()
    }

    private fun setupFilters() {
        findViewById<Chip>(R.id.chipAllUsers).setOnClickListener { filterUsers("all") }
        findViewById<Chip>(R.id.chipAdmins).setOnClickListener { filterUsers("admin") }
        findViewById<Chip>(R.id.chipPartners).setOnClickListener { filterUsers("partners") }
        findViewById<Chip>(R.id.chipUsers).setOnClickListener { filterUsers("usuario") }
        findViewById<Chip>(R.id.chipBlocked).setOnClickListener { filterUsers("bloqueado") }
    }

    private fun setupPageSize() {
        findViewById<Chip>(R.id.chip10).setOnClickListener {
            currentPageSize = 10
            loadUsers()
        }
        findViewById<Chip>(R.id.chip20).setOnClickListener {
            currentPageSize = 20
            loadUsers()
        }
        findViewById<Chip>(R.id.chip50).setOnClickListener {
            currentPageSize = 50
            loadUsers()
        }
    }

    private fun filterUsers(filter: String) {
        currentFilter = filter
        filteredUsersList.clear()

        when (filter) {
            "all" -> filteredUsersList.addAll(allUsersList)
            "admin" -> filteredUsersList.addAll(allUsersList.filter { it.role == "admin" })
            "partners" -> filteredUsersList.addAll(allUsersList.filter {
                it.role in listOf("veterinaria_verificada", "albergue_verificado", "tienda_verificada")
            })
            "usuario" -> filteredUsersList.addAll(allUsersList.filter { it.role == "usuario" })
            "bloqueado" -> filteredUsersList.addAll(allUsersList.filter { it.role == "bloqueado" })
        }

        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun loadUsers() {
        showLoading(true)

        db.collection("users")
            .limit(currentPageSize.toLong())
            .get()
            .addOnSuccessListener { documents ->
                allUsersList.clear()
                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    allUsersList.add(user)
                }

                // Aplicar filtro actual
                filterUsers(currentFilter)

                // Actualizar contador
                tvUserCount.text = "${allUsersList.size} usuarios cargados"

                showLoading(false)
                updateEmptyState()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUserManagementDialog(user: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_management, null)

        val tvDialogUserInitial = dialogView.findViewById<TextView>(R.id.tvDialogUserInitial)
        val tvDialogUserName = dialogView.findViewById<TextView>(R.id.tvDialogUserName)
        val tvDialogUserEmail = dialogView.findViewById<TextView>(R.id.tvDialogUserEmail)
        val tvDialogCurrentRole = dialogView.findViewById<TextView>(R.id.tvDialogCurrentRole)

        // Opciones de roles
        val roleOptions = mapOf(
            "roleOption1" to Pair("usuario", R.id.checkRole1),
            "roleOption2" to Pair("admin", R.id.checkRole2),
            "roleOption3" to Pair("veterinaria_verificada", R.id.checkRole3),
            "roleOption4" to Pair("albergue_verificado", R.id.checkRole4),
            "roleOption5" to Pair("tienda_verificada", R.id.checkRole5),
            "roleOption6" to Pair("bloqueado", R.id.checkRole6)
        )

        val btnDeleteUser = dialogView.findViewById<LinearLayout>(R.id.btnDeleteUser)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnSaveChanges = dialogView.findViewById<CardView>(R.id.btnSaveChanges)

        // Configurar información del usuario
        val initial = if (user.fullName.isNotEmpty()) user.fullName.substring(0, 1).uppercase() else "?"
        tvDialogUserInitial.text = initial
        tvDialogUserName.text = user.fullName.ifEmpty { "Sin nombre" }
        tvDialogUserEmail.text = user.email
        tvDialogCurrentRole.text = formatRoleName(user.role)

        // Variable para almacenar el nuevo rol seleccionado
        var selectedRole = user.role

        // Marcar el rol actual
        roleOptions.forEach { (optionId, pair) ->
            val roleValue = pair.first
            val checkId = pair.second

            val option = dialogView.findViewById<LinearLayout>(dialogView.resources.getIdentifier(optionId, "id", packageName))
            val check = dialogView.findViewById<ImageView>(checkId)

            // Mostrar check si es el rol actual
            check.visibility = if (roleValue == user.role) View.VISIBLE else View.GONE

            // Click listener para cambiar selección
            option.setOnClickListener {
                selectedRole = roleValue

                // Actualizar checks
                roleOptions.forEach { (_, p) ->
                    dialogView.findViewById<ImageView>(p.second).visibility = View.GONE
                }
                check.visibility = View.VISIBLE
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Botón cancelar
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Botón guardar cambios
        btnSaveChanges.setOnClickListener {
            if (selectedRole != user.role) {
                updateUserRole(user.uid, selectedRole)
            }
            dialog.dismiss()
        }

        // Botón eliminar usuario
        btnDeleteUser.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog(user)
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(user: User) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Eliminar usuario")
            .setMessage("¿Estás seguro de que deseas eliminar a ${user.fullName}?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteUser(user.uid)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateUserRole(uid: String, role: String) {
        db.collection("users").document(uid)
            .update("role", role)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Rol actualizado correctamente", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Error al actualizar rol", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUser(uid: String) {
        db.collection("users").document(uid)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Usuario eliminado correctamente", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Error al eliminar usuario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatRoleName(role: String): String {
        return when(role) {
            "veterinaria_verificada" -> "VETERINARIA"
            "albergue_verificado" -> "ALBERGUE"
            "tienda_verificada" -> "TIENDA"
            "admin" -> "ADMINISTRADOR"
            "bloqueado" -> "BLOQUEADO"
            else -> "USUARIO"
        }
    }

    private fun showLoading(show: Boolean) {
        loadingState.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        if (filteredUsersList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}