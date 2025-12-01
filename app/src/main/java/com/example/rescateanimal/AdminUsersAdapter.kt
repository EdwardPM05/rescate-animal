package com.example.rescateanimal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class AdminUsersAdapter(
    private val usersList: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvInitial: TextView = view.findViewById(R.id.tvUserInitial)
        val tvRole: TextView = view.findViewById(R.id.tvUserRole)
        val cvRoleBadge: CardView = view.findViewById(R.id.cvRoleBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Usamos el nuevo diseño bonito
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = usersList[position]

        holder.tvName.text = if (user.fullName.isNotEmpty()) user.fullName else "Sin Nombre"
        holder.tvEmail.text = user.email

        // Poner la inicial del nombre en el círculo
        val initial = if (user.fullName.isNotEmpty()) user.fullName.substring(0, 1).uppercase() else "?"
        holder.tvInitial.text = initial

        // Configurar el Badge del Rol con colores
        holder.tvRole.text = formatRoleName(user.role)

        when (user.role) {
            "admin" -> {
                holder.cvRoleBadge.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Rojo claro
                holder.tvRole.setTextColor(Color.parseColor("#D32F2F")) // Rojo oscuro
            }
            "veterinaria_verificada" -> {
                holder.cvRoleBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Verde claro
                holder.tvRole.setTextColor(Color.parseColor("#388E3C")) // Verde oscuro
            }
            "bloqueado" -> {
                holder.cvRoleBadge.setCardBackgroundColor(Color.parseColor("#212121")) // Negro
                holder.tvRole.setTextColor(Color.parseColor("#FFFFFF")) // Blanco
            }
            else -> {
                holder.cvRoleBadge.setCardBackgroundColor(Color.parseColor("#F5F5F5")) // Gris
                holder.tvRole.setTextColor(Color.parseColor("#757575")) // Gris oscuro
            }
        }

        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    private fun formatRoleName(role: String): String {
        return when(role) {
            "veterinaria_verificada" -> "VETERINARIA"
            "albergue_verificado" -> "ALBERGUE"
            "tienda_verificada" -> "TIENDA"
            else -> role.uppercase()
        }
    }

    override fun getItemCount() = usersList.size
}