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

        // Setup navigation
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()

        // Setup bottom navigation specifically for this activity
        setupBottomNavigation()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val tvWelcomeUser = findViewById<TextView>(R.id.tvWelcomeUser)

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

        // Reportar
        findViewById<LinearLayout>(R.id.navReportar).setOnClickListener {
            Toast.makeText(this, "Reportar - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Adoptar
        findViewById<LinearLayout>(R.id.navAdoptar).setOnClickListener {
            Toast.makeText(this, "Adoptar - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Perfil - NUEVA FUNCIONALIDAD
        findViewById<LinearLayout>(R.id.navPerfil).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}