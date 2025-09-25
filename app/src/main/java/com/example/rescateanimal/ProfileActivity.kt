package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var navigationHelper: NavigationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupViews()
        loadUserData()

        // Setup navigation usando NavigationHelper
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando regresamos de EditProfile
        loadUserData()
    }

    private fun setupViews() {
        // Back Button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Menu Options
        findViewById<android.widget.LinearLayout>(R.id.menuEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<android.widget.LinearLayout>(R.id.menuMyAdoptions).setOnClickListener {
            Toast.makeText(this, "Mis Adopciones - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.LinearLayout>(R.id.menuMyReports).setOnClickListener {
            Toast.makeText(this, "Mis Reportes - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.LinearLayout>(R.id.menuNotifications).setOnClickListener {
            Toast.makeText(this, "Notificaciones - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.LinearLayout>(R.id.menuPrivacy).setOnClickListener {
            Toast.makeText(this, "Privacidad - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.LinearLayout>(R.id.menuHelp).setOnClickListener {
            Toast.makeText(this, "Ayuda - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Logout
        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Obtener datos básicos del usuario
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument.exists()) {
                        val user = userDocument.toObject(User::class.java)
                        user?.let {
                            findViewById<TextView>(R.id.tvUserName).text = it.displayName
                            findViewById<TextView>(R.id.tvEmail).text = it.email
                            findViewById<TextView>(R.id.tvMemberSince).text = "Rescatista Desde ${formatDate(it.createdAt)}"
                            findViewById<TextView>(R.id.tvJoinDate).text = formatDate(it.createdAt)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar datos básicos: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // Obtener datos del perfil extendido
            db.collection("user_profiles").document(currentUser.uid)
                .get()
                .addOnSuccessListener { profileDocument ->
                    if (profileDocument.exists()) {
                        updateUIWithProfileData(profileDocument.data!!)
                    } else {
                        // Si no hay perfil extendido, usar datos por defecto
                        setDefaultProfileData()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                    setDefaultProfileData()
                }
        }
    }

    private fun updateUIWithProfileData(profileData: Map<String, Any>) {
        // Actualizar datos del perfil extendido
        findViewById<TextView>(R.id.tvPhone).text = profileData["phone"] as? String ?: "+51 999 999 999"
        findViewById<TextView>(R.id.tvLocation).text =
            (profileData["address"] as? String)?.split(",")?.take(2)?.joinToString(", ") ?: "Lima, Perú"

        // Stats - por ahora seguimos con valores por defecto, luego los calcularemos dinámicamente
        findViewById<TextView>(R.id.tvReportCount).text = "0"
        findViewById<TextView>(R.id.tvRescueCount).text = "0"
        findViewById<TextView>(R.id.tvPointsCount).text = "100"
    }

    private fun setDefaultProfileData() {
        // Datos por defecto cuando no hay perfil extendido
        findViewById<TextView>(R.id.tvPhone).text = "No configurado"
        findViewById<TextView>(R.id.tvLocation).text = "Lima, Perú"

        // Stats por defecto
        findViewById<TextView>(R.id.tvReportCount).text = "0"
        findViewById<TextView>(R.id.tvRescueCount).text = "0"
        findViewById<TextView>(R.id.tvPointsCount).text = "100"
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
        return format.format(date)
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}