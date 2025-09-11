package com.example.metales_galvanizados_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.metales_galvanizados_app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val user = binding.etUser.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            when {
                // Usuario administrador - va a MainActivity
                user == "app.megacero" && pass == "qwerty12345" -> {
                    startActivity(Intent(this, SplashActivity::class.java).apply {
                        putExtra("next", "main")
                        putExtra("user_type", "admin")
                        putExtra("username", user)
                    })
                    finish()
                }

                // Usuario normal - va a MapActivity
                user == "usuario.megacero" && pass == "usuario123" -> {
                    startActivity(Intent(this, SplashActivity::class.java).apply {
                        putExtra("next", "map")
                        putExtra("user_type", "user")
                        putExtra("username", user)
                    })
                    finish()
                }

                else -> {
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    // Limpiar campos después de error
                    binding.etPassword.setText("")
                    binding.etUser.requestFocus()
                }
            }
        }

        // Opcional: Permitir login con Enter
        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            binding.btnLogin.performClick()
            true
        }
    }
}