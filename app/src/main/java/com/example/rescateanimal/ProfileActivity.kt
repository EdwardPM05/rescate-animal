package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var navigationHelper: NavigationHelper

    private lateinit var menuAffiliate: LinearLayout
    private lateinit var tvAffiliateTitle: TextView
    private lateinit var tvAffiliateSubtitle: TextView
    private lateinit var tvAffiliateIcon: TextView

    private var userAffiliateStatus: String? = null // null, "pending", "approved", "rejected"
    private var userAffiliateType: String? = null // null, "veterinaria", "tienda", "albergue"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupViews()
        loadUserData()
        checkAffiliateStatus()

        // Setup navigation usando NavigationHelper
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando regresamos
        loadUserData()
        checkAffiliateStatus()
    }

    private fun setupViews() {
        // Back Button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Menu Options
        findViewById<LinearLayout>(R.id.menuEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.menuMyAdoptions).setOnClickListener {
            Toast.makeText(this, "Mis Adopciones - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuMyReports).setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.menuNotifications).setOnClickListener {
            Toast.makeText(this, "Notificaciones - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuPrivacy).setOnClickListener {
            Toast.makeText(this, "Privacidad - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuHelp).setOnClickListener {
            Toast.makeText(this, "Ayuda - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // AFFILIATE OPTION - Setup views
        menuAffiliate = findViewById(R.id.menuAffiliate)
        tvAffiliateIcon = menuAffiliate.findViewById<TextView>(R.id.tvAffiliateIcon)
        val affiliateTextContainer = menuAffiliate.getChildAt(1) as LinearLayout
        tvAffiliateTitle = affiliateTextContainer.getChildAt(0) as TextView
        tvAffiliateSubtitle = affiliateTextContainer.getChildAt(1) as TextView

        // Click will be set after checking affiliate status

        // Logout
        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    private fun checkAffiliateStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        // Check if user has any affiliate registration
        db.collection("affiliates")
            .whereEqualTo("userId", currentUser.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No affiliate registration - Show register option
                    setupAffiliateButton(null, null)
                } else {
                    val affiliateDoc = documents.documents[0]
                    userAffiliateStatus = affiliateDoc.getString("status")
                    userAffiliateType = affiliateDoc.getString("type")
                    setupAffiliateButton(userAffiliateStatus, userAffiliateType)
                }
            }
            .addOnFailureListener {
                // On error, show default affiliate button
                setupAffiliateButton(null, null)
            }
    }

    private fun setupAffiliateButton(status: String?, type: String?) {
        when {
            // Approved shelter - Show "Register Pets" button
            status == "approved" && type == "albergue" -> {
                tvAffiliateIcon.text = "🐾"
                tvAffiliateTitle.text = "Registrar mascotas"
                tvAffiliateSubtitle.text = "Publica mascotas disponibles para adopción"
                menuAffiliate.setOnClickListener {
                    val intent = Intent(this, RegisterPetActivity::class.java)
                    startActivity(intent)
                }
            }

            // Approved (non-shelter) - Show status
            status == "approved" -> {
                tvAffiliateIcon.text = "✓"
                tvAffiliateTitle.text = "Negocio afiliado"
                tvAffiliateSubtitle.text = "Tu ${getAffiliateTypeText(type)} está aprobada"
                menuAffiliate.setOnClickListener {
                    Toast.makeText(this, "Tu negocio ya está afiliado y aparece en el mapa", Toast.LENGTH_SHORT).show()
                }
            }

            // Pending approval
            status == "pending" -> {
                tvAffiliateIcon.text = "⏳"
                tvAffiliateTitle.text = "Solicitud en revisión"
                tvAffiliateSubtitle.text = "Tu solicitud está siendo revisada (2-3 días hábiles)"
                menuAffiliate.setOnClickListener {
                    Toast.makeText(this, "Tu solicitud está en proceso de revisión", Toast.LENGTH_SHORT).show()
                }
            }

            // Rejected
            status == "rejected" -> {
                tvAffiliateIcon.text = "✗"
                tvAffiliateTitle.text = "Solicitud rechazada"
                tvAffiliateSubtitle.text = "Tu solicitud no fue aprobada. Intenta nuevamente"
                menuAffiliate.setOnClickListener {
                    val intent = Intent(this, AffiliateActivity::class.java)
                    startActivity(intent)
                }
            }

            // No registration - Default
            else -> {
                tvAffiliateIcon.text = "🏢"
                tvAffiliateTitle.text = "Afiliar mi negocio"
                tvAffiliateSubtitle.text = "Registro para veterinarias, tiendas y albergues"
                menuAffiliate.setOnClickListener {
                    val intent = Intent(this, AffiliateActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun getAffiliateTypeText(type: String?): String {
        return when (type) {
            "veterinaria" -> "veterinaria"
            "tienda" -> "tienda"
            "albergue" -> "albergue"
            else -> "negocio"
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Obtener datos básicos del usuario
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument.exists()) {
                        val user = userDocument.toObject(User::class.java)
                        user?.let {
                            findViewById<TextView>(R.id.tvUserName).text = it.displayName
                            findViewById<TextView>(R.id.tvEmail).text = it.email
                            findViewById<TextView>(R.id.tvMemberSince).text = "Rescatista Desde ${formatDate(it.createdAt)}"
                            findViewById<TextView>(R.id.tvJoinDate).text = formatDate(it.createdAt)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar datos básicos: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // Obtener datos del perfil extendido
            db.collection("user_profiles").document(currentUser.uid)
                .get()
                .addOnSuccessListener { profileDocument ->
                    if (profileDocument.exists()) {
                        updateUIWithProfileData(profileDocument.data!!)
                    } else {
                        setDefaultProfileData()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                    setDefaultProfileData()
                }
        }
    }

    private fun updateUIWithProfileData(profileData: Map<String, Any>) {
        findViewById<TextView>(R.id.tvPhone).text = profileData["phone"] as? String ?: "+51 999 999 999"
        findViewById<TextView>(R.id.tvLocation).text =
            (profileData["address"] as? String)?.split(",")?.take(2)?.joinToString(", ") ?: "Lima, Perú"

        findViewById<TextView>(R.id.tvReportCount).text = "0"
        findViewById<TextView>(R.id.tvRescueCount).text = "0"
        findViewById<TextView>(R.id.tvPointsCount).text = "100"
    }

    private fun setDefaultProfileData() {
        findViewById<TextView>(R.id.tvPhone).text = "No configurado"
        findViewById<TextView>(R.id.tvLocation).text = "Lima, Perú"

        findViewById<TextView>(R.id.tvReportCount).text = "0"
        findViewById<TextView>(R.id.tvRescueCount).text = "0"
        findViewById<TextView>(R.id.tvPointsCount).text = "100"
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
        return format.format(date)
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}