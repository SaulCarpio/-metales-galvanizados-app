package com.example.metales_galvanizados_app.network
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
object RetrofitClient {
    // RECUERDA USAR TU URL DE NGROK
    private const val BASE_URL = "https://sunshineless-impressionistically-beaulah.ngrok-free.dev/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}