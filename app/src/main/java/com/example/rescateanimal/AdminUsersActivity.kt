package com.example.rescateanimal

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescateanimal.User // Asegúrate de importar tu modelo User correcto
import com.google.firebase.firestore.FirebaseFirestore

class AdminUsersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUsersAdapter
    private val usersList = mutableListOf<User>()
    private lateinit var navigationHelper: NavigationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CORRECCIÓN: Ahora apuntamos al archivo que acabamos de crear
        setContentView(R.layout.activity_admin_users)

        db = FirebaseFirestore.getInstance()

        // Ahora sí encontrará este ID porque está en activity_admin_users.xml
        recyclerView = findViewById(R.id.rvAdminUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminUsersAdapter(usersList) { user ->
            showRoleDialog(user)
        }
        recyclerView.adapter = adapter

        setupNavigation()
        loadUsers()
    }

    private fun loadUsers() {
        db.collection("users")
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                usersList.clear()
                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    usersList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRoleDialog(user: User) {
        // Opciones de roles disponibles
        val roles = arrayOf("usuario", "admin", "veterinaria_verificada", "albergue_verificado", "tienda_verificada", "bloqueado")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Gestionar: ${user.fullName}")
        builder.setItems(roles) { _, which ->
            val selectedRole = roles[which]
            updateUserRole(user.uid, selectedRole)
        }
        builder.show()
    }

    private fun updateUserRole(uid: String, role: String) {
        db.collection("users").document(uid)
            .update("role", role)
            .addOnSuccessListener {
                Toast.makeText(this, "Rol actualizado a: $role", Toast.LENGTH_SHORT).show()
                // Opcional: Recargar la lista para reflejar cambios visuales si los hubiera
                loadUsers()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar rol", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}