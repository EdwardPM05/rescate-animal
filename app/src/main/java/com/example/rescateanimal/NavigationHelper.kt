package com.example.rescateanimal

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class NavigationHelper(private val activity: Activity) {

    private val roleManager = RoleManager(activity)
    private val TAG = "NavigationHelper"

    /**
     * Configura la navegación según el rol del usuario
     */
    fun setupBottomNavigation() {
        val currentRole = roleManager.getCurrentRole()

        when (currentRole) {
            RoleManager.ROLE_ADMIN -> setupAdminNavigation()
            RoleManager.ROLE_PARTNER -> setupPartnerNavigation()
            RoleManager.ROLE_USER -> setupUserNavigation()
            else -> setupUserNavigation()
        }
    }

    // ========== NAVEGACIÓN PARA USUARIO NORMAL ==========
    private fun setupUserNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Asegurarse que todos los items estén visibles (en caso de venir de otro rol)
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        navReportar?.visibility = View.VISIBLE
        navAdoptar?.visibility = View.VISIBLE

        // Restaurar iconos y textos de usuario normal
        updateNavItem(R.id.navInicio, R.drawable.nav_home, "Inicio")
        updateNavItem(R.id.navMapa, R.drawable.nav_map, "Mapa")
        updateNavItem(R.id.navReportar, R.drawable.nav_report, "Reportar")
        updateNavItem(R.id.navAdoptar, R.drawable.nav_adopt, "Adoptar")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        // Mostrar/ocultar botón de cambio de rol
        val canSwitchRole = roleManager.canSwitchRole()

        if (canSwitchRole && navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 6f

            navRol.setOnClickListener {
                showRoleSelectorDialog()
            }
        } else if (navRol != null) {
            navRol.visibility = View.GONE
            bottomNavLayout.weightSum = 5f
        }

        setupUserNavigationListeners()
        setSelectedTab()
    }

    private fun setupUserNavigationListeners() {
        val navInicio = activity.findViewById<LinearLayout>(R.id.navInicio)
        val navMapa = activity.findViewById<LinearLayout>(R.id.navMapa)
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        val navPerfil = activity.findViewById<LinearLayout>(R.id.navPerfil)

        navInicio?.setOnClickListener {
            if (activity !is MainActivity) {
                navigateToActivity(MainActivity::class.java)
            }
        }

        navMapa?.setOnClickListener {
            if (activity !is MapActivity) {
                navigateToActivity(MapActivity::class.java)
            }
        }

        navReportar?.setOnClickListener {
            if (activity !is ReportActivity) {
                navigateToActivity(ReportActivity::class.java)
            }
        }

        navAdoptar?.setOnClickListener {
            if (activity !is AdoptActivity) {
                navigateToActivity(AdoptActivity::class.java)
            }
        }

        navPerfil?.setOnClickListener {
            if (activity !is ProfileActivity) {
                navigateToActivity(ProfileActivity::class.java)
            }
        }
    }

    // ========== NAVEGACIÓN PARA ADMIN ==========
    private fun setupAdminNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Asegurarse que todos los items estén visibles
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        navReportar?.visibility = View.VISIBLE
        navAdoptar?.visibility = View.VISIBLE

        // Cambiar iconos y textos para modo admin
        updateNavItem(R.id.navInicio, R.drawable.ic_metrics, "Métricas")
        updateNavItem(R.id.navMapa, R.drawable.ic_reports_review, "Reportes")
        updateNavItem(R.id.navReportar, R.drawable.ic_affiliates, "Afiliados")
        updateNavItem(R.id.navAdoptar, R.drawable.ic_usuarios, "Usuarios")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        // Mostrar botón de rol para poder cambiar
        if (navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 6f

            navRol.setOnClickListener {
                showRoleSelectorDialog()
            }
        }

        setupAdminNavigationListeners()
        setSelectedTab()
    }

    private fun setupAdminNavigationListeners() {
        val navInicio = activity.findViewById<LinearLayout>(R.id.navInicio)
        val navMapa = activity.findViewById<LinearLayout>(R.id.navMapa)
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        val navPerfil = activity.findViewById<LinearLayout>(R.id.navPerfil)

        // Métricas (Dashboard de Admin)
        navInicio?.setOnClickListener {
            if (activity !is AdminMetricsActivity) {
                navigateToActivity(AdminMetricsActivity::class.java)
            }
        }

        // Revisión de Reportes
        navMapa?.setOnClickListener {
            if (activity !is AdminReportsActivity) {
                navigateToActivity(AdminReportsActivity::class.java)
            }
        }

        // Gestión de Afiliaciones
        navReportar?.setOnClickListener {
            if (activity !is AdminAffiliatesActivity) {
                navigateToActivity(AdminAffiliatesActivity::class.java)
            }
        }

        // Gestión de Usuarios
        navAdoptar?.setOnClickListener {
            if (activity !is AdminUsersActivity) {
                navigateToActivity(AdminUsersActivity::class.java)
            }
        }

        // Perfil (mismo para todos)
        navPerfil?.setOnClickListener {
            if (activity !is ProfileActivity) {
                navigateToActivity(ProfileActivity::class.java)
            }
        }
    }

    // ========== NAVEGACIÓN PARA PARTNER ==========
    private fun setupPartnerNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Partner solo usa: Inicio (Mis Adopciones), Mapa, Perfil
        // Ocultar "Reportar" y "Adoptar"
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)

        navReportar?.visibility = View.GONE
        navAdoptar?.visibility = View.GONE

        // Configurar iconos y textos para Partner
        updateNavItem(R.id.navInicio, R.drawable.ic_adopciones, "Adopciones")
        updateNavItem(R.id.navMapa, R.drawable.nav_map, "Mapa")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        // Mostrar botón de rol
        if (navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 4f // Solo 4 items: Adopciones, Mapa, Perfil, Rol
        }

        setupPartnerNavigationListeners()
        setSelectedTab()
    }

    private fun setupPartnerNavigationListeners() {
        val navInicio = activity.findViewById<LinearLayout>(R.id.navInicio)
        val navMapa = activity.findViewById<LinearLayout>(R.id.navMapa)
        val navPerfil = activity.findViewById<LinearLayout>(R.id.navPerfil)
        val navRol = activity.findViewById<LinearLayout>(R.id.navRol)

        // Mis Adopciones (Main de Partner)
        navInicio?.setOnClickListener {
            if (activity !is PartnerMainActivity) {
                navigateToActivity(PartnerMainActivity::class.java)
            }
        }

        // Mapa (CORREGIDO: Ahora navega a MapPartnerActivity)
        navMapa?.setOnClickListener {
            if (activity !is MapPartnerActivity) {
                navigateToActivity(MapPartnerActivity::class.java)
            }
        }

        // Perfil
        navPerfil?.setOnClickListener {
            if (activity !is ProfileActivity) {
                navigateToActivity(ProfileActivity::class.java)
            }
        }

        // Cambiar Rol
        navRol?.setOnClickListener {
            showRoleSelectorDialog()
        }
    }

    // ========== FUNCIONES AUXILIARES ==========

    private fun updateNavItem(navId: Int, iconRes: Int, text: String) {
        val navItem = activity.findViewById<LinearLayout>(navId)
        navItem?.let {
            val iconView = it.getChildAt(0) as? ImageView
            val textView = it.getChildAt(1) as? TextView

            iconView?.setImageResource(iconRes)
            textView?.text = text
        }
    }

    private fun navigateToActivity(targetActivity: Class<*>) {
        val intent = Intent(activity, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }

    private fun setSelectedTab() {
        val currentRole = roleManager.getCurrentRole()

        val tabs = listOf(
            activity.findViewById<LinearLayout>(R.id.navInicio),
            activity.findViewById<LinearLayout>(R.id.navMapa),
            activity.findViewById<LinearLayout>(R.id.navReportar),
            activity.findViewById<LinearLayout>(R.id.navAdoptar),
            activity.findViewById<LinearLayout>(R.id.navPerfil)
        )

        // Reset all tabs
        tabs.forEach { tab ->
            tab?.let {
                val iconView = it.getChildAt(0) as? ImageView
                iconView?.setColorFilter(
                    ContextCompat.getColor(activity, R.color.text_secondary)
                )

                val textView = it.getChildAt(1) as? TextView
                textView?.apply {
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        // Determinar tab seleccionado según rol y activity actual
        val selectedTab = when (currentRole) {
            RoleManager.ROLE_ADMIN -> getSelectedTabForAdmin()
            RoleManager.ROLE_PARTNER -> getSelectedTabForPartner()
            RoleManager.ROLE_USER -> getSelectedTabForUser()
            else -> null
        }

        // Marcar el tab seleccionado
        selectedTab?.let {
            val selectedIcon = it.getChildAt(0) as? ImageView
            selectedIcon?.setColorFilter(
                ContextCompat.getColor(activity, R.color.primary_orange)
            )

            val selectedTextView = it.getChildAt(1) as? TextView
            selectedTextView?.apply {
                setTextColor(ContextCompat.getColor(activity, R.color.primary_orange))
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun getSelectedTabForUser(): LinearLayout? {
        return when (activity::class.java.simpleName) {
            "MainActivity" -> activity.findViewById(R.id.navInicio)
            "MapActivity" -> activity.findViewById(R.id.navMapa)
            "ReportActivity" -> activity.findViewById(R.id.navReportar)
            "AdoptActivity" -> activity.findViewById(R.id.navAdoptar)
            "ProfileActivity" -> activity.findViewById(R.id.navPerfil)
            else -> null
        }
    }

    private fun getSelectedTabForAdmin(): LinearLayout? {
        return when (activity::class.java.simpleName) {
            "AdminMetricsActivity" -> activity.findViewById(R.id.navInicio)
            "AdminReportsActivity" -> activity.findViewById(R.id.navMapa)
            "AdminAffiliatesActivity" -> activity.findViewById(R.id.navReportar)
            "AdminUsersActivity" -> activity.findViewById(R.id.navAdoptar)
            "ProfileActivity" -> activity.findViewById(R.id.navPerfil)
            else -> null
        }
    }

    private fun getSelectedTabForPartner(): LinearLayout? {
        return when (activity::class.java.simpleName) {
            "PartnerMainActivity" -> activity.findViewById(R.id.navInicio)
            "MapPartnerActivity" -> activity.findViewById(R.id.navMapa) // ✅ CORREGIDO
            "ProfileActivity" -> activity.findViewById(R.id.navPerfil)
            else -> null
        }
    }

    private fun showRoleSelectorDialog() {
        try {
            val dialog = RoleSelectorDialog(activity, roleManager) { newRole ->
                Toast.makeText(
                    activity,
                    "Cambiando a modo: ${roleManager.getRoleDisplayName(newRole)}",
                    Toast.LENGTH_SHORT
                ).show()

                // Redirigir al RoleDispatcherActivity que se encarga de mandar a la Activity correcta
                redirectToRoleHome()
            }
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}", e)
            Toast.makeText(activity, "Error al abrir selector de rol", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Redirige al RoleDispatcherActivity que mandará al usuario a la Activity correcta
     */
    private fun redirectToRoleHome() {
        val currentRole = roleManager.getCurrentRole()

        // Ir directamente a la activity correspondiente
        val intent = when (currentRole) {
            RoleManager.ROLE_USER -> Intent(activity, MainActivity::class.java)
            RoleManager.ROLE_PARTNER -> Intent(activity, PartnerMainActivity::class.java)
            RoleManager.ROLE_ADMIN -> Intent(activity, AdminMetricsActivity::class.java)
            else -> Intent(activity, MainActivity::class.java)
        }

        // Limpiar el back stack pero sin cerrar toda la app
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        activity.startActivity(intent)

        // Transición suave
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        activity.finish()
    }

    // ========== FUNCIONES PÚBLICAS ==========

    /**
     * Verifica si el usuario puede cambiar de rol
     */
    fun canUserSwitchRole(): Boolean {
        return roleManager.canSwitchRole()
    }

    /**
     * Obtiene el rol actual del usuario
     */
    fun getCurrentRole(): String {
        return roleManager.getCurrentRole()
    }

    /**
     * Muestra el selector de roles (para usar desde el perfil)
     */
    fun showRoleSwitcher() {
        if (!roleManager.canSwitchRole()) {
            Toast.makeText(
                activity,
                "No tienes roles adicionales disponibles",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        showRoleSelectorDialog()
    }
}