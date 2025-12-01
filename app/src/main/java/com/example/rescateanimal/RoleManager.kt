package com.example.rescateanimal

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Clase para gestionar los roles de usuario en la aplicación
 * Roles disponibles: user, partner, admin
 */
class RoleManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_PARTNER = "partner"
        const val ROLE_ADMIN = "admin"

        const val KEY_CURRENT_ROLE = "current_role"
        const val KEY_AVAILABLE_ROLES = "available_roles"
    }

    /**
     * Obtiene el rol actual activo del usuario
     */
    fun getCurrentRole(): String {
        return prefs.getString(KEY_CURRENT_ROLE, ROLE_USER) ?: ROLE_USER
    }

    /**
     * Obtiene todos los roles disponibles para el usuario
     * Almacenados como string separado por comas: "user,partner,admin"
     */
    fun getAvailableRoles(): List<String> {
        val rolesString = prefs.getString(KEY_AVAILABLE_ROLES, ROLE_USER) ?: ROLE_USER
        return rolesString.split(",").map { it.trim() }
    }

    /**
     * Cambia el rol activo del usuario (solo si está en sus roles disponibles)
     */
    fun switchRole(newRole: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val availableRoles = getAvailableRoles()

        if (!availableRoles.contains(newRole)) {
            onError("No tienes permisos para acceder a este rol")
            return
        }

        // Guardar el nuevo rol activo
        prefs.edit().putString(KEY_CURRENT_ROLE, newRole).apply()

        onSuccess()
        Toast.makeText(context, "Modo cambiado a: ${getRoleDisplayName(newRole)}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Carga los roles disponibles del usuario desde Firestore
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
                    // Obtener el rol principal del usuario desde Firestore
                    val userRoleFromFirestore = document.getString("role") ?: ROLE_USER

                    // Crear lista de roles disponibles
                    val availableRoles = mutableListOf(ROLE_USER)

                    // Agregar partner si tiene ese rol
                    if (userRoleFromFirestore == ROLE_PARTNER || userRoleFromFirestore == ROLE_ADMIN) {
                        availableRoles.add(ROLE_PARTNER)
                    }

                    // Agregar admin si tiene ese rol
                    if (userRoleFromFirestore == ROLE_ADMIN) {
                        availableRoles.add(ROLE_ADMIN)
                    }

                    // Guardar roles disponibles
                    val rolesString = availableRoles.joinToString(",")

                    // CRÍTICO: Solo actualizar availableRoles
                    // NO sobrescribir currentRole si ya existe
                    val currentRole = getCurrentRole()

                    prefs.edit()
                        .putString(KEY_AVAILABLE_ROLES, rolesString)
                        // Solo establecer currentRole si no existe o no está en availableRoles
                        .apply {
                            if (currentRole.isEmpty() || !availableRoles.contains(currentRole)) {
                                putString(KEY_CURRENT_ROLE, userRoleFromFirestore)
                            }
                        }
                        .apply()

                    onComplete(true)
                } else {
                    // Si no existe el documento, asignar rol user por defecto
                    prefs.edit()
                        .putString(KEY_AVAILABLE_ROLES, ROLE_USER)
                        .putString(KEY_CURRENT_ROLE, ROLE_USER)
                        .apply()
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    /**
     * Verifica si el usuario puede cambiar de rol (debe tener más de un rol disponible)
     */
    fun canSwitchRole(): Boolean {
        return getAvailableRoles().size > 1
    }

    /**
     * Obtiene el nombre para mostrar del rol
     */
    fun getRoleDisplayName(role: String): String {
        return when (role) {
            ROLE_USER -> "Usuario Normal"
            ROLE_PARTNER -> "Partner (Albergue/Veterinaria)"
            ROLE_ADMIN -> "Administrador"
            else -> "Usuario"
        }
    }

    /**
     * Obtiene la descripción del rol
     */
    fun getRoleDescription(role: String): String {
        return when (role) {
            ROLE_USER -> "Acceso a todas las funciones principales: reportar animales, buscar en el mapa, adopciones y perfil personal."
            ROLE_PARTNER -> "Vista para albergues y veterinarias: gestionar casos, colaborar con otros aliados y acceder al radar de ayuda."
            ROLE_ADMIN -> "Panel de administración: moderar reportes, aprobar afiliaciones y ver métricas de la plataforma."
            else -> ""
        }
    }

    /**
     * Limpia los datos de rol (útil al cerrar sesión)
     */
    fun clearRoleData() {
        prefs.edit()
            .remove(KEY_CURRENT_ROLE)
            .remove(KEY_AVAILABLE_ROLES)
            .apply()
    }
}