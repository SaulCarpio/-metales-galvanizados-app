package com.example.metales_galvanizados_app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/find-route")
    suspend fun findRoute(@Body request: FindRouteRequest): Response<FindRouteResponse>
}