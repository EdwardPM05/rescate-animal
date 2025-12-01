package com.example.rescateanimal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var navigationHelper: NavigationHelper
    private lateinit var roleManager: RoleManager

    private lateinit var ivProfileImage: ImageView
    private lateinit var menuMyReports: LinearLayout
    private lateinit var menuAffiliate: LinearLayout
    private lateinit var dividerMyReports: View
    private lateinit var dividerAffiliate: View
    private lateinit var ivAffiliateIcon: ImageView
    private lateinit var tvAffiliateTitle: TextView
    private lateinit var tvAffiliateSubtitle: TextView

    private var userAffiliateStatus: String? = null
    private var userAffiliateType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inicializar TODAS las dependencias PRIMERO
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        roleManager = RoleManager(this)
        navigationHelper = NavigationHelper(this)

        // Luego configurar las vistas y UI
        setupViews()
        loadUserData()
        configureUIBasedOnRole()

        // Finalmente configurar la navegación
        navigationHelper.setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        configureUIBasedOnRole()
    }

    private fun setupViews() {
        // Profile Image
        ivProfileImage = findViewById(R.id.ivProfileImage)

        // Back Button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Menu Options
        findViewById<LinearLayout>(R.id.menuEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // REFERENCIA A MIS REPORTES (para ocultar si es admin/partner)
        menuMyReports = findViewById(R.id.menuMyReports)
        menuMyReports.setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }

        // REFERENCIA AL DIVIDER de Mis Reportes
        dividerMyReports = findViewById(R.id.dividerMyReports)

        // REFERENCIA A AFFILIATE (para ocultar si es partner o admin)
        menuAffiliate = findViewById(R.id.menuAffiliate)
        ivAffiliateIcon = findViewById(R.id.ivAffiliateIcon)
        tvAffiliateTitle = findViewById(R.id.tvAffiliateTitle)
        tvAffiliateSubtitle = findViewById(R.id.tvAffiliateSubtitle)
        dividerAffiliate = findViewById(R.id.dividerAffiliate)

        findViewById<LinearLayout>(R.id.menuNotifications).setOnClickListener {
            Toast.makeText(this, "Notificaciones - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.menuPrivacy).setOnClickListener {
            Toast.makeText(this, "Privacidad - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Logout
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    /**
     * Configura la interfaz según el rol del usuario
     * - USER: Ve todo (Mis Reportes + Afiliar mi negocio)
     * - PARTNER: Solo opciones personales (ya está afiliado, no necesita "Afiliar negocio")
     * - ADMIN: Solo opciones personales (no necesita reportes ni afiliación)
     */
    private fun configureUIBasedOnRole() {
        val currentRole = roleManager.getCurrentRole()

        when (currentRole) {
            RoleManager.ROLE_USER -> {
                // USUARIO NORMAL: Ve todas las opciones
                menuMyReports.visibility = View.VISIBLE
                dividerMyReports.visibility = View.VISIBLE

                menuAffiliate.visibility = View.VISIBLE
                dividerAffiliate.visibility = View.VISIBLE

                // Verificar estado de afiliación para mostrar opciones dinámicas
                checkAffiliateStatus()
            }
            RoleManager.ROLE_PARTNER -> {
                // PARTNER: Ya está afiliado, no necesita "Afiliar negocio"
                menuMyReports.visibility = View.GONE
                dividerMyReports.visibility = View.GONE

                menuAffiliate.visibility = View.GONE
                dividerAffiliate.visibility = View.GONE
            }
            RoleManager.ROLE_ADMIN -> {
                // ADMIN: Solo configuración personal
                menuMyReports.visibility = View.GONE
                dividerMyReports.visibility = View.GONE

                menuAffiliate.visibility = View.GONE
                dividerAffiliate.visibility = View.GONE
            }
        }
    }

    private fun checkAffiliateStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        db.collection("affiliates")
            .whereEqualTo("userId", currentUser.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    setupAffiliateButton(null, null)
                } else {
                    val affiliateDoc = documents.documents[0]
                    userAffiliateStatus = affiliateDoc.getString("status")
                    userAffiliateType = affiliateDoc.getString("type")
                    setupAffiliateButton(userAffiliateStatus, userAffiliateType)
                }
            }
            .addOnFailureListener {
                setupAffiliateButton(null, null)
            }
    }

    private fun setupAffiliateButton(status: String?, type: String?) {
        when {
            // APROBADO - Ya es afiliado
            status == "approved" -> {
                ivAffiliateIcon.setImageResource(R.drawable.ic_check)
                tvAffiliateTitle.text = "Negocio afiliado"
                tvAffiliateSubtitle.text = "Tu ${getAffiliateTypeText(type)} está aprobado"
                menuAffiliate.setOnClickListener {
                    Toast.makeText(this, "Tu negocio ya está afiliado. Cambia al modo Partner para gestionar tu negocio.", Toast.LENGTH_LONG).show()
                }
            }

            // PENDIENTE DE APROBACIÓN
            status == "pending" -> {
                ivAffiliateIcon.setImageResource(R.drawable.ic_pending)
                tvAffiliateTitle.text = "Solicitud en revisión"
                tvAffiliateSubtitle.text = "Revisaremos tu solicitud en 2-3 días hábiles"
                menuAffiliate.setOnClickListener {
                    Toast.makeText(this, "Tu solicitud está siendo revisada por nuestro equipo", Toast.LENGTH_LONG).show()
                }
            }

            // RECHAZADO - Puede volver a intentar
            status == "rejected" -> {
                ivAffiliateIcon.setImageResource(R.drawable.ic_error)
                tvAffiliateTitle.text = "Solicitud rechazada"
                tvAffiliateSubtitle.text = "Puedes volver a solicitar la afiliación"
                menuAffiliate.setOnClickListener {
                    val intent = Intent(this, AffiliateActivity::class.java)
                    startActivity(intent)
                }
            }

            // NUEVO USUARIO - Sin afiliación
            else -> {
                ivAffiliateIcon.setImageResource(R.drawable.ic_affiliate)
                tvAffiliateTitle.text = "Afiliar mi negocio"
                tvAffiliateSubtitle.text = "Registra tu veterinaria, tienda o albergue"
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
                            findViewById<TextView>(R.id.tvJoinDate).text = formatDate(it.createdAt)
                        }
                    }
                }

            // Obtener datos del perfil extendido (INCLUYENDO FOTO)
            db.collection("user_profiles").document(currentUser.uid)
                .get()
                .addOnSuccessListener { profileDocument ->
                    if (profileDocument.exists()) {
                        updateUIWithProfileData(profileDocument.data!!)

                        // CARGAR FOTO DE PERFIL
                        val photoUrl = profileDocument.getString("profilePhotoUrl")
                        loadProfileImage(photoUrl)
                    } else {
                        setDefaultProfileData()
                        loadProfileImage(null)
                    }
                }
                .addOnFailureListener {
                    setDefaultProfileData()
                    loadProfileImage(null)
                }
        }
    }

    private fun loadProfileImage(photoUrl: String?) {
        if (!photoUrl.isNullOrEmpty()) {
            // Cargar imagen desde Firebase Storage
            Glide.with(this)
                .load(photoUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                )
                .into(ivProfileImage)
        } else {
            // Mostrar placeholder si no hay foto
            Glide.with(this)
                .load(R.drawable.ic_profile_placeholder)
                .apply(RequestOptions().circleCrop())
                .into(ivProfileImage)
        }
    }

    private fun updateUIWithProfileData(profileData: Map<String, Any>) {
        findViewById<TextView>(R.id.tvPhone).text = profileData["phone"] as? String ?: "+51 999 999 999"
        findViewById<TextView>(R.id.tvLocation).text =
            (profileData["address"] as? String)?.split(",")?.take(2)?.joinToString(", ") ?: "Lima, Perú"
    }

    private fun setDefaultProfileData() {
        findViewById<TextView>(R.id.tvPhone).text = "No configurado"
        findViewById<TextView>(R.id.tvLocation).text = "Lima, Perú"
    }

    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
        return format.format(date)
    }

    private fun logout() {
        auth.signOut()
        roleManager.clearRoleData()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}