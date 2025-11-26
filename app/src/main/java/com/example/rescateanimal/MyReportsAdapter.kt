package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.text.SimpleDateFormat
import java.util.*

class MyReportsAdapter(
    private val reports: List<Report>,
    private val onDeleteClick: (Report) -> Unit,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<MyReportsAdapter.ReportViewHolder>() {

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivReportPhoto: ImageView = itemView.findViewById(R.id.ivReportPhoto)
        val tvReportIcon: TextView = itemView.findViewById(R.id.tvReportIcon)
        val tvReportType: TextView = itemView.findViewById(R.id.tvReportType)
        val tvReportInfo: TextView = itemView.findViewById(R.id.tvReportInfo)
        val tvReportDate: TextView = itemView.findViewById(R.id.tvReportDate)
        val tvReportLocation: TextView = itemView.findViewById(R.id.tvReportLocation)
        val tvReportStatus: TextView = itemView.findViewById(R.id.tvReportStatus)
        val btnDelete: TextView = itemView.findViewById(R.id.btnDeleteReport)

        fun bind(report: Report) {
            // Load photo or show icon
            if (report.photoUrls.isNotEmpty() && report.photoUrls[0].isNotEmpty()) {
                // Show photo
                tvReportIcon.visibility = View.GONE
                ivReportPhoto.visibility = View.VISIBLE

                android.util.Log.d("MyReportsAdapter", "Cargando foto: ${report.photoUrls[0]}")

                Glide.with(itemView.context)
                    .load(report.photoUrls[0])
                    .transform(CenterCrop(), RoundedCorners(24))
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .into(ivReportPhoto)
            } else {
                // Show icon
                ivReportPhoto.visibility = View.GONE
                tvReportIcon.visibility = View.VISIBLE
            }

            // Set icon and type based on report type
            when (report.type) {
                "danger" -> {
                    tvReportIcon.text = "⚠️"
                    tvReportType.text = "Animal en peligro"
                    tvReportInfo.text = "⚠️ Urgente"
                }
                "lost" -> {
                    tvReportIcon.text = "💔"
                    tvReportType.text = "Animal perdido"
                    tvReportInfo.text = "💔 Búsqueda activa"
                }
                "abandoned" -> {
                    tvReportIcon.text = "🏠"
                    tvReportType.text = "Animal abandonado"
                    tvReportInfo.text = "🏠 Necesita hogar"
                }
                else -> {
                    tvReportIcon.text = "📍"
                    tvReportType.text = "Reporte"
                    tvReportInfo.text = "📍 Reporte general"
                }
            }

            // Format date
            tvReportDate.text = formatDate(report.createdAt)

            // Location (first part of address)
            val locationText = report.address?.split(",")?.take(2)?.joinToString(", ") ?: "Ubicación no disponible"
            tvReportLocation.text = "📍 $locationText"

            // Status
            when (report.status) {
                "pending" -> {
                    tvReportStatus.text = "⏳ Pendiente"
                    tvReportStatus.setBackgroundResource(R.drawable.status_badge_pending)
                }
                "in_progress" -> {
                    tvReportStatus.text = "🔄 En proceso"
                    tvReportStatus.setBackgroundResource(R.drawable.status_badge_in_progress)
                }
                "resolved" -> {
                    tvReportStatus.text = "✅ Resuelto"
                    tvReportStatus.setBackgroundResource(R.drawable.status_badge_resolved)
                }
                else -> {
                    tvReportStatus.text = "⏳ Pendiente"
                    tvReportStatus.setBackgroundResource(R.drawable.status_badge_pending)
                }
            }

            // Delete button
            btnDelete.setOnClickListener {
                onDeleteClick(report)
            }

            // Item click
            itemView.setOnClickListener {
                onItemClick(report)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reports[position])
    }

    override fun getItemCount() = reports.size

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Fecha no disponible"

        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> {
                SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(date)
            }
            days > 0 -> "Hace ${days.toInt()} día${if (days.toInt() > 1) "s" else ""}"
            hours > 0 -> "Hace ${hours.toInt()} hora${if (hours.toInt() > 1) "s" else ""}"
            minutes > 0 -> "Hace ${minutes.toInt()} minuto${if (minutes.toInt() > 1) "s" else ""}"
            else -> "Hace un momento"
        }
    }
}