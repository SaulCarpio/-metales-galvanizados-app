// HeatmapOverlay.kt
package com.example.metales_galvanizados_app

import android.content.Context
import android.graphics.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay

/**
 * HeatmapOverlay:
 * - points: lista de GeoPoint que representan ferias
 * - radiusMeters: radio en metros para cada "mancha" de contribución
 */
class HeatmapOverlay(
    private val ctx: Context,
    var points: List<GeoPoint>,
    private val radiusMeters: Double = 150.0
) : Overlay() {

    private var cacheBitmap: Bitmap? = null
    private var cacheZoom: Double = -1.0
    private var cacheWidth: Int = 0
    private var cacheHeight: Int = 0

    // Gradiente de colores para mapear intensidad (0..255) -> color
    private val colorRamp: IntArray by lazy {
        // Simple ramp: azul -> cyan -> green -> yellow -> red
        val ramp = IntArray(256)
        for (i in 0..255) {
            val t = i / 255.0f
            // interpolation: blue to red via green (simple)
            val r = (255 * Math.min(1.0f, t * 2.0f)).toInt()
            val g = (255 * Math.max(0f, 1f - Math.abs(t - 0.5f) * 2f)).toInt()
            val b = (255 * Math.max(0f, 1f - t * 2f)).toInt()
            ramp[i] = Color.argb(255, r, g, b)
        }
        ramp
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val w = mapView.width
        val h = mapView.height
        if (w <= 0 || h <= 0) return

        // Rebuild cache if cambia tamaño o zoom
        val zoom = mapView.zoomLevelDouble
        if (cacheBitmap == null || cacheWidth != w || cacheHeight != h || cacheZoom != zoom) {
            cacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cacheWidth = w
            cacheHeight = h
            cacheZoom = zoom
            renderHeatmap(cacheBitmap!!, mapView.projection)
        }

        cacheBitmap?.let {
            // dibujar bitmap colorizado sobre el canvas
            val paint = Paint()
            paint.isFilterBitmap = true
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
    }

    /**
     * Renderiza el heatmap en escala de grises y luego lo colorea
     */
    private fun renderHeatmap(bitmap: Bitmap, proj: Projection) {
        // Flecha: primero limpiar a transparente
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val tmpBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
        val tmpCanvas = Canvas(tmpBitmap)

        // Pintura para dibujar las manchas en escala de grises (alpha)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        // Blur: para suavizar
        paint.maskFilter = BlurMaskFilter((radiusMetersToPixels(proj, radiusMeters) / 2f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)

        // Dibujamos una mancha por cada punto (en ALPHA_8)
        for (p in points) {
            val pt = proj.toPixels(p, null)
            // Si punto fuera fuera del bitmap, igual dejamos (no dibuja)
            tmpCanvas.drawCircle(pt.x.toFloat(), pt.y.toFloat(), radiusMetersToPixels(proj, radiusMeters).toFloat(), paint)
        }

        // Ahora tmpBitmap contiene intensidades en canal alfa. Convertimos a ARGB coloreado.
        // Recorremos pixeles y aplicamos rampa de color según alpha.
        val w = tmpBitmap.width
        val h = tmpBitmap.height
        val outPixels = IntArray(w * h)
        val alphaPixels = IntArray(w * h)
        tmpBitmap.getPixels(alphaPixels, 0, w, 0, 0, w, h)

        // alphaPixels tiene valores ARGB en formato ALPHA_8 bitmap: pero getPixels devuelve ints con alpha in high byte
        for (i in 0 until w * h) {
            val px = alphaPixels[i]
            val alpha = (px ushr 24) and 0xFF
            if (alpha == 0) {
                outPixels[i] = 0x00000000
            } else {
                // map alpha (0..255) -> colorRamp
                val col = colorRamp[alpha]
                // conservar alpha proporcional para visibilidad
                val a = (alpha.toFloat() * 0.9f).toInt().coerceIn(0, 255)
                outPixels[i] = (a shl 24) or (col and 0x00FFFFFF)
            }
        }

        bitmap.setPixels(outPixels, 0, w, 0, 0, w, h)

        // Liberar tmpBitmap
        tmpBitmap.recycle()
    }

    /**
     * Convierte radiom en metros a pixeles usando la proyección actual
     */
    private fun radiusMetersToPixels(proj: Projection, meters: Double): Int {
        // Tomamos el punto del centro del mapa y calculamos un desplazamiento hacia el norte de "meters"
        val centerGeo = proj.boundingBox.centerWithDateLine
        val pCenter = proj.toPixels(centerGeo, null)
        val northPoint = GeoPoint(centerGeo.latitude + (meters / 111320.0), centerGeo.longitude) // approx
        val pNorth = proj.toPixels(northPoint, null)
        val dy = Math.abs(pCenter.y - pNorth.y)
        return dy.toInt().coerceAtLeast(2)
    }
}
