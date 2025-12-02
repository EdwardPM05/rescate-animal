package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AffiliateDetailPhotosAdapter(
    private val photos: List<PhotoWithCategory>
) : RecyclerView.Adapter<AffiliateDetailPhotosAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_affiliate_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        holder.bind(photo)
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)

        fun bind(photo: PhotoWithCategory) {
            // Cargar imagen con Glide
            Glide.with(itemView.context)
                .load(photo.url)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.ic_error)
                .centerCrop()
                .into(ivPhoto)

            // Mostrar categoría
            tvCategory.text = photo.category
        }
    }
}