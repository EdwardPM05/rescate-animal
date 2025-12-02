package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class MapPartnerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLocationAddress: TextView
    private lateinit var tvLocationCoords: TextView
    private lateinit var locationInfoCard: LinearLayout
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var hasLocationPermission = false
    private var mapReady = false

    // Variables para coordenadas del intent (solo negocios)
    private var targetLatitude: Double? = null
    private var targetLongitude: Double? = null
    private var targetName: String? = null

    // Lista para guardar marcadores de afiliados
    private val affiliateMarkers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_partner)

        NavigationHelper(this).setupBottomNavigation()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tvLocationAddress = findViewById(R.id.tvLocationAddress)
        tvLocationCoords = findViewById(R.id.tvLocationCoords)
        locationInfoCard = findViewById(R.id.locationInfoCard)

        hasLocationPermission = hasLocationPermission()

        // Recibir coordenadas del intent (solo negocios)
        targetLatitude = intent.getDoubleExtra("latitude", 0.0).takeIf { it != 0.0 }
        targetLongitude = intent.getDoubleExtra("longitude", 0.0).takeIf { it != 0.0 }
        targetName = intent.getStringExtra("businessName") ?: "Negocio Afiliado"

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        val currentPermissionStatus = hasLocationPermission()
        if (currentPermissionStatus != hasLocationPermission) {
            hasLocationPermission = currentPermissionStatus
            if (mapReady) {
                setupMapLocation()
            }
        }

        // Recargar afiliados cada vez que vuelves al mapa
        if (mapReady) {
            reloadAffiliates()
        }
    }

    // Nueva función para recargar afiliados
    private fun reloadAffiliates() {
        affiliateMarkers.forEach { it.remove() }
        affiliateMarkers.clear()
        loadAffiliatesOnMap()
    }

    private fun setupUI() {
        val btnBack = findViewById<View>(R.id.btnBack)
        val btnMyLocation = findViewById<View>(R.id.btnMyLocation)

        btnBack.setOnClickListener {
            finish()
        }

        btnMyLocation.setOnClickListener {
            getCurrentLocation()
        }
    }



    private fun createMarkerFromDrawable(drawableRes: Int, backgroundColor: Int): BitmapDescriptor {
        val size = 140
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Sombra
        val shadowPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f + 4, size / 2f + 4, size / 2f - 10, shadowPaint)

        // Fondo circular
        val backgroundPaint = Paint().apply {
            color = backgroundColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 10, backgroundPaint)

        // Borde blanco
        val borderPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 13, borderPaint)

        // Dibujar el ícono PNG en el centro
        val drawable = ContextCompat.getDrawable(this, drawableRes)
        if (drawable != null) {
            val iconSize = 60
            val left = (size - iconSize) / 2
            val top = (size - iconSize) / 2
            val right = left + iconSize
            val bottom = top + iconSize
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapReady = true

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        map.setOnMarkerClickListener { marker ->
            val affiliateInfo = marker.tag as? AffiliateInfo

            when {
                affiliateInfo != null -> showAffiliateDetailsDialog(affiliateInfo)
                else -> marker.showInfoWindow()
            }

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 16f))
            true
        }

        map.setOnInfoWindowClickListener { marker ->
            val affiliateInfo = marker.tag as? AffiliateInfo
            if (affiliateInfo != null) {
                showAffiliateDetailsDialog(affiliateInfo)
            }
        }

        // Verificar si hay coordenadas específicas para mostrar
        if (targetLatitude != null && targetLongitude != null) {
            showSpecificLocation(targetLatitude!!, targetLongitude!!, targetName ?: "Negocio")
        } else {
            // Si no, mostrar Lima por defecto
            val lima = LatLng(-12.0464, -77.0428)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12f))
        }

        setupMapLocation()
        loadAffiliatesOnMap()
    }

    // Función para mostrar ubicación específica (solo negocios)
    private fun showSpecificLocation(lat: Double, lng: Double, title: String) {
        val location = LatLng(lat, lng)

        // Ícono naranja para negocios
        val markerIcon = createMarkerFromDrawable(
            R.drawable.ic_pin,
            Color.parseColor("#FF6B35")
        )

        // Agregar marcador destacado
        val marker = map.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .snippet("📍 Toca para más información")
                .icon(markerIcon)
        )

        marker?.showInfoWindow()

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(location, 17f),
            1000,
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {}
                override fun onCancel() {}
            }
        )

        // Actualizar la información de ubicación
        tvLocationCoords.text = "Lat: ${String.format("%.6f", lat)}, Lng: ${String.format("%.6f", lng)}"

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = buildString {
                    if (address.thoroughfare != null) append("${address.thoroughfare}, ")
                    if (address.subLocality != null) append("${address.subLocality}, ")
                    if (address.locality != null) append("${address.locality}")
                }
                tvLocationAddress.text = if (addressText.isNotEmpty()) addressText else title
            } else {
                tvLocationAddress.text = title
            }
        } catch (e: Exception) {
            tvLocationAddress.text = title
        }

        locationInfoCard.visibility = LinearLayout.VISIBLE
    }

    private fun setupMapLocation() {
        if (hasLocationPermission) {
            enableMyLocation()
            getCurrentLocation()
        } else {
            checkLocationPermission()
        }
    }

    private fun enableMyLocation() {
        try {
            if (hasLocationPermission && mapReady) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = false
            }
        } catch (e: SecurityException) {
            showToast("Error de permisos de ubicación")
        }
    }

    private fun checkLocationPermission() {
        if (hasLocationPermission()) {
            hasLocationPermission = true
            if (mapReady) {
                enableMyLocation()
                getCurrentLocation()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasLocationPermission = true
                if (mapReady) {
                    enableMyLocation()
                    getCurrentLocation()
                }
            } else {
                hasLocationPermission = false
                showToast("Permiso de ubicación denegado")
            }
        }
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission) {
            checkLocationPermission()
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Ya no agregamos marcador porque isMyLocationEnabled muestra la bolita azul
                    updateLocationInfo(location)
                } else {
                    showToast("No se pudo obtener la ubicación")
                }
            }.addOnFailureListener { e ->
                showToast("Error al obtener ubicación: ${e.message}")
            }
        } catch (e: SecurityException) {
            showToast("Error de permisos")
        }
    }

    private fun updateLocationInfo(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        tvLocationCoords.text = "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}"

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = buildString {
                    if (address.thoroughfare != null) append("${address.thoroughfare}, ")
                    if (address.subLocality != null) append("${address.subLocality}, ")
                    if (address.locality != null) append("${address.locality}")
                }
                tvLocationAddress.text = if (addressText.isNotEmpty()) addressText else "Dirección no disponible"
            } else {
                tvLocationAddress.text = "Dirección no disponible"
            }
        } catch (e: Exception) {
            tvLocationAddress.text = "Error al obtener dirección"
        }

        locationInfoCard.visibility = LinearLayout.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showCallDialog(phoneNumber: String, businessName: String) {
        AlertDialog.Builder(this)
            .setTitle("Contactar")
            .setMessage("¿Deseas llamar a:\n$businessName?\n\nTeléfono: $phoneNumber")
            .setPositiveButton("Llamar") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = android.net.Uri.parse("tel:$phoneNumber")
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAffiliateDetailsDialog(affiliateInfo: AffiliateInfo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_details, null)

        val ivDialogIcon = dialogView.findViewById<ImageView>(R.id.ivDialogIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvDialogInfo)
        val viewPager = dialogView.findViewById<ViewPager2>(R.id.viewPagerPhotos)
        val photoIndicator = dialogView.findViewById<TextView>(R.id.tvPhotoIndicator)
        val photoContainer = dialogView.findViewById<View>(R.id.photoContainer)
        val btnCall = dialogView.findViewById<View>(R.id.btnCall)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)

        // Establecer ícono según tipo de afiliado
        when (affiliateInfo.type) {
            "veterinaria" -> ivDialogIcon.setImageResource(R.drawable.ic_vet)
            "tienda" -> ivDialogIcon.setImageResource(R.drawable.ic_shop)
            "albergue" -> ivDialogIcon.setImageResource(R.drawable.ic_rescue)
            else -> ivDialogIcon.setImageResource(R.drawable.ic_pin)
        }

        tvTitle.text = affiliateInfo.businessName

        val message = buildString {
            append("🏢 ${affiliateInfo.businessName}\n")
            append("📊 Tipo: ${getAffiliateTypeText(affiliateInfo.type)}\n\n")

            if (affiliateInfo.description.isNotEmpty()) {
                append("📝 Descripción:\n${affiliateInfo.description}\n\n")
            }

            append("👤 Encargado: ${affiliateInfo.contactPerson}\n")
            append("📞 Teléfono: ${affiliateInfo.phone}\n")
            append("📍 Dirección: ${affiliateInfo.address}\n\n")

            if (affiliateInfo.hours.isNotEmpty()) {
                append("🕐 Horario: ${affiliateInfo.hours}\n")
            }

            if (affiliateInfo.socialMedia.isNotEmpty()) {
                append("🌐 ${affiliateInfo.socialMedia}")
            }
        }

        tvInfo.text = message

        // Cargar foto principal si existe
        if (affiliateInfo.mainPhotoUrl.isNotEmpty()) {
            val photoUrls = listOf(affiliateInfo.mainPhotoUrl)
            photoContainer.visibility = View.VISIBLE
            val adapter = PhotoPagerAdapter(photoUrls)
            viewPager.adapter = adapter
            photoIndicator.text = "1 / 1"
        } else {
            loadAffiliatePhotos(affiliateInfo.documentId) { photoUrls ->
                if (photoUrls.isNotEmpty()) {
                    photoContainer.visibility = View.VISIBLE
                    val adapter = PhotoPagerAdapter(photoUrls)
                    viewPager.adapter = adapter
                    photoIndicator.text = "1 / ${photoUrls.size}"

                    viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            photoIndicator.text = "${position + 1} / ${photoUrls.size}"
                        }
                    })
                } else {
                    photoContainer.visibility = View.GONE
                }
            }
        }

        btnCall.visibility = View.VISIBLE
        btnCall.setOnClickListener {
            showCallDialog(affiliateInfo.phone, affiliateInfo.businessName)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadAffiliatePhotos(documentId: String, callback: (List<String>) -> Unit) {
        val storage = FirebaseStorage.getInstance()
        val affiliateRef = storage.reference.child("affiliates/$documentId/photos")

        affiliateRef.listAll()
            .addOnSuccessListener { listResult ->
                val urls = mutableListOf<String>()
                var processedCount = 0

                if (listResult.items.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                listResult.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        urls.add(uri.toString())
                        processedCount++
                        if (processedCount == listResult.items.size) {
                            callback(urls.sorted())
                        }
                    }.addOnFailureListener {
                        processedCount++
                        if (processedCount == listResult.items.size) {
                            callback(urls.sorted())
                        }
                    }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    inner class PhotoPagerAdapter(private val photoUrls: List<String>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.imageViewPhoto)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_page, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(photoUrls[position])
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = photoUrls.size
    }

    data class AffiliateInfo(
        val type: String,
        val businessName: String,
        val contactPerson: String,
        val phone: String,
        val address: String,
        val description: String,
        val hours: String,
        val socialMedia: String,
        val verified: Boolean,
        val documentId: String,
        val mainPhotoUrl: String
    )

    private fun loadAffiliatesOnMap() {
        val db = FirebaseFirestore.getInstance()

        db.collection("affiliates")
            .whereEqualTo("status", "approved")
            .whereEqualTo("verified", true)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val affiliateData = document.data

                    val latitude = affiliateData["latitude"] as? Double
                    val longitude = affiliateData["longitude"] as? Double

                    if (latitude != null && longitude != null) {
                        val affiliateLocation = LatLng(latitude, longitude)

                        val type = affiliateData["type"] as? String ?: "unknown"
                        val businessName = affiliateData["businessName"] as? String ?: "Negocio"
                        val contactPerson = affiliateData["contactPerson"] as? String ?: ""
                        val phone = affiliateData["phone"] as? String ?: ""
                        val address = affiliateData["address"] as? String ?: ""
                        val description = affiliateData["description"] as? String ?: ""
                        val hours = affiliateData["hours"] as? String ?: ""
                        val socialMedia = affiliateData["socialMedia"] as? String ?: ""
                        val verified = affiliateData["verified"] as? Boolean ?: false
                        val mainPhotoUrl = affiliateData["mainPhotoUrl"] as? String ?: ""

                        val markerTitle = getAffiliateTypeEmoji(type) + " " + businessName
                        val markerSnippet = getAffiliateTypeText(type) + "\n📞 ${phone}\n💬 Toca para más detalles"

                        val fullAffiliateInfo = AffiliateInfo(
                            type = type,
                            businessName = businessName,
                            contactPerson = contactPerson,
                            phone = phone,
                            address = address,
                            description = description,
                            hours = hours,
                            socialMedia = socialMedia,
                            verified = verified,
                            documentId = document.id,
                            mainPhotoUrl = mainPhotoUrl
                        )

                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(affiliateLocation)
                                .title(markerTitle)
                                .snippet(markerSnippet)
                                .icon(getMarkerIconByAffiliateType(type))
                        )

                        marker?.tag = fullAffiliateInfo
                        marker?.let { affiliateMarkers.add(it) }
                    }
                }
            }
            .addOnFailureListener { e ->
                showToast("Error al cargar negocios: ${e.message}")
            }
    }

    private fun getAffiliateTypeText(type: String): String {
        return when (type) {
            "veterinaria" -> "Veterinaria"
            "tienda" -> "Tienda de Mascotas"
            "albergue" -> "Albergue / Refugio"
            else -> "Negocio"
        }
    }

    private fun getAffiliateTypeEmoji(type: String): String {
        return when (type) {
            "veterinaria" -> "🏥"
            "tienda" -> "🛍️"
            "albergue" -> "🏠"
            else -> "📍"
        }
    }

    private fun getMarkerIconByAffiliateType(type: String): BitmapDescriptor {
        return when (type) {
            "veterinaria" -> createMarkerFromDrawable(R.drawable.ic_vet, Color.parseColor("#26C6DA"))
            "tienda" -> createMarkerFromDrawable(R.drawable.ic_shop, Color.parseColor("#AB47BC"))
            "albergue" -> createMarkerFromDrawable(R.drawable.ic_rescue, Color.parseColor("#66BB6A"))
            else -> createMarkerFromDrawable(R.drawable.ic_pin, Color.parseColor("#42A5F5"))
        }
    }
}