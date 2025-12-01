package com.example.rescateanimal

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView // IMPORTANTE: Usamos MaterialCardView

class RoleSelectorDialog(
    context: Context,
    private val roleManager: RoleManager,
    private val onRoleSelected: (String) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_role_selector)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupUI()
    }

    private fun setupUI() {
        val container = findViewById<LinearLayout>(R.id.rolesContainer)
        val currentRole = roleManager.getCurrentRole()
        val availableRoles = roleManager.getAvailableRoles()

        container.removeAllViews()

        availableRoles.forEach { role ->
            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_role_selection, container, false)

            // AHORA USAMOS MaterialCardView AQUÍ
            val cvRole = itemView.findViewById<MaterialCardView>(R.id.cvRoleItem)
            val ivIcon = itemView.findViewById<ImageView>(R.id.ivRoleIcon)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvRoleTitle)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvRoleDescription)
            val ivCheck = itemView.findViewById<ImageView>(R.id.ivSelectedCheck)

            // Configurar textos e iconos
            tvTitle.text = roleManager.getRoleDisplayName(role)
            tvDesc.text = roleManager.getRoleDescription(role)

            val iconRes = when (role) {
                RoleManager.MODE_ADMIN -> R.drawable.ic_security
                RoleManager.MODE_PARTNER -> R.drawable.ic_store_business
                else -> R.drawable.ic_person_outline
            }
            ivIcon.setImageResource(iconRes)

            // Estado seleccionado (Ahora sí funcionan strokeColor y strokeWidth)
            if (role == currentRole) {
                cvRole.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_orange_light))
                cvRole.strokeColor = ContextCompat.getColor(context, R.color.primary_orange)
                cvRole.strokeWidth = 4
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary_orange))
                ivCheck.visibility = View.VISIBLE
            } else {
                cvRole.setCardBackgroundColor(Color.WHITE)
                cvRole.strokeColor = Color.LTGRAY
                cvRole.strokeWidth = 2
                ivIcon.setColorFilter(Color.GRAY)
                ivCheck.visibility = View.GONE
            }

            // Click listener
            itemView.setOnClickListener {
                if (role != currentRole) {
                    roleManager.switchRole(
                        newMode = role,
                        onSuccess = {
                            onRoleSelected(role)
                            dismiss()
                        },
                        onError = { error ->
                            // Manejar error si es necesario
                        }
                    )
                } else {
                    dismiss()
                }
            }

            container.addView(itemView)
        }

        findViewById<View>(R.id.btnCloseDialog)?.setOnClickListener {
            dismiss()
        }
    }
}