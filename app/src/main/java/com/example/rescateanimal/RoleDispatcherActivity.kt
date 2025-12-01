package com.example.rescateanimal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity que actúa como dispatcher/router
 * Redirige a la Activity correcta según el rol del usuario
 * IMPORTANTE: Ahora es el LAUNCHER y usa el Splash Screen nativo de Android 12+
 */
class RoleDispatcherActivity : Activity() {

    private lateinit var roleManager: RoleManager
    private lateinit var auth: FirebaseAuth
    private val TAG = "RoleDispatcherActivity"
    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar el splash screen ANTES de super.onCreate()
        val splashScreen = installSplashScreen()

        // Mantener el splash screen mientras cargamos
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)

        // NO establecer layout - esto es solo un dispatcher invisible

        auth = FirebaseAuth.getInstance()
        roleManager = RoleManager(this)

        Log.d(TAG, "onCreate - Iniciando dispatcher")

        // Verificar si el usuario está autenticado
        if (auth.currentUser == null) {
            Log.d(TAG, "Usuario no autenticado - redirigiendo a Login")
            keepSplashOnScreen = false
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Cargar roles desde Firestore
        Log.d(TAG, "Cargando roles desde Firestore")
        roleManager.loadUserRolesFromFirestore { success ->
            Log.d(TAG, "Roles cargados: $success")
            keepSplashOnScreen = false
            redirectToRoleActivity()
        }
    }

    /**
     * Redirige a la Activity correspondiente según el rol actual
     */
    private fun redirectToRoleActivity() {
        val currentRole = roleManager.getCurrentRole()
        Log.d(TAG, "Rol actual: $currentRole")

        val intent = when (currentRole) {
            RoleManager.ROLE_USER -> {
                Log.d(TAG, "Redirigiendo a MainActivity (USER)")
                Intent(this, MainActivity::class.java)
            }
            RoleManager.ROLE_PARTNER -> {
                Log.d(TAG, "Redirigiendo a PartnerMainActivity (PARTNER)")
                Intent(this, PartnerMainActivity::class.java)
            }
            RoleManager.ROLE_ADMIN -> {
                Log.d(TAG, "Redirigiendo a AdminMetricsActivity (ADMIN)")
                Intent(this, AdminMetricsActivity::class.java)
            }
            else -> {
                Log.d(TAG, "Rol desconocido, redirigiendo a MainActivity por defecto")
                Intent(this, MainActivity::class.java)
            }
        }

        // Usar flags más suaves que no cierran toda la app
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        Log.d(TAG, "Iniciando activity destino")
        startActivity(intent)

        // Sin animación para transición suave
        overridePendingTransition(0, 0)

        finish()
        Log.d(TAG, "Dispatcher finalizado")
    }
}