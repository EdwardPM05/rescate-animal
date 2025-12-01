package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * PartnerMainActivity - Main de Partners
 * Vista principal para Albergues y Veterinarias afiliadas
 * Muestra "Mis Adopciones" - los animales que el partner ha registrado
 */
class PartnerMainActivity : AppCompatActivity() {

    private lateinit var roleManager: RoleManager
    private lateinit var navigationHelper: NavigationHelper
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner_main)

        auth = FirebaseAuth.getInstance()
        roleManager = RoleManager(this)

        // Verificar que el rol sea PARTNER
        if (roleManager.getCurrentRole() != RoleManager.ROLE_PARTNER) {
            redirectToCorrectActivity()
            return
        }

        initViews()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        if (roleManager.getCurrentRole() != RoleManager.ROLE_PARTNER) {
            redirectToCorrectActivity()
        }
    }

    private fun redirectToCorrectActivity() {
        val intent = Intent(this, RoleDispatcherActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        findViewById<TextView>(R.id.tvPartnerWelcome)?.apply {
            text = "Mis Adopciones"
        }

        findViewById<TextView>(R.id.tvPartnerDescription)?.apply {
            text = "Aquí podrás ver y gestionar los animales que has registrado para adopción.\n\n" +
                    "Esta sección está en desarrollo. Próximamente podrás:\n\n" +
                    "• Ver todos tus animales registrados\n" +
                    "• Editar información de cada animal\n" +
                    "• Marcar animales como adoptados\n" +
                    "• Ver estadísticas de adopciones"
        }
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}