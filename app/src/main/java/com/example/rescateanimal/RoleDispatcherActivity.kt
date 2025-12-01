package com.example.rescateanimal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth

class RoleDispatcherActivity : Activity() {

    private lateinit var roleManager: RoleManager
    private lateinit var auth: FirebaseAuth
    private val TAG = "RoleDispatcher"
    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        roleManager = RoleManager(this)

        if (auth.currentUser == null) {
            keepSplashOnScreen = false
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        roleManager.loadUserRolesFromFirestore { success ->
            keepSplashOnScreen = false
            redirectToRoleActivity()
        }
    }

    private fun redirectToRoleActivity() {
        val currentMode = roleManager.getCurrentRole()
        Log.d(TAG, "Modo actual: $currentMode")

        val intent = when (currentMode) {
            RoleManager.MODE_USER -> {
                // Tiendas y Usuarios normales van aquí
                Intent(this, MainActivity::class.java)
            }
            RoleManager.MODE_PARTNER -> {
                // Veterinarias y Albergues van aquí
                Intent(this, PartnerMainActivity::class.java)
            }
            RoleManager.MODE_ADMIN -> {
                // Admin va aquí
                Intent(this, AdminMetricsActivity::class.java)
            }
            else -> Intent(this, MainActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}