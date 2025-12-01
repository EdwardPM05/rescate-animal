package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rescateanimal.data.models.Affiliate

class AdminAffiliatesAdapter(
    private val affiliatesList: List<Affiliate>,
    private val onAffiliateClick: (Affiliate) -> Unit
) : RecyclerView.Adapter<AdminAffiliatesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivAffiliatePhoto)
        val tvName: TextView = view.findViewById(R.id.tvAffiliateName)
        val tvType: TextView = view.findViewById(R.id.tvAffiliateType)
        val tvAddress: TextView = view.findViewById(R.id.tvAffiliateAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Asegúrate de que el XML se llame item_admin_affiliate_card.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_affiliate_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val affiliate = affiliatesList[position]

        holder.tvName.text = affiliate.businessName
        holder.tvAddress.text = affiliate.address
        holder.tvType.text = affiliate.type.uppercase()

        if (affiliate.mainPhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(affiliate.mainPhotoUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_image) // Usa un drawable existente
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_image)
        }

        // Clic en toda la tarjeta
        holder.itemView.setOnClickListener {
            onAffiliateClick(affiliate)
        }
    }

    override fun getItemCount() = affiliatesList.size
}