package com.example.rescateanimal

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class AdminMetricsActivity : AppCompatActivity() {

    private lateinit var navigationHelper: NavigationHelper
    private val firestore = FirebaseFirestore.getInstance()

    // Métricas
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalReports: TextView
    private lateinit var tvPendingReports: TextView
    private lateinit var tvTotalAdoptions: TextView
    private lateinit var tvActivePartners: TextView
    private lateinit var tvPendingAffiliates: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_metrics)

        initViews()
        setupNavigation()
        loadMetrics()
    }

    private fun initViews() {
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvTotalReports = findViewById(R.id.tvTotalReports)
        tvPendingReports = findViewById(R.id.tvPendingReports)
        tvTotalAdoptions = findViewById(R.id.tvTotalAdoptions)
        tvActivePartners = findViewById(R.id.tvActivePartners)
        tvPendingAffiliates = findViewById(R.id.tvPendingAffiliates)
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    private fun loadMetrics() {
        loadTotalUsers()
        loadTotalReports()
        loadPendingReports()
        loadTotalAdoptions()
        loadActivePartners()
        loadPendingAffiliates()
    }

    private fun loadTotalUsers() {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                tvTotalUsers.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvTotalUsers.text = "Error"
            }
    }

    private fun loadTotalReports() {
        firestore.collection("reports")
            .get()
            .addOnSuccessListener { documents ->
                tvTotalReports.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvTotalReports.text = "Error"
            }
    }

    private fun loadPendingReports() {
        firestore.collection("reports")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                tvPendingReports.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvPendingReports.text = "Error"
            }
    }

    private fun loadTotalAdoptions() {
        firestore.collection("adoptions")
            .get()
            .addOnSuccessListener { documents ->
                tvTotalAdoptions.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvTotalAdoptions.text = "Error"
            }
    }

    private fun loadActivePartners() {
        firestore.collection("users")
            .whereEqualTo("role", "partner")
            .get()
            .addOnSuccessListener { documents ->
                tvActivePartners.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvActivePartners.text = "Error"
            }
    }

    private fun loadPendingAffiliates() {
        firestore.collection("affiliations")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                tvPendingAffiliates.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvPendingAffiliates.text = "Error"
            }
    }
}