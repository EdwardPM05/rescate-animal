package com.example.rescateanimal

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

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
            showToast("Reportar - Próximamente")
        }

        navAdoptar.setOnClickListener {
            showToast("Adoptar - Próximamente")
        }

        navPerfil.setOnClickListener {
            showToast("Perfil - Próximamente")
        }
    }

    private fun navigateToActivity(targetActivity: Class<*>) {
        val intent = Intent(activity, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivity(intent)
    }

    private fun setSelectedTab(selectedTab: LinearLayout) {
        // Reset all tabs
        val tabs = listOf(
            activity.findViewById<LinearLayout>(R.id.navInicio),
            activity.findViewById<LinearLayout>(R.id.navMapa),
            activity.findViewById<LinearLayout>(R.id.navReportar),
            activity.findViewById<LinearLayout>(R.id.navAdoptar),
            activity.findViewById<LinearLayout>(R.id.navPerfil)
        )

        tabs.forEach { tab ->
            val textView = tab.getChildAt(1) as TextView
            textView.setTextColor(activity.getColor(R.color.text_secondary))
        }

        // Set selected tab
        val selectedTextView = selectedTab.getChildAt(1) as TextView
        selectedTextView.setTextColor(activity.getColor(R.color.primary_orange))
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}