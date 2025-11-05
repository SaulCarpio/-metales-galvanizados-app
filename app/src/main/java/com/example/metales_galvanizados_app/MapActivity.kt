package com.example.metales_galvanizados_app

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.metales_galvanizados_app.network.FindRouteRequest
import com.example.metales_galvanizados_app.network.RetrofitClient
import com.example.metales_galvanizados_app.network.RouteData
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var progressBar: ProgressBar
    private lateinit var infoPanel: LinearLayout
    private lateinit var tvRouteInfo: TextView
    private lateinit var btnAddPoint: Button
    private lateinit var btnCalculateRoute: Button
    private lateinit var btnClearMap: Button

    private val waypoints = mutableListOf<GeoPoint>()
    private val pointMarkers = mutableListOf<Marker>()
    private val routeSegments = mutableListOf<Polyline>()

    private var isAddingPoints = false
    private var requestStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // <-- MEJORA IMPORTANTE: Configurar el User Agent para osmdroid
        // Esto evita que te bloqueen los servidores de mapas. Es crucial.
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        setContentView(R.layout.activity_map)

        // Inicializar Vistas
        map = findViewById(R.id.map)
        progressBar = findViewById(R.id.progressBar)
        infoPanel = findViewById(R.id.infoPanel)
        tvRouteInfo = findViewById(R.id.tvRouteInfo)
        btnAddPoint = findViewById(R.id.btnAddPoint)
        btnCalculateRoute = findViewById(R.id.btnCalculateRoute)
        btnClearMap = findViewById(R.id.btnClearMap)

        // <-- CORRECCIÓN CLAVE: Añadir el punto inicial ANTES de configurar el mapa
        val initialPoint = GeoPoint(-16.482466, -68.242357)
        if (waypoints.isEmpty()) {
            waypoints.add(initialPoint)
        }

        // Ahora llamamos a las funciones de configuración
        setupMap(initialPoint) // Le pasamos el punto para asegurar que el centro es correcto
        setupButtons()
        updateMarkersOnMap() // Dibujar el marcador inicial
        requestLocationPermissions()

        val username = intent.getStringExtra("username") ?: "Usuario"
        Toast.makeText(this, "Bienvenido, $username. Presiona 'Agregar Punto' para empezar.", Toast.LENGTH_LONG).show()
    }

    // <-- CORRECCIÓN: Acepta el punto de inicio para centrar el mapa
    private fun setupMap(centerPoint: GeoPoint) {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(13.5)
        map.controller.setCenter(centerPoint) // Ahora es seguro usarlo

        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { handleMapTap(it) }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        map.overlays.add(0, mapEventsOverlay)
    }

    private fun setupButtons() {
        btnAddPoint.setOnClickListener {
            isAddingPoints = !isAddingPoints
            if (isAddingPoints) {
                btnAddPoint.text = "Dejar de Agregar"
                // Es mejor usar colores definidos en colors.xml
                btnAddPoint.background.setTint(ContextCompat.getColor(this, R.color.colorAccent))
                Toast.makeText(this, "Modo de agregar puntos activado.", Toast.LENGTH_SHORT).show()
            } else {
                btnAddPoint.text = "Agregar Punto"
                btnAddPoint.background.setTint(ContextCompat.getColor(this, R.color.colorPrimary))
            }
        }

        btnCalculateRoute.setOnClickListener {
            if (waypoints.size < 2) {
                Toast.makeText(this, "Necesitas al menos 2 puntos para calcular la ruta.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(isAddingPoints) { // Salir del modo de agregar al calcular
                isAddingPoints = false
                btnAddPoint.text = "Agregar Punto"
                btnAddPoint.background.setTint(ContextCompat.getColor(this, R.color.colorPrimary))
            }
            findOptimalRouteApiCall()
        }

        btnClearMap.setOnClickListener {
            clearMap()
        }
    }

    // --- El resto del código permanece igual ---

    private fun handleMapTap(tappedPoint: GeoPoint) {
        if (isAddingPoints) {
            waypoints.add(tappedPoint)
            updateMarkersOnMap()
        }
    }

    private fun updateMarkersOnMap() {
        pointMarkers.forEach { map.overlays.remove(it) }
        pointMarkers.clear()

        waypoints.forEachIndexed { index, point ->
            val title: String
            val iconDrawable: BitmapDrawable

            if (index == 0) {
                title = "INICIO"
                iconDrawable = createMarkerIcon(title, Color.RED, 70) // Más grande
            } else {
                title = "P$index"
                iconDrawable = createMarkerIcon(title, Color.BLUE, 55)
            }

            val marker = Marker(map).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                this.title = title
                icon = iconDrawable
            }
            map.overlays.add(marker)
            pointMarkers.add(marker)
        }
        map.invalidate()
    }

    private fun createMarkerIcon(text: String, color: Int, size: Int): BitmapDrawable {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = size / 2.8f
            textAlign = Paint.Align.CENTER
        }
        val textY = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, size / 2f, textY, textPaint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun findOptimalRouteApiCall() {
        progressBar.visibility = View.VISIBLE
        infoPanel.visibility = View.GONE
        clearRoute()
        requestStartTime = System.currentTimeMillis()

        lifecycleScope.launch {
            try {
                val waypointsPayload = waypoints.map { listOf(it.latitude, it.longitude) }
                val request = FindRouteRequest(waypoints = waypointsPayload)

                val response = RetrofitClient.instance.findRoute(request)
                val latency = System.currentTimeMillis() - requestStartTime

                if (response.isSuccessful) {
                    val routeResponse = response.body()
                    if (routeResponse?.success == true && routeResponse.route != null) {
                        drawGradientRoute(routeResponse.route)
                        displayRouteInfo(routeResponse.route, latency)
                    } else {
                        Toast.makeText(this@MapActivity, "Error: ${routeResponse?.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MapActivity, "Error de servidor: ${response.code()}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MapActivity, "No se pudo conectar al servidor.", Toast.LENGTH_LONG).show()
                Log.e("MapActivity", "Error de red: ${e.message}", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun drawGradientRoute(route: RouteData) {
        val routePoints = route.coordinates.map { GeoPoint(it[0], it[1]) }
        if (routePoints.size < 2) return

        clearRoute() // Limpiar segmentos previos
        val totalSegments = routePoints.size - 1
        for (i in 0 until totalSegments) {
            val segment = Polyline().apply {
                addPoint(routePoints[i])
                addPoint(routePoints[i+1])
                val percentage = i.toFloat() / totalSegments
                color = getColorForPercentage(percentage)
                width = 12.0f
            }
            map.overlays.add(segment)
            routeSegments.add(segment)
        }

        map.invalidate()
        // Crear una polilínea invisible solo para obtener sus límites y hacer zoom
        val boundsPolyline = Polyline().apply { setPoints(routePoints) }
        map.zoomToBoundingBox(boundsPolyline.bounds, true, 100)
    }

    private fun getColorForPercentage(p: Float): Int {
        val red = (255 * (1 - p)).toInt()
        val blue = (255 * p).toInt()
        return Color.rgb(red, 0, blue)
    }

    private fun displayRouteInfo(route: RouteData, latency: Long) {
        val distanceKm = "%.2f".format(route.distanceMeters / 1000)
        val timeMin = "%.0f".format(route.predictedTimeMin)
        val stops = waypoints.size

        val infoText = """
            📍 Distancia: $distanceKm km
            ⏱️ Tiempo estimado: $timeMin min
            🗺️ Paradas: $stops
            📡 Latencia: ${latency}ms
        """.trimIndent()

        tvRouteInfo.text = infoText
        infoPanel.visibility = View.VISIBLE
    }

    private fun clearRoute() {
        routeSegments.forEach { map.overlays.remove(it) }
        routeSegments.clear()
        infoPanel.visibility = View.GONE
        map.invalidate()
    }

    private fun clearMap() {
        clearRoute()
        if (waypoints.isNotEmpty()) {
            val firstPoint = waypoints[0]
            waypoints.clear()
            waypoints.add(firstPoint)
        }
        updateMarkersOnMap()
        if (waypoints.isNotEmpty()) {
            map.controller.setCenter(waypoints[0])
            map.controller.setZoom(13.5)
        }
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

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}