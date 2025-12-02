package com.example.rescateanimal

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rescateanimal.data.models.Affiliate

class AdminAffiliatesAdapter(
    private val affiliatesList: List<Affiliate>,
    private val onEditClick: (Affiliate) -> Unit,
    private val onDeleteClick: (Affiliate) -> Unit
) : RecyclerView.Adapter<AdminAffiliatesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivAffiliatePhoto)
        val ivIcon: ImageView = view.findViewById(R.id.ivAffiliateIcon)
        val vIconBackground: View = view.findViewById(R.id.vIconBackground)
        val tvBusinessName: TextView = view.findViewById(R.id.tvAffiliateBusinessName)
        val tvType: TextView = view.findViewById(R.id.tvAffiliateType)
        val tvAddress: TextView = view.findViewById(R.id.tvAffiliateAddress)
        val tvContact: TextView = view.findViewById(R.id.tvAffiliateContact)
        val tvStatus: TextView = view.findViewById(R.id.tvAffiliateStatus)
        val tvVerified: TextView = view.findViewById(R.id.tvAffiliateVerified)
        val llStatusBadge: LinearLayout = view.findViewById(R.id.llStatusBadge)
        val ivStatusIcon: ImageView = view.findViewById(R.id.ivStatusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_affiliate, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val affiliate = affiliatesList[position]
        val context = holder.itemView.context

        // Business Name
        holder.tvBusinessName.text = affiliate.businessName

        // Type with emoji
        holder.tvType.text = when(affiliate.type) {
            "veterinaria" -> "🏥 Veterinaria"
            "tienda" -> "🛍️ Tienda"
            "albergue" -> "🏠 Albergue"
            else -> affiliate.type.replaceFirstChar { it.uppercase() }
        }

        // Address
        holder.tvAddress.text = affiliate.address

        // Contact - Manejo correcto de contactPerson
        holder.tvContact.text = if (affiliate.contactPerson.isNotEmpty()) {
            "${affiliate.contactPerson} · ${affiliate.phone}"
        } else {
            affiliate.phone
        }

        // Photo or Icon
        if (affiliate.mainPhotoUrl.isNotEmpty()) {
            holder.ivPhoto.visibility = View.VISIBLE
            holder.vIconBackground.visibility = View.GONE
            holder.ivIcon.visibility = View.GONE

            Glide.with(context)
                .load(affiliate.mainPhotoUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_negocio)
                .error(R.drawable.ic_negocio)
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.visibility = View.GONE
            holder.vIconBackground.visibility = View.VISIBLE
            holder.ivIcon.visibility = View.VISIBLE
        }

        // Status Badge
        when(affiliate.status) {
            "pending" -> {
                holder.tvStatus.text = "Pendiente"
                setStatusBadgeColor(holder.llStatusBadge, "#FF9800") // Orange
                holder.ivStatusIcon.setImageResource(R.drawable.ic_pending)
            }
            "approved" -> {
                holder.tvStatus.text = "Aprobado"
                setStatusBadgeColor(holder.llStatusBadge, "#4CAF50") // Green
                holder.ivStatusIcon.setImageResource(R.drawable.ic_check)
            }
            "rejected" -> {
                holder.tvStatus.text = "Rechazado"
                setStatusBadgeColor(holder.llStatusBadge, "#F44336") // Red
                holder.ivStatusIcon.setImageResource(R.drawable.ic_error)
            }
            else -> {
                holder.tvStatus.text = affiliate.status
                setStatusBadgeColor(holder.llStatusBadge, "#9E9E9E") // Gray
            }
        }

        // Verified Status
        if (affiliate.verified) {
            holder.tvVerified.text = "✓ Verificado"
            try {
                holder.tvVerified.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            } catch (e: Exception) {
                holder.tvVerified.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
        } else {
            holder.tvVerified.text = "⚠ Sin verificar"
            try {
                holder.tvVerified.setTextColor(ContextCompat.getColor(context, R.color.warning_orange))
            } catch (e: Exception) {
                holder.tvVerified.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            }
        }


        // Click en el item completo para abrir detalles
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AffiliateDetailActivity::class.java)
            intent.putExtra("AFFILIATE_ID", affiliate.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = affiliatesList.size

    private fun setStatusBadgeColor(view: LinearLayout, colorHex: String) {
        val drawable = GradientDrawable()
        drawable.cornerRadius = 12f
        drawable.setColor(android.graphics.Color.parseColor(colorHex))
        view.background = drawable
    }
}