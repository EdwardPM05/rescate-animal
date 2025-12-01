package com.example.rescateanimal

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Clase para gestionar los roles de usuario en la aplicación
 * Mapea los roles de la BD (veterinaria, tienda...) a Modos de App (User, Partner, Admin)
 */
class RoleManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        // --- MODOS DE LA APP (Nuevos) ---
        const val MODE_USER = "mode_user"       // Interfaz normal
        const val MODE_PARTNER = "mode_partner" // Interfaz de gestión (Vet/Albergue)
        const val MODE_ADMIN = "mode_admin"     // Interfaz de Admin

        // --- COMPATIBILIDAD CON CÓDIGO ANTIGUO (Legacy) ---
        // Esto arregla los errores en MainActivity y PartnerMainActivity
        const val ROLE_USER = MODE_USER
        const val ROLE_PARTNER = MODE_PARTNER
        const val ROLE_ADMIN = MODE_ADMIN

        // --- ROLES EN FIRESTORE (Base de Datos) ---
        const val DB_ROLE_ADMIN = "admin"
        const val DB_ROLE_VET = "veterinaria_verificada"
        const val DB_ROLE_SHELTER = "albergue_verificado"
        const val DB_ROLE_STORE = "tienda_verificada"
        const val DB_ROLE_USER = "usuario"

        const val KEY_CURRENT_MODE = "current_mode"
        const val KEY_AVAILABLE_MODES = "available_modes"
    }

    /**
     * Obtiene el MODO actual activo del usuario
     */
    fun getCurrentRole(): String {
        return prefs.getString(KEY_CURRENT_MODE, MODE_USER) ?: MODE_USER
    }

    /**
     * Obtiene todos los modos disponibles
     */
    fun getAvailableRoles(): List<String> {
        val rolesString = prefs.getString(KEY_AVAILABLE_MODES, MODE_USER) ?: MODE_USER
        return rolesString.split(",").map { it.trim() }
    }

    /**
     * Cambia el modo activo
     */
    fun switchRole(newMode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val availableModes = getAvailableRoles()

        if (!availableModes.contains(newMode)) {
            onError("No tienes permisos para acceder a este modo")
            return
        }

        prefs.edit().putString(KEY_CURRENT_MODE, newMode).apply()

        onSuccess()
        val modeName = when(newMode) {
            MODE_ADMIN -> "Administrador"
            MODE_PARTNER -> "Gestión (Partner)"
            else -> "Usuario"
        }
        Toast.makeText(context, "Modo cambiado a: $modeName", Toast.LENGTH_SHORT).show()
    }

    /**
     * Carga los roles desde Firestore y decide qué modos habilitar
     */
    fun loadUserRolesFromFirestore(onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onComplete(false)
            return
        }

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val dbRole = document.getString("role") ?: DB_ROLE_USER

                    val availableModes = mutableListOf(MODE_USER) // Todos tienen modo usuario
                    var defaultMode = MODE_USER

                    when (dbRole) {
                        DB_ROLE_ADMIN -> {
                            availableModes.add(MODE_PARTNER)
                            availableModes.add(MODE_ADMIN)
                            defaultMode = MODE_ADMIN
                        }
                        DB_ROLE_VET, DB_ROLE_SHELTER -> {
                            availableModes.add(MODE_PARTNER)
                            defaultMode = MODE_PARTNER
                        }
                        DB_ROLE_STORE -> {
                            // Tiendas son usuarios normales visualmente
                            defaultMode = MODE_USER
                        }
                    }

                    val modesString = availableModes.joinToString(",")
                    val currentMode = getCurrentRole()

                    val editor = prefs.edit().putString(KEY_AVAILABLE_MODES, modesString)

                    if (currentMode.isEmpty() || !availableModes.contains(currentMode)) {
                        editor.putString(KEY_CURRENT_MODE, defaultMode)
                    }

                    editor.apply()
                    onComplete(true)
                } else {
                    prefs.edit()
                        .putString(KEY_AVAILABLE_MODES, MODE_USER)
                        .putString(KEY_CURRENT_MODE, MODE_USER)
                        .apply()
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun canSwitchRole(): Boolean {
        return getAvailableRoles().size > 1
    }

    fun getRoleDisplayName(mode: String): String {
        return when (mode) {
            MODE_USER -> "Usuario / Tienda"
            MODE_PARTNER -> "Gestión (Vet/Albergue)"
            MODE_ADMIN -> "Administrador"
            else -> "Usuario"
        }
    }

    // --- REINTEGRADO PARA CORREGIR ERROR EN DIÁLOGO ---
    fun getRoleDescription(mode: String): String {
        return when (mode) {
            MODE_USER -> "Acceso a funciones principales: mapa, reportes y adopciones."
            MODE_PARTNER -> "Panel de gestión para Veterinarias y Albergues."
            MODE_ADMIN -> "Control total y métricas de la plataforma."
            else -> ""
        }
    }
}