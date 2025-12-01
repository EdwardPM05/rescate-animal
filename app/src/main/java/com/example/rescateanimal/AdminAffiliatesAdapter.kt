package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
// IMPORTANTE: Importamos el modelo
import com.example.rescateanimal.data.models.Affiliate

class AdminAffiliatesAdapter(
    private val affiliatesList: List<Affiliate>,
    private val onAffiliateClick: (Affiliate) -> Unit
) : RecyclerView.Adapter<AdminAffiliatesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivAffiliatePhoto)
        val tvType: TextView = view.findViewById(R.id.tvAffiliateType)
        val tvName: TextView = view.findViewById(R.id.tvAffiliateName)
        val tvAddress: TextView = view.findViewById(R.id.tvAffiliateAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_affiliate_card, parent, false)

        val layoutParams = view.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        view.layoutParams = layoutParams

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val affiliate = affiliatesList[position]

        holder.tvName.text = affiliate.businessName
        holder.tvAddress.text = affiliate.address
        holder.tvType.text = when(affiliate.type) {
            "veterinaria" -> "🏥 Veterinaria"
            "tienda" -> "🛍️ Tienda"
            "albergue" -> "🏠 Albergue"
            else -> affiliate.type.replaceFirstChar { it.uppercase() }
        }

        if (affiliate.mainPhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(affiliate.mainPhotoUrl)
                .centerCrop()
                .into(holder.ivPhoto)
        }

        holder.itemView.setOnClickListener { onAffiliateClick(affiliate) }
    }

    override fun getItemCount() = affiliatesList.size
}