package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.text.SimpleDateFormat
import java.util.*

class AdminReportsAdapter(
    private val reports: List<Report>,
    private val onDeleteClick: (Report) -> Unit,
    private val onItemClick: (Report) -> Unit,
    private val onStatusClick: (Report) -> Unit
) : RecyclerView.Adapter<AdminReportsAdapter.ReportViewHolder>() {

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivReportPhoto: ImageView = itemView.findViewById(R.id.ivReportPhoto)
        val vIconBackground: View = itemView.findViewById(R.id.vIconBackground)
        val ivReportIcon: ImageView = itemView.findViewById(R.id.ivReportIcon)
        val ivReportInfoIcon: ImageView = itemView.findViewById(R.id.ivReportInfoIcon)
        val tvReportType: TextView = itemView.findViewById(R.id.tvReportType)
        val tvReportInfo: TextView = itemView.findViewById(R.id.tvReportInfo)
        val tvReportDate: TextView = itemView.findViewById(R.id.tvReportDate)
        val tvReportLocation: TextView = itemView.findViewById(R.id.tvReportLocation)
        val llStatusBadge: LinearLayout = itemView.findViewById(R.id.llStatusBadge)
        val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        val tvReportStatus: TextView = itemView.findViewById(R.id.tvReportStatus)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteReport)
        val btnChangeStatus: ImageView = itemView.findViewById(R.id.btnChangeStatus)

        fun bind(report: Report) {
            // Load photo or show icon
            if (report.photoUrls.isNotEmpty() && report.photoUrls[0].isNotEmpty()) {
                ivReportPhoto.visibility = View.VISIBLE
                vIconBackground.visibility = View.GONE
                ivReportIcon.visibility = View.GONE

                Glide.with(itemView.context)
                    .load(report.photoUrls[0])
                    .transform(CenterCrop(), RoundedCorners(24))
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .error(R.drawable.ic_pet_placeholder)
                    .into(ivReportPhoto)
            } else {
                ivReportPhoto.visibility = View.GONE
                vIconBackground.visibility = View.VISIBLE
                ivReportIcon.visibility = View.VISIBLE
            }

            // Set icon and type based on report type
            when (report.type) {
                "danger" -> {
                    ivReportIcon.setImageResource(R.drawable.ic_warning)
                    ivReportInfoIcon.setImageResource(R.drawable.ic_warning)
                    vIconBackground.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.danger_light)
                    tvReportType.text = "Animal en peligro"
                    tvReportInfo.text = "Urgente"
                }
                "lost" -> {
                    ivReportIcon.setImageResource(R.drawable.ic_lost_pet)
                    ivReportInfoIcon.setImageResource(R.drawable.ic_lost_pet)
                    vIconBackground.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.lost_light)
                    tvReportType.text = "Animal perdido"
                    tvReportInfo.text = "Búsqueda activa"
                }
                "abandoned" -> {
                    ivReportIcon.setImageResource(R.drawable.ic_house)
                    ivReportInfoIcon.setImageResource(R.drawable.ic_house)
                    vIconBackground.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.abandoned_light)
                    tvReportType.text = "Animal abandonado"
                    tvReportInfo.text = "Necesita hogar"
                }
                else -> {
                    ivReportIcon.setImageResource(R.drawable.ic_pin)
                    ivReportInfoIcon.setImageResource(R.drawable.ic_pin)
                    vIconBackground.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.default_light)
                    tvReportType.text = "Reporte"
                    tvReportInfo.text = "Reporte general"
                }
            }

            // Format date
            tvReportDate.text = formatDate(report.createdAt)

            // Location
            val locationText = report.address?.split(",")?.take(2)?.joinToString(", ") ?: "Ubicación no disponible"
            tvReportLocation.text = locationText

            // Status
            when (report.status) {
                "pending" -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_pending)
                    tvReportStatus.text = "Pendiente"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_pending)
                }
                "in_progress" -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_in_progress)
                    tvReportStatus.text = "En proceso"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_in_progress)
                }
                "resolved" -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_check)
                    tvReportStatus.text = "Resuelto"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_resolved)
                }
                else -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_pending)
                    tvReportStatus.text = "Pendiente"
                    llStatusBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_pending)
                }
            }

            // Delete button
            btnDelete.setOnClickListener {
                onDeleteClick(report)
            }

            // Change status button
            btnChangeStatus.setOnClickListener {
                onStatusClick(report)
            }

            // Item click
            itemView.setOnClickListener {
                onItemClick(report)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_report, parent, false)
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
            days > 7 -> SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(date)
            days > 0 -> "Hace ${days.toInt()} día${if (days.toInt() > 1) "s" else ""}"
            hours > 0 -> "Hace ${hours.toInt()} hora${if (hours.toInt() > 1) "s" else ""}"
            minutes > 0 -> "Hace ${minutes.toInt()} minuto${if (minutes.toInt() > 1) "s" else ""}"
            else -> "Hace un momento"
        }
    }
}