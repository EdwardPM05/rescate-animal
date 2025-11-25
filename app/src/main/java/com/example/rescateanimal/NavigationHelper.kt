package com.example.rescateanimal

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class NavigationHelper(private val activity: Activity) {

    fun setupBottomNavigation() {
        val navInicio = activity.findViewById<LinearLayout>(R.id.navInicio)
        val navMapa = activity.findViewById<LinearLayout>(R.id.navMapa)
        val navReportar = activity.findViewById<LinearLayout>(R.id.navReportar)
        val navAdoptar = activity.findViewById<LinearLayout>(R.id.navAdoptar)
        val navPerfil = activity.findViewById<LinearLayout>(R.id.navPerfil)

        // Determinar cuál tab debe estar seleccionado basado en la activity actual
        when (activity::class.java.simpleName) {
            "MainActivity" -> setSelectedTab(navInicio)
            "MapActivity" -> setSelectedTab(navMapa)
            "ReportActivity" -> setSelectedTab(navReportar)
            "AdoptActivity" -> setSelectedTab(navAdoptar)
            "ProfileActivity" -> setSelectedTab(navPerfil)
        }

        navInicio.setOnClickListener {
            if (activity !is MainActivity) {
                navigateToActivity(MainActivity::class.java)
            }
        }

        navMapa.setOnClickListener {
            if (activity !is MapActivity) {
                navigateToActivity(MapActivity::class.java)
            }
        }

        navReportar.setOnClickListener {
            if (activity !is ReportActivity) {
                navigateToActivity(ReportActivity::class.java)
            }
        }

        navAdoptar.setOnClickListener {
            if (activity !is AdoptActivity) {
                navigateToActivity(AdoptActivity::class.java)
            }
        }

        navPerfil.setOnClickListener {
            if (activity !is ProfileActivity) {
                navigateToActivity(ProfileActivity::class.java)
            }
        }
    }

    private fun navigateToActivity(targetActivity: Class<*>) {
        val intent = Intent(activity, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0) // Sin animación
    }

    private fun setSelectedTab(selectedTab: LinearLayout) {
        // Lista de todos los tabs con sus IDs
        val tabs = listOf(
            activity.findViewById<LinearLayout>(R.id.navInicio),
            activity.findViewById<LinearLayout>(R.id.navMapa),
            activity.findViewById<LinearLayout>(R.id.navReportar),
            activity.findViewById<LinearLayout>(R.id.navAdoptar),
            activity.findViewById<LinearLayout>(R.id.navPerfil)
        )

        // Reset all tabs (color gris)
        tabs.forEach { tab ->
            tab?.let {
                // Cambiar color del ImageView (icono)
                val iconView = it.getChildAt(0) as? ImageView
                iconView?.setColorFilter(
                    ContextCompat.getColor(activity, R.color.text_secondary)
                )

                // Cambiar color y estilo del TextView (texto)
                val textView = it.getChildAt(1) as? TextView
                textView?.apply {
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        // Set selected tab (color naranja y bold)
        selectedTab.let {
            // Cambiar color del ImageView seleccionado
            val selectedIcon = it.getChildAt(0) as? ImageView
            selectedIcon?.setColorFilter(
                ContextCompat.getColor(activity, R.color.primary_orange)
            )

            // Cambiar color y estilo del TextView seleccionado
            val selectedTextView = it.getChildAt(1) as? TextView
            selectedTextView?.apply {
                setTextColor(ContextCompat.getColor(activity, R.color.primary_orange))
                setTypeface(null, Typeface.BOLD)
            }
        }
    }
}