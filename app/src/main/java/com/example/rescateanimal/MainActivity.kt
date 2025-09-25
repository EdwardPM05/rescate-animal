package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var navigationHelper: NavigationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load user data
        loadUserData()

        // Setup navigation (comentar si tienes conflictos)
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando regresamos de otras actividades
        loadUserData()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val tvWelcomeUser = findViewById<TextView>(R.id.tvWelcomeUser)

            // Cargar datos básicos del usuario
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val displayName = document.getString("displayName") ?: "Usuario"
                        tvWelcomeUser.text = "¡Hola, $displayName!"
                    } else {
                        tvWelcomeUser.text = "¡Hola, ${currentUser.email?.split("@")?.get(0)}!"
                    }
                }
                .addOnFailureListener {
                    tvWelcomeUser.text = "¡Hola, Usuario!"
                }

            // Aquí puedes agregar más datos si necesitas mostrar stats dinámicas
            // Por ejemplo, cargar número de reportes reales, etc.
        }
    }

    private fun setupBottomNavigation() {
        // Inicio - Ya estamos aquí (resaltar como activo)
        findViewById<LinearLayout>(R.id.navInicio).setOnClickListener {
            // Ya estamos en inicio, no hacer nada
        }

        // Mapa
        findViewById<LinearLayout>(R.id.navMapa).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // Reportar - FUNCIONA CON ReportActivity
        findViewById<LinearLayout>(R.id.navReportar).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        // Adoptar - ACTUALIZADO para navegar a AdoptActivity
        findViewById<LinearLayout>(R.id.navAdoptar).setOnClickListener {
            startActivity(Intent(this, AdoptActivity::class.java))
        }

        // Perfil - FUNCIONA CORRECTAMENTE
        findViewById<LinearLayout>(R.id.navPerfil).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}