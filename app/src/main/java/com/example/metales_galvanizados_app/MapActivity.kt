package com.example.metales_galvanizados_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recomendado por osmdroid: definir userAgent
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        setContentView(R.layout.activity_map)

        // Inicializar mapa
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Centrar el mapa en La Paz
        val startPoint = GeoPoint(-16.5, -68.2036)
        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        // Agregar las dos fábricas
        addFactory(-16.482324, -68.242357, "Fábrica 1")
        addFactory(-16.520538, -68.231788, "Fábrica 2")

        // Configurar botón de cerrar sesión
        setupLogoutButton()

        // Obtener información del usuario (opcional)
        val username = intent.getStringExtra("username") ?: "Usuario"
        val userType = intent.getStringExtra("user_type") ?: "user"

        // Mostrar mensaje de bienvenida
        Toast.makeText(this, "Bienvenido, $username", Toast.LENGTH_SHORT).show()
    }

    private fun setupLogoutButton() {
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun logout() {
        // Limpiar preferencias si las hay
        getSharedPreferences("user_session", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Ir al login y limpiar stack de actividades
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        finish()
    }

    // Manejo del botón de atrás
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            // Si se presiona atrás dos veces en menos de 2 segundos, confirmar salida
            showLogoutConfirmation()
        } else {
            // Mostrar mensaje para presionar de nuevo
            Toast.makeText(this, "Presiona de nuevo para cerrar sesión", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    private fun addFactory(latitude: Double, longitude: Double, title: String) {
        val factoryPoint = GeoPoint(latitude, longitude)

        // Crear marcador para la fábrica
        val marker = Marker(map)
        marker.position = factoryPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        marker.snippet = "Coordenadas: $latitude, $longitude"

        // Crear círculo de 50 metros de radio
        val circle = Polygon(map)
        val circlePoints = createCircle(factoryPoint, 50.0) // 50 metros de radio
        circle.points = circlePoints
        circle.fillColor = Color.argb(50, 0, 0, 255) // Azul semi-transparente
        circle.strokeColor = Color.BLUE
        circle.strokeWidth = 50f

        // Agregar al mapa
        map.overlays.add(circle)
        map.overlays.add(marker)
    }

    private fun createCircle(center: GeoPoint, radiusInMeters: Double): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val earthRadius = 6378137.0 // Radio de la Tierra en metros

        for (i in 0..360 step 10) {
            val angle = Math.toRadians(i.toDouble())

            // Calcular el desplazamiento en latitud y longitud
            val deltaLat = (radiusInMeters * Math.cos(angle)) / earthRadius
            val deltaLon = (radiusInMeters * Math.sin(angle)) /
                    (earthRadius * Math.cos(Math.toRadians(center.latitude)))

            val lat = center.latitude + Math.toDegrees(deltaLat)
            val lon = center.longitude + Math.toDegrees(deltaLon)

            points.add(GeoPoint(lat, lon))
        }

        return points
    }
}