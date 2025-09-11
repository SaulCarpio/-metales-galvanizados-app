package com.example.metales_galvanizados_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView

class SplashActivity : AppCompatActivity() {

    // Declarar el Handler y Runnable como variables de clase para poder detenerlos
    private lateinit var dotsHandler: Handler
    private lateinit var dotsRunnable: Runnable
    private var dots = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Obtener referencias a los elementos de la vista
        val logo = findViewById<ImageView>(R.id.splash_logo)
        val title = findViewById<TextView>(R.id.splash_title)
        val loadingDots = findViewById<TextView>(R.id.loading_dots)

        // Cargar animaciones
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)

        // Aplicar animaciones con retrasos escalonados
        logo.startAnimation(fadeIn)

        Handler(Looper.getMainLooper()).postDelayed({
            title.startAnimation(slideUp)
        }, 500)

        Handler(Looper.getMainLooper()).postDelayed({
            logo.startAnimation(rotate)
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            logo.startAnimation(bounce)
        }, 1500)

        // ANIMACIÓN DE PUNTOS DE CARGA
        dotsHandler = Handler(Looper.getMainLooper())
        dotsRunnable = object : Runnable {
            override fun run() {
                dots = if (dots.length < 3) dots + "." else ""
                loadingDots.text = "Cargando$dots"
                dotsHandler.postDelayed(this, 500)
            }
        }
        dotsHandler.postDelayed(dotsRunnable, 500)

        // Obtener datos del intent
        val next = intent.getStringExtra("next")
        val userType = intent.getStringExtra("user_type")
        val username = intent.getStringExtra("username")

        // Esperar 3 segundos y luego redirigir según el tipo de usuario
        Handler(Looper.getMainLooper()).postDelayed({
            when (next) {
                "main" -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("user_type", userType)
                        putExtra("username", username)
                    })
                }
                "map" -> {
                    startActivity(Intent(this, MapActivity::class.java).apply {
                        putExtra("user_type", userType)
                        putExtra("username", username)
                    })
                }
                else -> {
                    // Fallback al login si no hay redirección específica
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3000)
    }

    // No olvides detener la animación al salir de la actividad
    override fun onDestroy() {
        super.onDestroy()
        if (::dotsHandler.isInitialized) {
            dotsHandler.removeCallbacks(dotsRunnable)
        }
    }
}