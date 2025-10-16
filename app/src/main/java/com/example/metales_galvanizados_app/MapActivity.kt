package com.example.metales_galvanizados_app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.metales_galvanizados_app.network.FindRouteRequest
import com.example.metales_galvanizados_app.network.RetrofitClient
import com.example.metales_galvanizados_app.network.RouteData
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline


// ===================================================================
// CLASE DE LA ACTIVIDAD DEL MAPA
// (NOTA: Todo el código de red ha sido movido a la carpeta 'network')
// ===================================================================
class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var progressBar: ProgressBar
    private lateinit var infoPanel: LinearLayout
    private lateinit var tvRouteInfo: TextView

    private val selectedPoints = mutableListOf<GeoPoint>()
    private val pointMarkers = mutableListOf<Marker>()
    private var routeOverlay: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        progressBar = findViewById(R.id.progressBar)
        infoPanel = findViewById(R.id.infoPanel)
        tvRouteInfo = findViewById(R.id.tvRouteInfo)

        setupMap()
        setupLogoutButton()
        requestLocationPermissions()

        val username = intent.getStringExtra("username") ?: "Usuario"
        Toast.makeText(this, "Bienvenido, $username. Toca 2 puntos para crear una ruta.", Toast.LENGTH_LONG).show()
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(13.5)
        map.controller.setCenter(GeoPoint(-16.5, -68.2036))

        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { handleMapTap(it) }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })
        map.overlays.add(0, mapEventsOverlay)
    }

    private fun handleMapTap(tappedPoint: GeoPoint) {
        if (selectedPoints.size >= 2) {
            clearMap()
        }

        selectedPoints.add(tappedPoint)
        addMarker(tappedPoint, "Punto ${selectedPoints.size}")

        if (selectedPoints.size == 2) {
            findRouteApiCall(selectedPoints[0], selectedPoints[1])
        }
    }

    private fun addMarker(point: GeoPoint, title: String) {
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        map.overlays.add(marker)
        pointMarkers.add(marker)
        map.invalidate()
    }

    private fun findRouteApiCall(origin: GeoPoint, destination: GeoPoint) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val request = FindRouteRequest(
                    origin = listOf(origin.latitude, origin.longitude),
                    destination = listOf(destination.latitude, destination.longitude)
                )

                // Esta llamada ahora funcionará sin errores de referencia
                val response = RetrofitClient.instance.findRoute(request)

                if (response.isSuccessful) {
                    val routeResponse = response.body()
                    if (routeResponse?.success == true && routeResponse.route != null) {
                        drawRoute(routeResponse.route)
                        displayRouteInfo(routeResponse.route)
                    } else {
                        Toast.makeText(this@MapActivity, "Error: ${routeResponse?.message}", Toast.LENGTH_LONG).show()
                        clearMap()
                    }
                } else {
                    Toast.makeText(this@MapActivity, "Error de servidor: ${response.code()}", Toast.LENGTH_LONG).show()
                    clearMap()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MapActivity, "No se pudo conectar al servidor.", Toast.LENGTH_LONG).show()
                Log.e("MapActivity", "Error de red: ${e.message}")
                clearMap()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun drawRoute(route: RouteData) {
        val routePoints = route.coordinates.map { GeoPoint(it[0], it[1]) }

        routeOverlay = Polyline().apply {
            setPoints(routePoints)
            color = Color.BLUE
            width = 10.0f
        }

        map.overlays.add(routeOverlay)
        map.invalidate()
        map.zoomToBoundingBox(routeOverlay?.bounds, true, 100)
    }

    private fun displayRouteInfo(route: RouteData) {
        val distanceKm = "%.2f".format(route.distanceMeters / 1000)
        val timeMin = "%.2f".format(route.predictedTimeMin)

        tvRouteInfo.text = "Distancia: $distanceKm km | Tiempo Estimado: $timeMin min"
        infoPanel.visibility = View.VISIBLE
    }

    private fun clearMap() {
        selectedPoints.clear()
        pointMarkers.forEach { map.overlays.remove(it) }
        pointMarkers.clear()
        routeOverlay?.let { map.overlays.remove(it) }
        routeOverlay = null
        infoPanel.visibility = View.GONE
        map.invalidate()
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 101)
        }
    }

    private fun setupLogoutButton() {
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}