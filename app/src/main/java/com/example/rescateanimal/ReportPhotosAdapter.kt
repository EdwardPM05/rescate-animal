package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop

class ReportPhotosAdapter(
    private val photoUrls: List<String>
) : RecyclerView.Adapter<ReportPhotosAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)

        fun bind(photoUrl: String) {
            android.util.Log.d("PhotoAdapter", "Cargando URL: $photoUrl")

            Glide.with(itemView.context)
                .load(photoUrl)
                .transform(CenterCrop())
                .placeholder(R.drawable.ic_pet_placeholder)
                .error(R.drawable.ic_pet_placeholder)
                .into(ivPhoto)

            // Click para ver en pantalla completa
            ivPhoto.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(photoUrl))
                    itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("PhotoAdapter", "Error al abrir imagen: ${e.message}")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoUrls[position])
    }

    override fun getItemCount() = photoUrls.size
}