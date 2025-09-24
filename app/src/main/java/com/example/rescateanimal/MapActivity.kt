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
    private lateinit var navigationHelper: NavigationHelper

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
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapReady = true

        // Enable zoom controls
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        // Set default location (Lima, Peru)
        val lima = LatLng(-12.0464, -77.0428)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12f))

        // Setup map location based on permissions
        setupMapLocation()
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
                    map.clear()
                    map.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Tu ubicación")
                            .snippet("Estás aquí")
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
}