package com.example.rescateanimal

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class AnimalPhotosAdapter(
    private val photoUrls: List<String>
) : RecyclerView.Adapter<AnimalPhotosAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        fun bind(photoUrl: String) {
            android.util.Log.d("AnimalPhotoAdapter", "Cargando URL: $photoUrl")

            // Usar Glide para cargar imágenes remotas
            Glide.with(imageView.context)
                .load(photoUrl)
                .transform(CenterCrop(), RoundedCorners(16))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)

            // Click para ver imagen en tamaño completo
            imageView.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(photoUrl))
                    imageView.context.startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("AnimalPhotoAdapter", "Error al abrir imagen: ${e.message}")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400).apply {
                marginEnd = 16
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return PhotoViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoUrls[position])
    }

    override fun getItemCount() = photoUrls.size
}