package com.example.rescateanimal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MyReportsAdapter(
    private val reports: List<Report>,
    private val onDeleteClick: (Report) -> Unit,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<MyReportsAdapter.ReportViewHolder>() {

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReportType: TextView = itemView.findViewById(R.id.tvReportType)
        val tvReportIcon: TextView = itemView.findViewById(R.id.tvReportIcon)
        val tvReportDate: TextView = itemView.findViewById(R.id.tvReportDate)
        val tvReportLocation: TextView = itemView.findViewById(R.id.tvReportLocation)
        val tvReportStatus: TextView = itemView.findViewById(R.id.tvReportStatus)
        val btnDelete: TextView = itemView.findViewById(R.id.btnDeleteReport)

        fun bind(report: Report) {
            // Set icon and type based on report type
            when (report.type) {
                "danger" -> {
                    tvReportIcon.text = "⚠️"
                    tvReportType.text = "Animal en peligro"
                }
                "lost" -> {
                    tvReportIcon.text = "💔"
                    tvReportType.text = "Animal perdido"
                }
                "abandoned" -> {
                    tvReportIcon.text = "🏠"
                    tvReportType.text = "Animal abandonado"
                }
                else -> {
                    tvReportIcon.text = "📍"
                    tvReportType.text = "Reporte"
                }
            }

            // Format date
            tvReportDate.text = formatDate(report.createdAt)

            // Location (first part of address)
            val locationText = report.address?.split(",")?.take(2)?.joinToString(", ") ?: "Ubicación no disponible"
            tvReportLocation.text = "📍 $locationText"

            // Status
            tvReportStatus.text = when (report.status) {
                "pending" -> "⏳ Pendiente"
                "in_progress" -> "🔄 En proceso"
                "resolved" -> "✅ Resuelto"
                else -> "⏳ Pendiente"
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