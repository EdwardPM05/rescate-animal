package com.example.rescateanimal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Button
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
import java.util.*

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvSelectedAddress: TextView
    private lateinit var tvSelectedCoords: TextView
    private lateinit var btnConfirmLocation: Button

    private var selectedLatLng: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tvSelectedAddress = findViewById(R.id.tvSelectedAddress)
        tvSelectedCoords = findViewById(R.id.tvSelectedCoords)
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
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

        btnConfirmLocation.setOnClickListener {
            confirmLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        // Configurar listener para cuando el usuario mueva el mapa
        map.setOnCameraIdleListener {
            val centerLatLng = map.cameraPosition.target
            selectedLatLng = centerLatLng
            updateLocationInfo(centerLatLng)
        }

        // Verificar si hay una ubicación inicial pasada desde el intent
        val initialLat = intent.getDoubleExtra("latitude", -12.0464)
        val initialLng = intent.getDoubleExtra("longitude", -77.0428)
        val initialLatLng = LatLng(initialLat, initialLng)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 15f))
        selectedLatLng = initialLatLng
        updateLocationInfo(initialLatLng)

        // Habilitar ubicación si hay permisos
        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            checkLocationPermission()
        }
    }

    private fun enableMyLocation() {
        try {
            if (hasLocationPermission()) {
                map.isMyLocationEnabled = true
            }
        } catch (e: SecurityException) {
            showToast("Error de permisos de ubicación")
        }
    }

    private fun checkLocationPermission() {
        if (!hasLocationPermission()) {
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
                enableMyLocation()
                getCurrentLocation()
            } else {
                showToast("Permiso de ubicación denegado")
            }
        }
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            checkLocationPermission()
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                    selectedLatLng = currentLatLng
                    updateLocationInfo(currentLatLng)
                } else {
                    showToast("No se pudo obtener la ubicación actual")
                }
            }.addOnFailureListener { e ->
                showToast("Error al obtener ubicación: ${e.message}")
            }
        } catch (e: SecurityException) {
            showToast("Error de permisos")
        }
    }

    private fun updateLocationInfo(latLng: LatLng) {
        val latitude = latLng.latitude
        val longitude = latLng.longitude

        tvSelectedCoords.text = "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}"

        // Obtener dirección usando Geocoder
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

                tvSelectedAddress.text = if (addressText.isNotEmpty()) {
                    addressText
                } else {
                    "Dirección no disponible"
                }
            } else {
                tvSelectedAddress.text = "Arrastra el mapa para seleccionar"
            }
        } catch (e: Exception) {
            tvSelectedAddress.text = "Arrastra el mapa para seleccionar"
        }
    }

    private fun confirmLocation() {
        if (selectedLatLng == null) {
            showToast("Por favor selecciona una ubicación")
            return
        }

        val resultIntent = Intent()
        resultIntent.putExtra("latitude", selectedLatLng!!.latitude)
        resultIntent.putExtra("longitude", selectedLatLng!!.longitude)
        resultIntent.putExtra("address", tvSelectedAddress.text.toString())

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}