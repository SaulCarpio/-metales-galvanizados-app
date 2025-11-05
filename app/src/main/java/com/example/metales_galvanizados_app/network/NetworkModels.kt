package com.example.metales_galvanizados_app.network

import com.google.gson.annotations.SerializedName

// --- Modelos para Login ---
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: String?,
    val role: String?,
    @SerializedName("rol_id") val rolId: Int?,
    @SerializedName("redirect_url") val redirectUrl: String?,
    @SerializedName("change_required") val changeRequired: Boolean?
)

// --- Modelos para Rutas ---

// ▼▼▼ ESTA ES LA CLASE MODIFICADA ▼▼▼
data class FindRouteRequest(
    // Ahora espera una única lista llamada "waypoints"
    val waypoints: List<List<Double>>
)
// ▲▲▲ FIN DE LA MODIFICACIÓN ▲▲▲

data class RouteData(
    val coordinates: List<List<Double>>,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("predicted_time_min") val predictedTimeMin: Double
)

data class FindRouteResponse(
    val success: Boolean,
    val route: RouteData?,
    val message: String?
)