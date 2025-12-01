package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.rescateanimal.data.models.Affiliate

class AdminAffiliatesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var adapter: AdminAffiliatesAdapter
    private val affiliatesList = mutableListOf<Affiliate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_affiliates)

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.rvAdminAffiliates)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminAffiliatesAdapter(affiliatesList) { affiliate ->
            // Estrategia Segura: Pasar Objeto Completo
            try {
                val intent = Intent(this, AdminAffiliateDetailActivity::class.java)
                intent.putExtra("AFFILIATE_DATA", affiliate)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("AdminAffiliates", "Error al abrir detalle", e)
                Toast.makeText(this, "Error al abrir: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter

        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadPendingAffiliates()
    }

    private fun loadPendingAffiliates() {
        db.collection("affiliates")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                affiliatesList.clear()
                for (document in documents) {
                    val affiliate = document.toObject(Affiliate::class.java)
                    // Rellenar ID si falta
                    val finalAffiliate = if (affiliate.id.isEmpty()) {
                        affiliate.copy(id = document.id)
                    } else {
                        affiliate
                    }
                    affiliatesList.add(finalAffiliate)
                }

                if (affiliatesList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyStateLayout.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyStateLayout.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavigation() {
        val navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}