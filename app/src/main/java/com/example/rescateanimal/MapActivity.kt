package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLocationAddress: TextView
    private lateinit var tvLocationCoords: TextView
    private lateinit var locationInfoCard: LinearLayout

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var hasLocationPermission = false
    private var mapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize views
        tvLocationAddress = findViewById(R.id.tvLocationAddress)
        tvLocationCoords = findViewById(R.id.tvLocationCoords)
        locationInfoCard = findViewById(R.id.locationInfoCard)

        // Check permission status
        hasLocationPermission = hasLocationPermission()

        // Setup map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup UI
        setupUI()

        // Setup navigation
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning to activity
        val currentPermissionStatus = hasLocationPermission()
        if (currentPermissionStatus != hasLocationPermission) {
            hasLocationPermission = currentPermissionStatus
            if (mapReady) {
                setupMapLocation()
            }
        }
    }

    private fun setupUI() {
        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnMyLocation = findViewById<TextView>(R.id.btnMyLocation)

        btnBack.setOnClickListener {
            finish()
        }

        btnMyLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun setupBottomNavigation() {
        // Inicio
        findViewById<LinearLayout>(R.id.navInicio).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Mapa - Ya estamos aquí
        findViewById<LinearLayout>(R.id.navMapa).setOnClickListener {
            // Ya estamos en mapa, no hacer nada
        }

        // Reportar
        findViewById<LinearLayout>(R.id.navReportar).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        // Adoptar
        findViewById<LinearLayout>(R.id.navAdoptar).setOnClickListener {
            Toast.makeText(this, "Adoptar - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Perfil
        findViewById<LinearLayout>(R.id.navPerfil).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapReady = true

        // Enable zoom controls and info windows
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        // Configurar clicks en los marcadores para mostrar más información
        map.setOnMarkerClickListener { marker ->
            // Si es el marcador de "Tu ubicación", comportamiento normal
            if (marker.title == "Tu ubicación") {
                return@setOnMarkerClickListener false
            }

            // Para marcadores de reportes, mostrar diálogo con información completa
            val reportInfo = marker.tag as? ReportInfo
            if (reportInfo != null) {
                showReportDetailsDialog(reportInfo)
            } else {
                // Fallback para marcadores sin información completa
                marker.showInfoWindow()
            }

            // Centrar la cámara en el marcador seleccionado
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 16f))

            true // Retornar true para manejar el click nosotros
        }

        // Configurar clicks en las ventanas de información (para el snippet corto)
        map.setOnInfoWindowClickListener { marker ->
            val reportInfo = marker.tag as? ReportInfo
            if (reportInfo != null) {
                showReportDetailsDialog(reportInfo)
            } else {
                // Extraer número de teléfono del snippet si existe
                val snippet = marker.snippet ?: ""
                val phoneRegex = """📞 ([+]?\d+)""".toRegex()
                val phoneMatch = phoneRegex.find(snippet)

                if (phoneMatch != null) {
                    val phoneNumber = phoneMatch.groupValues[1]
                    showCallDialog(phoneNumber, marker.title ?: "Reporte")
                } else {
                    showToast("No hay información adicional disponible")
                }
            }
        }

        // Set default location (Lima, Peru)
        val lima = LatLng(-12.0464, -77.0428)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12f))

        // Setup map location based on permissions
        setupMapLocation()

        // Cargar reportes en el mapa
        loadReportsOnMap()
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
                map.uiSettings.isMyLocationButtonEnabled = false // We have our own button
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
        permissions: Array<out String>,
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

                    // Move camera to current location
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Add marker for current location
                    map.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Tu ubicación")
                            .snippet("Estás aquí")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE))
                    )

                    // Update location info
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

        // Show coordinates
        tvLocationCoords.text = "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}"

        // Get address using Geocoder
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

        // Show location info card
        locationInfoCard.visibility = LinearLayout.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showCallDialog(phoneNumber: String, reportTitle: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Contactar al reportador")
            .setMessage("¿Deseas llamar al reportador de:\n$reportTitle?\n\nTéléfono: $phoneNumber")
            .setPositiveButton("Llamar") { _, _ ->
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                intent.data = android.net.Uri.parse("tel:$phoneNumber")
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showReportDetailsDialog(reportInfo: ReportInfo) {
        val message = buildString {
            append("📅 Fecha: ${reportInfo.dateString}\n")
            append("📊 Estado: ${getStatusText(reportInfo.status)}\n\n")

            if (reportInfo.description.isNotEmpty()) {
                append("📝 Descripción:\n${reportInfo.description}\n\n")
            }

            if (reportInfo.location.isNotEmpty()) {
                append("📍 Ubicación específica:\n${reportInfo.location}\n\n")
            }

            append("👤 Reportado por: ${reportInfo.userEmail.substringBefore("@")}\n")

            if (reportInfo.contactPhone.isNotEmpty()) {
                append("📞 Teléfono: ${reportInfo.contactPhone}\n")
            } else {
                append("📧 Sin teléfono de contacto\n")
            }
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(reportInfo.title)
            .setMessage(message)
            .setPositiveButton("Cerrar", null)

        // Agregar botón de llamada si hay teléfono
        if (reportInfo.contactPhone.isNotEmpty()) {
            dialog.setNeutralButton("📞 Llamar") { _, _ ->
                showCallDialog(reportInfo.contactPhone, reportInfo.title)
            }
        }

        dialog.show()
    }

    // Clase de datos para almacenar información completa del reporte
    data class ReportInfo(
        val type: String,
        val title: String,
        val description: String,
        val location: String,
        val contactPhone: String,
        val status: String,
        val userEmail: String,
        val dateString: String,
        val documentId: String
    )

    private fun loadReportsOnMap() {
        // Cargar reportes desde Firestore
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("reports")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val reportData = document.data

                    // Obtener coordenadas del reporte
                    val latitude = reportData["latitude"] as? Double
                    val longitude = reportData["longitude"] as? Double

                    if (latitude != null && longitude != null) {
                        val reportLocation = LatLng(latitude, longitude)

                        // Datos del reporte (usando los campos que realmente tienes)
                        val reportType = reportData["reportType"] as? String ?: "unknown"
                        val description = reportData["description"] as? String ?: ""
                        val location = reportData["location"] as? String ?: ""
                        val contactPhone = reportData["contactPhone"] as? String ?: ""
                        val status = reportData["status"] as? String ?: "pending"
                        val userEmail = reportData["userEmail"] as? String ?: "Usuario"
                        val createdAt = reportData["createdAt"] as? Long

                        // Formatear fecha
                        val dateString = if (createdAt != null) {
                            val date = java.util.Date(createdAt)
                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(date)
                        } else "Fecha desconocida"

                        // Crear título del marcador (más corto para que se vea)
                        val markerTitle = getReportTypeTitle(reportType)

                        // Snippet más corto para evitar truncamiento
                        val markerSnippet = buildString {
                            append("📅 ${dateString}\n")
                            append("📊 ${getStatusText(status)}")
                            if (contactPhone.isNotEmpty()) {
                                append("\n📞 ${contactPhone}")
                            }
                            append("\n💬 Toca para ver más detalles")
                        }

                        // Guardar toda la información en el tag del marcador
                        val fullReportInfo = ReportInfo(
                            type = reportType,
                            title = markerTitle,
                            description = description,
                            location = location,
                            contactPhone = contactPhone,
                            status = status,
                            userEmail = userEmail,
                            dateString = dateString,
                            documentId = document.id
                        )

                        // Agregar marcador al mapa con información completa
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(reportLocation)
                                .title(markerTitle)
                                .snippet(markerSnippet)
                                .icon(getMarkerIconByReportType(reportType))
                        )

                        // Asociar información completa al marcador
                        marker?.tag = fullReportInfo
                    }
                }

                if (documents.isEmpty()) {
                    showToast("No hay reportes disponibles en el mapa")
                } else {
                    showToast("Se cargaron ${documents.size()} reportes en el mapa")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error al cargar reportes: ${e.message}")
            }
    }

    private fun getReportTypeTitle(reportType: String): String {
        return when (reportType) {
            "danger" -> "🆘 Animal en Peligro"
            "lost" -> "🔍 Animal Perdido"
            "abandoned" -> "🏠 Animal Abandonado"
            else -> "📍 Reporte de Animal"
        }
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "pending" -> "Pendiente"
            "reviewed" -> "Revisado"
            "resolved" -> "Resuelto"
            else -> "Desconocido"
        }
    }

    private fun getMarkerIconByReportType(reportType: String): com.google.android.gms.maps.model.BitmapDescriptor {
        return when (reportType) {
            "danger" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
            "lost" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW)
            "abandoned" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE)
            else -> com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE)
        }
    }
}