package com.example.rescateanimal

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AdminMetricsActivity : AppCompatActivity() {

    private lateinit var navigationHelper: NavigationHelper
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AdminMetrics"

    // Views
    private lateinit var tvLastUpdate: TextView
    private lateinit var btnRefresh: ImageView

    // Métricas
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvActivePartners: TextView
    private lateinit var tvTotalReports: TextView
    private lateinit var tvTotalAdoptions: TextView

    // Cards
    private lateinit var cardUsers: CardView
    private lateinit var cardReports: CardView
    private lateinit var cardPartners: CardView
    private lateinit var cardAdoptions: CardView

    // Íconos
    private lateinit var iconUsers: ImageView
    private lateinit var iconReports: ImageView
    private lateinit var iconPartners: ImageView
    private lateinit var iconAdoptions: ImageView

    // Gráficas pequeñas
    private lateinit var chartUsers: LineChart
    private lateinit var chartReports: LineChart
    private lateinit var chartPartners: LineChart
    private lateinit var chartAdoptions: LineChart

    // Gráficas grandes
    private lateinit var chartReportsByStatus: BarChart
    private lateinit var chartWeeklyActivity: LineChart
    private lateinit var chartAdoptionDistribution: PieChart
    private lateinit var chartMonthlyGrowth: LineChart

    private var isLoading = false

    // Datos reales de Firebase
    private var totalUsers = 0
    private var totalPartners = 0
    private var totalReports = 0
    private var totalAnimals = 0
    private var reportsPending = 0
    private var reportsResolved = 0
    private var reportsInProgress = 0
    private var reportsRejected = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_metrics)

        initViews()
        setupNavigation()
        setupClickListeners()
        animateInitialLoad()
        loadAllData()
    }

    private fun initViews() {
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        btnRefresh = findViewById(R.id.btnRefresh)

        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvActivePartners = findViewById(R.id.tvActivePartners)
        tvTotalReports = findViewById(R.id.tvTotalReports)
        tvTotalAdoptions = findViewById(R.id.tvTotalAdoptions)

        // Cards
        cardUsers = findViewById(R.id.cardUsers)
        cardReports = findViewById(R.id.cardReports)
        cardPartners = findViewById(R.id.cardPartners)
        cardAdoptions = findViewById(R.id.cardAdoptions)

        // Íconos
        iconUsers = findViewById(R.id.iconUsers)
        iconReports = findViewById(R.id.iconReports)
        iconPartners = findViewById(R.id.iconPartners)
        iconAdoptions = findViewById(R.id.iconAdoptions)

        chartUsers = findViewById(R.id.chartUsers)
        chartReports = findViewById(R.id.chartReports)
        chartPartners = findViewById(R.id.chartPartners)
        chartAdoptions = findViewById(R.id.chartAdoptions)

        chartReportsByStatus = findViewById(R.id.chartReportsByStatus)
        chartWeeklyActivity = findViewById(R.id.chartWeeklyActivity)
        chartAdoptionDistribution = findViewById(R.id.chartAdoptionDistribution)
        chartMonthlyGrowth = findViewById(R.id.chartMonthlyGrowth)
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            if (!isLoading) {
                animateRefreshButton()
                loadAllData()
            }
        }

        // Animación al hacer clic en las cards
        setupCardClickAnimation(cardUsers)
        setupCardClickAnimation(cardReports)
        setupCardClickAnimation(cardPartners)
        setupCardClickAnimation(cardAdoptions)
    }

    private fun setupCardClickAnimation(card: CardView) {
        card.setOnClickListener {
            val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                card,
                PropertyValuesHolder.ofFloat("scaleX", 0.95f),
                PropertyValuesHolder.ofFloat("scaleY", 0.95f)
            ).apply {
                duration = 100
            }

            val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                card,
                PropertyValuesHolder.ofFloat("scaleX", 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f)
            ).apply {
                duration = 100
                startDelay = 100
            }

            scaleDown.start()
            scaleUp.start()
        }
    }

    private fun animateInitialLoad() {
        // Animar entrada de cards con delay escalonado
        animateCardEntry(cardUsers, 100)
        animateCardEntry(cardReports, 200)
        animateCardEntry(cardPartners, 300)
        animateCardEntry(cardAdoptions, 400)

        // Animar íconos
        Handler(Looper.getMainLooper()).postDelayed({
            animateIconPulse(iconUsers)
            animateIconPulse(iconReports)
            animateIconPulse(iconPartners)
            animateIconPulse(iconAdoptions)
        }, 600)
    }

    private fun animateCardEntry(view: View, delay: Long) {
        view.apply {
            alpha = 0f
            translationY = 80f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(delay)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
        }
    }

    private fun animateIconPulse(imageView: ImageView) {
        val pulse = ObjectAnimator.ofPropertyValuesHolder(
            imageView,
            PropertyValuesHolder.ofFloat("scaleX", 1.1f),
            PropertyValuesHolder.ofFloat("scaleY", 1.1f)
        ).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        pulse.start()
    }

    private fun loadAllData() {
        if (isLoading) return
        isLoading = true

        loadUsersData()
        loadAffiliatesData()
        loadReportsData()
        loadAnimalsData()

        Handler(Looper.getMainLooper()).postDelayed({
            isLoading = false
            updateLastRefreshTime()
        }, 2000)
    }

    // ========== CARGAR USUARIOS ==========
    private fun loadUsersData() {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                totalUsers = documents.size()
                animateCounterValue(tvTotalUsers, totalUsers)

                Log.d(TAG, "Total usuarios: $totalUsers")

                setupMiniLineChart(
                    chartUsers,
                    generateTrendData(totalUsers),
                    Color.parseColor("#667eea"),
                    Color.parseColor("#764ba2")
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading users", e)
                tvTotalUsers.text = "0"
            }
    }

    // ========== CARGAR AFILIADOS/PARTNERS ==========
    private fun loadAffiliatesData() {
        db.collection("affiliates")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                totalPartners = documents.size()
                animateCounterValue(tvActivePartners, totalPartners)

                Log.d(TAG, "Partners activos: $totalPartners")

                setupMiniLineChart(
                    chartPartners,
                    generateTrendData(totalPartners),
                    Color.parseColor("#a8edea"),
                    Color.parseColor("#fed6e3")
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading affiliates", e)
                tvActivePartners.text = "0"
            }
    }

    // ========== CARGAR REPORTES ==========
    private fun loadReportsData() {
        db.collection("reports")
            .get()
            .addOnSuccessListener { documents ->
                totalReports = documents.size()
                animateCounterValue(tvTotalReports, totalReports)

                reportsPending = 0
                reportsInProgress = 0
                reportsResolved = 0
                reportsRejected = 0

                documents.forEach { doc ->
                    when (doc.getString("status")) {
                        "pending" -> reportsPending++
                        "in_progress" -> reportsInProgress++
                        "resolved" -> reportsResolved++
                        "rejected" -> reportsRejected++
                    }
                }

                Log.d(TAG, "Reportes - Total: $totalReports")

                setupMiniLineChart(
                    chartReports,
                    generateTrendData(totalReports),
                    Color.parseColor("#4facfe"),
                    Color.parseColor("#00f2fe")
                )
                setupReportsByStatusChart()
                setupWeeklyActivityChart()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading reports", e)
                tvTotalReports.text = "0"
            }
    }

    // ========== CARGAR ANIMALES (ADOPCIONES) ==========
    private fun loadAnimalsData() {
        db.collection("animals")
            .get()
            .addOnSuccessListener { documents ->
                totalAnimals = documents.size()
                animateCounterValue(tvTotalAdoptions, totalAnimals)

                var perros = 0
                var gatos = 0
                var otros = 0

                documents.forEach { doc ->
                    when (doc.getString("species")?.lowercase()) {
                        "perro", "dog" -> perros++
                        "gato", "cat" -> gatos++
                        else -> otros++
                    }
                }

                Log.d(TAG, "Animales - Total: $totalAnimals")

                setupMiniLineChart(
                    chartAdoptions,
                    generateTrendData(totalAnimals),
                    Color.parseColor("#fa709a"),
                    Color.parseColor("#fee140")
                )
                setupAdoptionDistributionChart(perros, gatos, otros)
                setupMonthlyGrowthChart()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading animals", e)
                tvTotalAdoptions.text = "0"
            }
    }

    // ========== GENERAR DATOS DE TENDENCIA ==========
    private fun generateTrendData(currentValue: Int): List<Float> {
        val base = if (currentValue > 10) currentValue - 10 else 0
        return listOf(
            base.toFloat(),
            (base + currentValue * 0.1).toFloat(),
            (base + currentValue * 0.15).toFloat(),
            (base + currentValue * 0.3).toFloat(),
            (base + currentValue * 0.5).toFloat(),
            (base + currentValue * 0.7).toFloat(),
            currentValue.toFloat()
        )
    }

    // ========== MINI GRÁFICAS MEJORADAS CON GRADIENTE ==========
    private fun setupMiniLineChart(
        chart: LineChart,
        values: List<Float>,
        colorStart: Int,
        colorEnd: Int
    ) {
        val entries = values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "").apply {
            setDrawValues(false)
            setDrawCircles(false)
            setDrawCircleHole(false)
            color = colorStart
            lineWidth = 3f
            setDrawFilled(true)

            // Crear gradiente para el área rellena
            val gradientColors = intArrayOf(
                Color.argb(180, Color.red(colorStart), Color.green(colorStart), Color.blue(colorStart)),
                Color.argb(80, Color.red(colorEnd), Color.green(colorEnd), Color.blue(colorEnd)),
                Color.TRANSPARENT
            )

            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                gradientColors
            )

            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setBackgroundColor(Color.TRANSPARENT)
            animateX(1200, Easing.EaseInOutQuart)
        }
    }

    // ========== GRÁFICA: REPORTES POR ESTADO (BarChart) ==========
    private fun setupReportsByStatusChart() {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, reportsPending.toFloat()))
        entries.add(BarEntry(1f, reportsInProgress.toFloat()))
        entries.add(BarEntry(2f, reportsResolved.toFloat()))
        entries.add(BarEntry(3f, reportsRejected.toFloat()))

        val dataSet = BarDataSet(entries, "Reportes").apply {
            colors = listOf(
                Color.parseColor("#667eea"), // Morado - Pendientes
                Color.parseColor("#4facfe"), // Azul - En Progreso
                Color.parseColor("#43e97b"), // Verde - Resueltos
                Color.parseColor("#fa709a")  // Rosa - Rechazados
            )
            valueTextSize = 14f
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }

        val labels = arrayOf("Pendientes", "En Progreso", "Resueltos", "Rechazados")

        chartReportsByStatus.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                textSize = 11f
                textColor = Color.BLACK
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E8E8E8")
                axisMinimum = 0f
                textSize = 11f
                textColor = Color.BLACK
            }

            axisRight.isEnabled = false
            legend.isEnabled = false

            animateY(1400, Easing.EaseInOutQuart)
            invalidate()
        }
    }

    // ========== GRÁFICA: ACTIVIDAD SEMANAL (LineChart) ==========
    private fun setupWeeklyActivityChart() {
        val reportEntries = ArrayList<Entry>()
        val adoptionEntries = ArrayList<Entry>()

        val avgReports = if (totalReports > 0) totalReports / 7 else 1
        val avgAnimals = if (totalAnimals > 0) totalAnimals / 7 else 1

        for (i in 0..6) {
            val reportVariation = (Math.random() * avgReports * 0.5).toFloat()
            val animalVariation = (Math.random() * avgAnimals * 0.5).toFloat()

            reportEntries.add(Entry(i.toFloat(), avgReports.toFloat() + reportVariation))
            adoptionEntries.add(Entry(i.toFloat(), avgAnimals.toFloat() + animalVariation))
        }

        val reportDataSet = LineDataSet(reportEntries, "Reportes").apply {
            color = Color.parseColor("#667eea")
            lineWidth = 3.5f
            setCircleColor(Color.parseColor("#667eea"))
            circleRadius = 6f
            circleHoleRadius = 3f
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            setDrawFilled(true)

            val gradientColors = intArrayOf(
                Color.parseColor("#80667eea"),
                Color.TRANSPARENT
            )
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                gradientColors
            )
        }

        val adoptionDataSet = LineDataSet(adoptionEntries, "Animales").apply {
            color = Color.parseColor("#43e97b")
            lineWidth = 3.5f
            setCircleColor(Color.parseColor("#43e97b"))
            circleRadius = 6f
            circleHoleRadius = 3f
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            setDrawFilled(true)

            val gradientColors = intArrayOf(
                Color.parseColor("#8043e97b"),
                Color.TRANSPARENT
            )
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                gradientColors
            )
        }

        val days = arrayOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

        chartWeeklyActivity.apply {
            data = LineData(reportDataSet, adoptionDataSet)
            description.isEnabled = false
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(days)
                textSize = 11f
                textColor = Color.BLACK
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E8E8E8")
                axisMinimum = 0f
                textSize = 11f
                textColor = Color.BLACK
            }

            axisRight.isEnabled = false

            legend.apply {
                textSize = 12f
                textColor = Color.BLACK
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            animateXY(1400, 1400, Easing.EaseInOutQuart)
            invalidate()
        }
    }

    // ========== GRÁFICA: DISTRIBUCIÓN DE ADOPCIONES (PieChart) ==========
    private fun setupAdoptionDistributionChart(perros: Int, gatos: Int, otros: Int) {
        val entries = ArrayList<PieEntry>()

        if (perros > 0) entries.add(PieEntry(perros.toFloat(), "Perros"))
        if (gatos > 0) entries.add(PieEntry(gatos.toFloat(), "Gatos"))
        if (otros > 0) entries.add(PieEntry(otros.toFloat(), "Otros"))

        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "Sin datos"))
        }

        val dataSet = PieDataSet(entries, "Animales por Tipo").apply {
            colors = listOf(
                Color.parseColor("#667eea"),
                Color.parseColor("#fa709a"),
                Color.parseColor("#4facfe"),
                Color.parseColor("#43e97b")
            )
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            sliceSpace = 4f
            selectionShift = 12f
        }

        chartAdoptionDistribution.apply {
            val pieData = PieData(dataSet)
            pieData.setValueFormatter(PercentFormatter(chartAdoptionDistribution))
            data = pieData

            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)

            centerText = "Distribución\nde Animales"
            setCenterTextSize(16f)
            setCenterTextColor(Color.BLACK)

            legend.apply {
                textSize = 12f
                textColor = Color.BLACK
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 10f
                yEntrySpace = 5f
            }

            setHoleColor(Color.TRANSPARENT)
            holeRadius = 45f
            transparentCircleRadius = 50f

            animateY(1400, Easing.EaseInOutQuart)
            invalidate()
        }
    }

    // ========== GRÁFICA: CRECIMIENTO MENSUAL (LineChart) ==========
    private fun setupMonthlyGrowthChart() {
        val userEntries = ArrayList<Entry>()
        val partnerEntries = ArrayList<Entry>()

        val userGrowth = if (totalUsers > 6) totalUsers / 6 else 1
        val partnerGrowth = if (totalPartners > 6) totalPartners / 6 else 1

        var accumulatedUsers = 0
        var accumulatedPartners = 0

        for (i in 0..5) {
            accumulatedUsers += userGrowth + (Math.random() * userGrowth * 0.3).toInt()
            accumulatedPartners += partnerGrowth + (Math.random() * partnerGrowth * 0.3).toInt()

            userEntries.add(Entry(i.toFloat(), accumulatedUsers.toFloat()))
            partnerEntries.add(Entry(i.toFloat(), accumulatedPartners.toFloat()))
        }

        val userDataSet = LineDataSet(userEntries, "Usuarios").apply {
            color = Color.parseColor("#4facfe")
            lineWidth = 4f
            setCircleColor(Color.parseColor("#4facfe"))
            circleRadius = 7f
            circleHoleRadius = 4f
            setDrawValues(true)
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
        }

        val partnerDataSet = LineDataSet(partnerEntries, "Partners").apply {
            color = Color.parseColor("#f093fb")
            lineWidth = 4f
            setCircleColor(Color.parseColor("#f093fb"))
            circleRadius = 7f
            circleHoleRadius = 4f
            setDrawValues(true)
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
        }

        val months = arrayOf("Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

        chartMonthlyGrowth.apply {
            data = LineData(userDataSet, partnerDataSet)
            description.isEnabled = false
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(months)
                textSize = 12f
                textColor = Color.BLACK
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E8E8E8")
                axisMinimum = 0f
                textSize = 11f
                textColor = Color.BLACK
            }

            axisRight.isEnabled = false

            legend.apply {
                textSize = 13f
                textColor = Color.BLACK
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
            }

            animateXY(1600, 1600, Easing.EaseInOutQuart)
            invalidate()
        }
    }

    // ========== FUNCIONES AUXILIARES ==========

    private fun animateCounterValue(textView: TextView, targetValue: Int) {
        val duration = 1500L
        val incrementTime = 30L
        val handler = Handler(Looper.getMainLooper())
        var currentValue = 0
        val increment = Math.max(1, targetValue / (duration / incrementTime).toInt())

        val runnable = object : Runnable {
            override fun run() {
                if (currentValue < targetValue) {
                    currentValue += increment
                    if (currentValue > targetValue) currentValue = targetValue
                    textView.text = currentValue.toString()
                    handler.postDelayed(this, incrementTime)
                } else {
                    textView.text = targetValue.toString()
                }
            }
        }

        handler.post(runnable)
    }

    private fun animateRefreshButton() {
        try {
            val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh)
            btnRefresh.startAnimation(rotate)
        } catch (e: Exception) {
            Log.e(TAG, "Error animating refresh button", e)
        }
    }

    private fun updateLastRefreshTime() {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvLastUpdate.text = "Actualizado a las ${format.format(Date())}"
    }

    override fun onResume() {
        super.onResume()
        loadAllData()
    }
}