package com.example.rescateanimal

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore

// IMPORTS DE LOS GRÁFICOS
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar

class AdminMetricsActivity : AppCompatActivity() {

    private lateinit var navigationHelper: NavigationHelper
    private val firestore = FirebaseFirestore.getInstance()

    // TextViews de números
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalReports: TextView
    private lateinit var tvPendingReports: TextView
    private lateinit var tvActivePartners: TextView

    // Tarjetas (Cards)
    private lateinit var cardTotalUsers: MaterialCardView
    private lateinit var cardActivePartners: MaterialCardView
    private lateinit var cardTotalReports: MaterialCardView
    private lateinit var cardPendingReports: MaterialCardView

    // Gráficos
    private lateinit var lineChartWeekly: LineChart
    private lateinit var pieChartDistribution: PieChart

    // Lista de afiliados
    private lateinit var llTopAffiliates: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_metrics)

        initViews()
        setupNavigation()

        // Iniciar animaciones de entrada
        animateCardsEntrance()

        // Cargar datos REALES
        loadAllMetrics()
    }

    private fun initViews() {
        // Vincular Textos
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvTotalReports = findViewById(R.id.tvTotalReports)
        tvPendingReports = findViewById(R.id.tvPendingReports)
        tvActivePartners = findViewById(R.id.tvActivePartners)

        // Vincular Tarjetas
        cardTotalUsers = findViewById(R.id.cardTotalUsers)
        cardActivePartners = findViewById(R.id.cardActivePartners)
        cardTotalReports = findViewById(R.id.cardTotalReports)
        cardPendingReports = findViewById(R.id.cardPendingReports)

        // Vincular Gráficos y Lista
        lineChartWeekly = findViewById(R.id.lineChartWeekly)
        pieChartDistribution = findViewById(R.id.pieChartDistribution)
        llTopAffiliates = findViewById(R.id.llTopAffiliates)

        // Hacer las tarjetas pequeñas e invisibles para la animación inicial
        listOf(cardTotalUsers, cardActivePartners, cardTotalReports, cardPendingReports).forEach {
            it.alpha = 0f
            it.scaleX = 0.8f
            it.scaleY = 0.8f
        }
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun loadAllMetrics() {
        // 1. Cargar contadores numéricos (Tarjetas superiores)
        loadTotalUsers()
        loadTotalReports()
        loadPendingReports()
        loadActivePartners()

        // 2. Cargar Top Afiliados (REAL)
        loadTopAffiliatesReal()

        // 3. Cargar Gráfico Semanal (REAL)
        loadWeeklyActivityChartReal()

        // 4. Cargar Gráfico Distribución (REAL - CORREGIDO COLORES)
        loadDistributionChartReal()
    }

    // --- LÓGICA REAL: GRÁFICO DE LÍNEAS (ACTIVIDAD SEMANAL) ---
    private fun loadWeeklyActivityChartReal() {
        // Configuración visual
        lineChartWeekly.description.isEnabled = false
        lineChartWeekly.setDrawGridBackground(false)
        lineChartWeekly.axisRight.isEnabled = false
        lineChartWeekly.extraBottomOffset = 10f

        // Eje X
        val xAxis = lineChartWeekly.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.parseColor("#666666")
        xAxis.textSize = 12f
        xAxis.granularity = 1f

        // Eje Y
        lineChartWeekly.axisLeft.setDrawGridLines(true)
        lineChartWeekly.axisLeft.enableGridDashedLine(10f, 10f, 0f)

        // Preparar etiquetas de días (Últimos 7 días)
        val daysLabels = ArrayList<String>()
        val reportCounts = IntArray(7) { 0 } // Contadores para 7 días

        for (i in 6 downTo 0) {
            val pastDay = Calendar.getInstance()
            pastDay.add(Calendar.DAY_OF_YEAR, -i)
            val dayName = when(pastDay.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lun"
                Calendar.TUESDAY -> "Mar"
                Calendar.WEDNESDAY -> "Mié"
                Calendar.THURSDAY -> "Jue"
                Calendar.FRIDAY -> "Vie"
                Calendar.SATURDAY -> "Sáb"
                Calendar.SUNDAY -> "Dom"
                else -> "?"
            }
            daysLabels.add(dayName)
        }
        xAxis.valueFormatter = IndexAxisValueFormatter(daysLabels)

        // Descargar datos de Firebase
        firestore.collection("reports").get()
            .addOnSuccessListener { documents ->
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 23) // Final del día

                val sevenDaysAgo = Calendar.getInstance()
                sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -6)
                sevenDaysAgo.set(Calendar.HOUR_OF_DAY, 0) // Inicio del día

                for (doc in documents) {
                    val createdAt = doc.getLong("createdAt") ?: 0L

                    // Si el reporte es de esta semana
                    if (createdAt >= sevenDaysAgo.timeInMillis && createdAt <= today.timeInMillis) {
                        val reportDate = Calendar.getInstance()
                        reportDate.timeInMillis = createdAt

                        // Calcular índice (0 a 6)
                        val diff = today.get(Calendar.DAY_OF_YEAR) - reportDate.get(Calendar.DAY_OF_YEAR)
                        val index = 6 - diff

                        if (index in 0..6) {
                            reportCounts[index]++
                        }
                    }
                }

                // Crear datos para el gráfico
                val entries = ArrayList<Entry>()
                for (i in 0..6) {
                    entries.add(Entry(i.toFloat(), reportCounts[i].toFloat()))
                }

                val dataSet = LineDataSet(entries, "Nuevos Reportes")
                dataSet.color = Color.parseColor("#42A5F5") // Azul
                dataSet.lineWidth = 3f
                dataSet.setCircleColor(Color.parseColor("#42A5F5"))
                dataSet.circleRadius = 5f
                dataSet.setDrawValues(true)
                dataSet.valueTextSize = 10f
                dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Curvas suaves

                val data = LineData(dataSet)
                lineChartWeekly.data = data
                lineChartWeekly.animateXY(1500, 1500)
                lineChartWeekly.invalidate()
            }
            .addOnFailureListener { Log.e("Metrics", "Error cargando gráfico semanal", it) }
    }

    // --- LÓGICA REAL: GRÁFICO DE PASTEL (DISTRIBUCIÓN - CON CORRECCIÓN DE COLOR) ---
    // --- LÓGICA REAL: ESTATUS DE LOS CASOS (PENDIENTE vs RESUELTO) ---
    private fun loadDistributionChartReal() {
        // Configuración visual
        pieChartDistribution.description.isEnabled = false
        pieChartDistribution.legend.isEnabled = false
        pieChartDistribution.setHoleColor(Color.TRANSPARENT)
        pieChartDistribution.setTransparentCircleColor(Color.WHITE)
        pieChartDistribution.setTransparentCircleAlpha(110)
        pieChartDistribution.holeRadius = 50f
        pieChartDistribution.transparentCircleRadius = 55f

        // Cambiamos el título del centro del gráfico (Opcional)
        pieChartDistribution.centerText = "Estatus"
        pieChartDistribution.setCenterTextSize(10f)
        pieChartDistribution.setCenterTextColor(Color.GRAY)

        firestore.collection("reports").get()
            .addOnSuccessListener { documents ->
                var pending = 0f
                var inProgress = 0f
                var resolved = 0f

                for (doc in documents) {
                    // Leemos el estado real de cada reporte
                    val status = (doc.getString("status") ?: "pending").lowercase()

                    when (status) {
                        "pending" -> pending++
                        "in_progress" -> inProgress++
                        "resolved" -> resolved++
                        else -> pending++ // Por defecto asumimos pendiente
                    }
                }

                if (pending + inProgress + resolved == 0f) return@addOnSuccessListener

                val entries = ArrayList<PieEntry>()
                val colors = ArrayList<Int>()

                // 1. PENDIENTES (Color Naranja/Rojo - Urgente)
                if (pending > 0) {
                    entries.add(PieEntry(pending, "Pendientes"))
                    colors.add(Color.parseColor("#FF7043")) // Naranja Fuerte
                }

                // 2. EN PROCESO (Color Azul/Amarillo - Trabajando)
                if (inProgress > 0) {
                    entries.add(PieEntry(inProgress, "En Proceso"))
                    colors.add(Color.parseColor("#42A5F5")) // Azul
                }

                // 3. RESUELTOS (Color Verde - Éxito)
                if (resolved > 0) {
                    entries.add(PieEntry(resolved, "Resueltos"))
                    colors.add(Color.parseColor("#66BB6A")) // Verde
                }

                val dataSet = PieDataSet(entries, "")
                dataSet.sliceSpace = 3f
                dataSet.selectionShift = 5f
                dataSet.colors = colors

                dataSet.valueTextColor = Color.WHITE
                dataSet.valueTextSize = 12f

                val data = PieData(dataSet)
                data.setDrawValues(false) // Limpio, sin números encimados

                pieChartDistribution.data = data
                pieChartDistribution.animateY(1400)
                pieChartDistribution.invalidate()
            }
            .addOnFailureListener {
                Log.e("Metrics", "Error cargando gráfico estatus", it)
            }
    }

    // --- LÓGICA REAL: TOP AFILIADOS ---
    private fun loadTopAffiliatesReal() {
        firestore.collection("reports").get()
            .addOnSuccessListener { documents ->
                // 1. Contar reportes por usuario
                val userCounts = HashMap<String, Int>()
                for (doc in documents) {
                    val userId = doc.getString("userId")
                    if (userId != null) {
                        userCounts[userId] = (userCounts[userId] ?: 0) + 1
                    }
                }

                // 2. Ordenar y tomar top 3
                val topUsers = userCounts.entries
                    .sortedByDescending { it.value }
                    .take(3)

                llTopAffiliates.removeAllViews()

                if (topUsers.isEmpty()) {
                    addAffiliateItem(1, "Sin datos", 0)
                    return@addOnSuccessListener
                }

                // 3. Buscar nombres reales
                topUsers.forEachIndexed { index, entry ->
                    val userId = entry.key
                    val count = entry.value

                    firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: userDoc.getString("fullName") ?: "Usuario"

                            // Asegurar actualización en hilo principal
                            Handler(Looper.getMainLooper()).post {
                                addAffiliateItem(index + 1, name, count)
                            }
                        }
                        .addOnFailureListener {
                            addAffiliateItem(index + 1, "Usuario", count)
                        }
                }
            }
    }

    // --- CONTADORES GENERALES ---

    private fun loadTotalUsers() {
        firestore.collection("users").get().addOnSuccessListener {
            animateNumber(tvTotalUsers, it.size())
        }
    }

    private fun loadTotalReports() {
        firestore.collection("reports").get().addOnSuccessListener {
            animateNumber(tvTotalReports, it.size())
        }
    }

    private fun loadPendingReports() {
        firestore.collection("reports")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener {
                animateNumber(tvPendingReports, it.size())
            }
    }

    private fun loadActivePartners() {
        firestore.collection("users")
            .whereEqualTo("role", "partner")
            .get()
            .addOnSuccessListener {
                animateNumber(tvActivePartners, it.size())
            }
    }

    // --- UI HELPERS ---

    private fun animateNumber(textView: TextView, finalValue: Int) {
        val animator = ValueAnimator.ofInt(0, finalValue)
        animator.duration = 1500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    private fun animateCardsEntrance() {
        val cards = listOf(cardTotalUsers, cardActivePartners, cardTotalReports, cardPendingReports)
        cards.forEachIndexed { index, card ->
            Handler(Looper.getMainLooper()).postDelayed({
                card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }, index * 100L)
        }
    }

    private fun addAffiliateItem(rank: Int, name: String, count: Int) {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Emoji Medalla
        val medal = TextView(this).apply {
            text = when(rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "🏅"
            }
            textSize = 18f
            setPadding(0,0,16,0)
        }

        // Nombre
        val nameView = TextView(this).apply {
            text = name
            textSize = 14f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        // Conteo
        val countView = TextView(this).apply {
            text = "$count reps"
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#FF6B35"))
        }

        itemLayout.addView(medal)
        itemLayout.addView(nameView)
        itemLayout.addView(countView)

        llTopAffiliates.addView(itemLayout)
    }
}