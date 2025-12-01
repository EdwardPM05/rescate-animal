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

    fun setupBottomNavigation() {
        val currentMode = roleManager.getCurrentRole()

        when (currentMode) {
            RoleManager.MODE_ADMIN -> setupAdminNavigation()
            RoleManager.MODE_PARTNER -> setupPartnerNavigation()
            RoleManager.MODE_USER -> setupUserNavigation()
            else -> setupUserNavigation()
        }
    }

    // --- MODO USUARIO (Incluye Tiendas Verificadas) ---
    private fun setupUserNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Restaurar visibilidad
        activity.findViewById<LinearLayout>(R.id.navReportar)?.visibility = View.VISIBLE
        activity.findViewById<LinearLayout>(R.id.navAdoptar)?.visibility = View.VISIBLE

        updateNavItem(R.id.navInicio, R.drawable.nav_home, "Inicio")
        updateNavItem(R.id.navMapa, R.drawable.nav_map, "Mapa")
        updateNavItem(R.id.navReportar, R.drawable.nav_report, "Reportar")
        updateNavItem(R.id.navAdoptar, R.drawable.nav_adopt, "Adoptar")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        // LÓGICA CLAVE: Si es Tienda, canSwitchRole() devuelve falso -> Botón oculto
        val canSwitch = roleManager.canSwitchRole()

        if (canSwitch && navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 6f
            navRol.setOnClickListener { showRoleSelectorDialog() }
        } else if (navRol != null) {
            navRol.visibility = View.GONE
            bottomNavLayout.weightSum = 5f
        }

        setupUserNavigationListeners()
        setSelectedTab()
    }

    // --- MODO ADMIN ---
    private fun setupAdminNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        updateNavItem(R.id.navInicio, R.drawable.ic_metrics, "Métricas")
        updateNavItem(R.id.navMapa, R.drawable.ic_reports_review, "Reportes")
        updateNavItem(R.id.navReportar, R.drawable.ic_affiliates, "Afiliados")
        updateNavItem(R.id.navAdoptar, R.drawable.ic_usuarios, "Usuarios")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        if (navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 6f
            navRol.setOnClickListener { showRoleSelectorDialog() }
        }

        setupAdminNavigationListeners()
        setSelectedTab()
    }

    // --- MODO PARTNER (Solo Vet/Albergue) ---
    private fun setupPartnerNavigation() {
        val bottomNavLayout = activity.findViewById<LinearLayout>(R.id.bottomNavigation)
        val navRol = bottomNavLayout.findViewById<LinearLayout>(R.id.navRol)

        // Ocultar tabs irrelevantes para gestión
        activity.findViewById<LinearLayout>(R.id.navReportar)?.visibility = View.GONE
        activity.findViewById<LinearLayout>(R.id.navAdoptar)?.visibility = View.GONE

        updateNavItem(R.id.navInicio, R.drawable.ic_adopciones, "Gestión") // O Adopciones
        updateNavItem(R.id.navMapa, R.drawable.ic_map, "Mapa")
        updateNavItem(R.id.navPerfil, R.drawable.nav_profile, "Perfil")

        if (navRol != null) {
            navRol.visibility = View.VISIBLE
            bottomNavLayout.weightSum = 4f
            navRol.setOnClickListener { showRoleSelectorDialog() }
        }

        setupPartnerNavigationListeners()
        setSelectedTab()
    }

    // --- LISTENERS SIMPLIFICADOS ---
    private fun setupUserNavigationListeners() {
        setListener(R.id.navInicio, MainActivity::class.java)
        setListener(R.id.navMapa, MapActivity::class.java)
        setListener(R.id.navReportar, ReportActivity::class.java)
        setListener(R.id.navAdoptar, AdoptActivity::class.java)
        setListener(R.id.navPerfil, ProfileActivity::class.java)
    }

    private fun setupAdminNavigationListeners() {
        setListener(R.id.navInicio, AdminMetricsActivity::class.java)
        setListener(R.id.navMapa, AdminReportsActivity::class.java)
        setListener(R.id.navReportar, AdminAffiliatesActivity::class.java)
        setListener(R.id.navAdoptar, AdminUsersActivity::class.java)
        setListener(R.id.navPerfil, ProfileActivity::class.java)
    }

    private fun setupPartnerNavigationListeners() {
        setListener(R.id.navInicio, PartnerMainActivity::class.java)
        setListener(R.id.navMapa, MapActivity::class.java)
        setListener(R.id.navPerfil, ProfileActivity::class.java)
    }

    // --- HELPERS ---
    private fun setListener(viewId: Int, target: Class<*>) {
        activity.findViewById<View>(viewId)?.setOnClickListener {
            if (activity.javaClass != target) {
                val intent = Intent(activity, target)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    private fun updateNavItem(navId: Int, iconRes: Int, text: String) {
        val navItem = activity.findViewById<LinearLayout>(navId) ?: return
        (navItem.getChildAt(0) as? ImageView)?.setImageResource(iconRes)
        (navItem.getChildAt(1) as? TextView)?.text = text
    }

    private fun showRoleSelectorDialog() {
        val dialog = RoleSelectorDialog(activity, roleManager) {
            val intent = when (roleManager.getCurrentRole()) {
                RoleManager.MODE_PARTNER -> Intent(activity, PartnerMainActivity::class.java)
                RoleManager.MODE_ADMIN -> Intent(activity, AdminMetricsActivity::class.java)
                else -> Intent(activity, MainActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
        dialog.show()
    }

    private fun setSelectedTab() {
        // Lógica simple para resaltar el tab actual
        val mode = roleManager.getCurrentRole()
        val currentClass = activity::class.java

        // Mapeo ID -> Actividad
        val userTabs = mapOf(
            R.id.navInicio to MainActivity::class.java,
            R.id.navMapa to MapActivity::class.java,
            R.id.navReportar to ReportActivity::class.java,
            R.id.navAdoptar to AdoptActivity::class.java,
            R.id.navPerfil to ProfileActivity::class.java
        )

        val adminTabs = mapOf(
            R.id.navInicio to AdminMetricsActivity::class.java,
            R.id.navMapa to AdminReportsActivity::class.java, // Reportes
            R.id.navReportar to AdminAffiliatesActivity::class.java, // Afiliados
            R.id.navAdoptar to AdminUsersActivity::class.java // Usuarios
        )

        val partnerTabs = mapOf(
            R.id.navInicio to PartnerMainActivity::class.java
        )

        // Resaltar
        val targetMap = when(mode) {
            RoleManager.MODE_ADMIN -> adminTabs
            RoleManager.MODE_PARTNER -> partnerTabs
            else -> userTabs
        }

        // Limpiar todos
        val allTabs = listOf(R.id.navInicio, R.id.navMapa, R.id.navReportar, R.id.navAdoptar, R.id.navPerfil)
        allTabs.forEach { id ->
            val item = activity.findViewById<LinearLayout>(id)
            (item?.getChildAt(0) as? ImageView)?.setColorFilter(ContextCompat.getColor(activity, R.color.text_secondary))
            (item?.getChildAt(1) as? TextView)?.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
            (item?.getChildAt(1) as? TextView)?.setTypeface(null, Typeface.NORMAL)
        }

        // Activar actual
        targetMap.forEach { (id, clazz) ->
            if (clazz == currentClass || (mode == RoleManager.MODE_PARTNER && currentClass == PartnerMainActivity::class.java && id == R.id.navInicio)) {
                val item = activity.findViewById<LinearLayout>(id)
                (item?.getChildAt(0) as? ImageView)?.setColorFilter(ContextCompat.getColor(activity, R.color.primary_orange))
                (item?.getChildAt(1) as? TextView)?.setTextColor(ContextCompat.getColor(activity, R.color.primary_orange))
                (item?.getChildAt(1) as? TextView)?.setTypeface(null, Typeface.BOLD)
            }
        }
        // Casos comunes (Mapa, Perfil)
        if (currentClass == MapActivity::class.java) highlight(R.id.navMapa)
        if (currentClass == ProfileActivity::class.java) highlight(R.id.navPerfil)
    }

    private fun highlight(id: Int) {
        val item = activity.findViewById<LinearLayout>(id)
        (item?.getChildAt(0) as? ImageView)?.setColorFilter(ContextCompat.getColor(activity, R.color.primary_orange))
        (item?.getChildAt(1) as? TextView)?.setTextColor(ContextCompat.getColor(activity, R.color.primary_orange))
        (item?.getChildAt(1) as? TextView)?.setTypeface(null, Typeface.BOLD)
    }
}