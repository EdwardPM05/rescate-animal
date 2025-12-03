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
     * Configura la navegación según el rol actual del usuario.
     * Llama a la función específica (Admin, Partner, User) dependiendo del rol.
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

    // ==========================================
    // 1. NAVEGACIÓN PARA USUARIO NORMAL
    // ==========================================
    private fun setupUserNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Asegurarse de que los items ocultos en otros roles estén visibles aquí
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        navReportar?.visibility = View.VISIBLE
        navAdoptar?.visibility = View.VISIBLE

        // Restaurar iconos y textos estándar
        updateNavItem(R.id.navInicio, R.drawable.nav_home, "Inicio")
        updateNavItem(R.id.navMapa, R.drawable.nav_map, "Mapa")
        updateNavItem(R.id.navReportar, R.drawable.nav_report, "Reportar")
        updateNavItem(R.id.navAdoptar, R.drawable.nav_adopt, "Adoptar")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        // LÓGICA DEL BOTÓN ROL:
        // Si el usuario tiene múltiples roles (ej: es admin pero está viendo como usuario), mostramos el botón.
        val canSwitchRole = roleManager.canSwitchRole()

        if (canSwitchRole && navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 6f // 6 Botones visibles

            navRol.setOnClickListener {
                showRoleSelectorDialog()
            }
        } else if (navRol != null) {
            // Si es un usuario normal sin permisos extra, ocultamos el botón
            navRol.visibility = View.GONE
            bottomNavLayout.weightSum = 5f // 5 Botones visibles
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
            if (activity !is MainActivity) navigateToActivity(MainActivity::class.java)
        }
        navMapa?.setOnClickListener {
            if (activity !is MapActivity) navigateToActivity(MapActivity::class.java)
        }
        navReportar?.setOnClickListener {
            if (activity !is ReportActivity) navigateToActivity(ReportActivity::class.java)
        }
        navAdoptar?.setOnClickListener {
            if (activity !is AdoptActivity) navigateToActivity(AdoptActivity::class.java)
        }
        navPerfil?.setOnClickListener {
            if (activity !is ProfileActivity) navigateToActivity(ProfileActivity::class.java)
        }
    }

    // ==========================================
    // 2. NAVEGACIÓN PARA ADMINISTRADOR
    // ==========================================
    private fun setupAdminNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Asegurar visibilidad de todos los slots
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        navReportar?.visibility = View.VISIBLE
        navAdoptar?.visibility = View.VISIBLE

        // CAMBIAR ICONOS Y TEXTOS PARA EL PANEL DE ADMIN
        // Reutilizamos los IDs existentes pero les cambiamos la función visual
        updateNavItem(R.id.navInicio, R.drawable.ic_metrics, "Métricas")     // Slot 1: Métricas
        updateNavItem(R.id.navMapa, R.drawable.ic_reports_review, "Reportes") // Slot 2: Gestión Reportes
        updateNavItem(R.id.navReportar, R.drawable.ic_affiliates, "Afiliados") // Slot 3: Gestión Afiliados
        updateNavItem(R.id.navAdoptar, R.drawable.ic_usuarios, "Usuarios")   // Slot 4: Gestión Usuarios
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")       // Slot 5: Perfil

        // El Admin siempre puede cambiar de rol, así que mostramos el botón
        if (navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 6f // Ajustar espacio para 6 botones

            // Icono de Rol (opcional: asegurar que tenga el icono de settings/switch)
            val iconView = navRol.getChildAt(0) as? ImageView
            iconView?.setImageResource(android.R.drawable.ic_menu_rotate)

            navRol.setOnClickListener {
                showRoleSelectorDialog()
            }
        }

        setupAdminNavigationListeners()
        setSelectedTab()
    }

    private fun setupAdminNavigationListeners() {
        val navInicio = activity.findViewById<LinearLayout>(R.id.navInicio)   // Métricas
        val navMapa = activity.findViewById<LinearLayout>(R.id.navMapa)       // Reportes
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar) // Afiliados
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)   // Usuarios
        val navPerfil = activity.findViewById<LinearLayout>(R.id.navPerfil)     // Perfil

        navInicio?.setOnClickListener {
            if (activity !is AdminMetricsActivity) navigateToActivity(AdminMetricsActivity::class.java)
        }
        navMapa?.setOnClickListener {
            if (activity !is AdminReportsActivity) navigateToActivity(AdminReportsActivity::class.java)
        }
        navReportar?.setOnClickListener {
            if (activity !is AdminAffiliatesActivity) navigateToActivity(AdminAffiliatesActivity::class.java)
        }
        navAdoptar?.setOnClickListener {
            if (activity !is AdminUsersActivity) navigateToActivity(AdminUsersActivity::class.java)
        }
        navPerfil?.setOnClickListener {
            if (activity !is ProfileActivity) navigateToActivity(ProfileActivity::class.java)
        }
    }

    // ==========================================
    // 3. NAVEGACIÓN PARA PARTNER (Refugio/Veterinaria)
    // ==========================================
    private fun setupPartnerNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Partner usa menos botones: Ocultamos "Reportar" y "Adoptar" (slots 3 y 4)
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        navReportar?.visibility = View.GONE
        navAdoptar?.visibility = View.GONE

        // Configurar Textos/Iconos Partner
        updateNavItem(R.id.navInicio, R.drawable.ic_adopciones, "Mis Adopciones")
        updateNavItem(R.id.navMapa, R.drawable.nav_map, "Mapa")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        // Botón Rol Visible
        if (navRol != null) {
            navRol.visibility = View.VISIBLE
            // Calculamos el peso: 1(Inicio) + 1(Mapa) + 0(Oculto) + 0(Oculto) + 1(Perfil) + 1(Rol) = 4
            bottomNavLayout.weightSum = 4f

            navRol.setOnClickListener {
                showRoleSelectorDialog()
            }
        }

        setupPartnerNavigationListeners()
        setSelectedTab()
    }

    private fun setupPartnerNavigationListeners() {
        val navInicio = activity.findViewById<LinearLayout>(R.id.navInicio)
        val navMapa = activity.findViewById<LinearLayout>(R.id.navMapa)
        val navPerfil = activity.findViewById<LinearLayout>(R.id.navPerfil)

        navInicio?.setOnClickListener {
            if (activity !is PartnerMainActivity) navigateToActivity(PartnerMainActivity::class.java)
        }
        navMapa?.setOnClickListener {
            if (activity !is MapActivity) navigateToActivity(MapActivity::class.java)
        }
        navPerfil?.setOnClickListener {
            if (activity !is ProfileActivity) navigateToActivity(ProfileActivity::class.java)
        }
    }

    // ==========================================
    // FUNCIONES AUXILIARES
    // ==========================================

    /**
     * Cambia la imagen y el texto de un botón del menú dinámicamente
     */
    private fun updateNavItem(navId: Int, iconRes: Int, text: String) {
        try {
            val navItem = activity.findViewById<LinearLayout>(navId)
            navItem?.let {
                val iconView = it.getChildAt(0) as? ImageView
                val textView = it.getChildAt(1) as? TextView

                // Si no tienes los iconos específicos, usa uno por defecto para evitar crash
                if (iconRes != 0) {
                    iconView?.setImageResource(iconRes)
                }
                textView?.text = text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating nav item: ${e.message}")
        }
    }

    private fun navigateToActivity(targetActivity: Class<*>) {
        val intent = Intent(activity, targetActivity)
        // Flags para limpiar la pila y evitar abrir múltiples instancias
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivity(intent)
        // Eliminar animación para que parezca un cambio de tab
        activity.overridePendingTransition(0, 0)
    }

    /**
     * Resalta el icono y texto de la pantalla actual
     */
    private fun setSelectedTab() {
        val currentRole = roleManager.getCurrentRole()

        // Lista de todos los tabs posibles
        val tabs = listOf(
            activity.findViewById<LinearLayout>(R.id.navInicio),
            activity.findViewById<LinearLayout>(R.id.navMapa),
            activity.findViewById<LinearLayout>(R.id.navReportar),
            activity.findViewById<LinearLayout>(R.id.navAdoptar),
            activity.findViewById<LinearLayout>(R.id.navPerfil),
            activity.findViewById<LinearLayout>(R.id.navRol)
        )

        // 1. Resetear estilo de todos (gris)
        tabs.forEach { tab ->
            tab?.let {
                val iconView = it.getChildAt(0) as? ImageView
                val textView = it.getChildAt(1) as? TextView

                val grayColor = ContextCompat.getColor(activity, android.R.color.darker_gray)
                iconView?.setColorFilter(grayColor)
                textView?.setTextColor(grayColor)
                textView?.setTypeface(null, Typeface.NORMAL)
            }
        }

        // 2. Determinar cuál tab debe estar activo (Naranja)
        val selectedTab = when (currentRole) {
            RoleManager.ROLE_ADMIN -> getSelectedTabForAdmin()
            RoleManager.ROLE_PARTNER -> getSelectedTabForPartner()
            else -> getSelectedTabForUser() // ROLE_USER
        }

        // 3. Pintar el activo
        selectedTab?.let {
            val selectedIcon = it.getChildAt(0) as? ImageView
            val selectedText = it.getChildAt(1) as? TextView

            // Usa tu color primario (naranja). Si no tienes R.color.primary_orange, usa Color.parseColor("#FF6B35")
            val activeColor = try {
                ContextCompat.getColor(activity, R.color.primary_orange) // Asegúrate que este color exista en colors.xml
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#FF6B35") // Naranja por defecto
            }

            selectedIcon?.setColorFilter(activeColor)
            selectedText?.setTextColor(activeColor)
            selectedText?.setTypeface(null, Typeface.BOLD)
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
            "AdminMetricsActivity" -> activity.findViewById(R.id.navInicio) // Slot 1
            "AdminReportsActivity" -> activity.findViewById(R.id.navMapa)   // Slot 2
            "AdminAffiliatesActivity" -> activity.findViewById(R.id.navReportar) // Slot 3
            "AdminUsersActivity" -> activity.findViewById(R.id.navAdoptar)  // Slot 4
            "ProfileActivity" -> activity.findViewById(R.id.navPerfil)      // Slot 5
            else -> null
        }
    }

    private fun getSelectedTabForPartner(): LinearLayout? {
        return when (activity::class.java.simpleName) {
            "PartnerMainActivity" -> activity.findViewById(R.id.navInicio)
            "MapActivity" -> activity.findViewById(R.id.navMapa)
            "ProfileActivity" -> activity.findViewById(R.id.navPerfil)
            else -> null
        }
    }

    private fun showRoleSelectorDialog() {
        try {
            val dialog = RoleSelectorDialog(activity, roleManager) { newRole ->
                Toast.makeText(activity, "Cambiando modo...", Toast.LENGTH_SHORT).show()
                redirectToRoleHome()
            }
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}")
            Toast.makeText(activity, "Error al abrir selector", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToRoleHome() {
        val currentRole = roleManager.getCurrentRole()
        val intent = when (currentRole) {
            RoleManager.ROLE_USER -> Intent(activity, MainActivity::class.java)
            RoleManager.ROLE_PARTNER -> Intent(activity, PartnerMainActivity::class.java)
            RoleManager.ROLE_ADMIN -> Intent(activity, AdminMetricsActivity::class.java)
            else -> Intent(activity, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivity(intent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        activity.finish()
    }
}