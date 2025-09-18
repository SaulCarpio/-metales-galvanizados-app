package com.example.metales_galvanizados_app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.metales_galvanizados_app.databinding.ActivityLoginBinding
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Configurar autenticación biométrica
        setupBiometricAuthentication()

        // Verificar si debe mostrar autenticación biométrica automáticamente
        checkAndShowBiometricAuth()

        binding.btnLogin.setOnClickListener {
            val user = binding.etUser.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            authenticateUser(user, pass)
        }

        // Opcional: Permitir login con Enter
        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            binding.btnLogin.performClick()
            true
        }
    }

    private fun setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                    // No hacer nada más, el usuario puede usar login manual
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Obtener credenciales guardadas
                    val savedUser = sharedPreferences.getString("saved_user", "")
                    val savedPass = sharedPreferences.getString("saved_pass", "")

                    if (!savedUser.isNullOrEmpty() && !savedPass.isNullOrEmpty()) {
                        authenticateUser(savedUser, savedPass)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación por huella digital")
            .setSubtitle("Use su huella para acceder rápidamente")
            .setNegativeButtonText("Usar contraseña")
            .setConfirmationRequired(false)
            .build()
    }

    private fun checkAndShowBiometricAuth() {
        val hasSavedCredentials = sharedPreferences.getBoolean("has_saved_credentials", false)
        val isBiometricAvailable = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

        // Si tiene credenciales guardadas y biométrica disponible, mostrar automáticamente
        if (hasSavedCredentials && isBiometricAvailable) {
            // Pequeño delay para que se cargue la UI completamente
            binding.root.postDelayed({
                biometricPrompt.authenticate(promptInfo)
            }, 500)
        }
    }

    private fun authenticateUser(user: String, pass: String) {
        when {
            // Usuario administrador - va a MainActivity
            user == "app.megacero" && pass == "qwerty12345" -> {
                val userType = "admin"
                val nextActivity = "main"
                handleSuccessfulLogin(user, pass, userType, nextActivity)
            }
            // Usuario normal - va a MapActivity
            user == "usuario.megacero" && pass == "usuario123" -> {
                val userType = "user"
                val nextActivity = "map"
                handleSuccessfulLogin(user, pass, userType, nextActivity)
            }
            else -> {
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                // Limpiar campos después de error
                binding.etPassword.setText("")
                binding.etUser.requestFocus()
            }
        }
    }

    private fun handleSuccessfulLogin(user: String, pass: String, userType: String, nextActivity: String) {
        val hasSavedCredentials = sharedPreferences.getBoolean("has_saved_credentials", false)
        val isBiometricAvailable = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

        // Si no tiene credenciales guardadas y el dispositivo soporta huella, preguntar
        if (!hasSavedCredentials && isBiometricAvailable) {
            showEnableFingerprintDialog(user, pass, userType, nextActivity)
        } else {
            // Ir directamente a la actividad
            navigateToNextActivity(user, userType, nextActivity)
        }
    }

    private fun showEnableFingerprintDialog(user: String, pass: String, userType: String, nextActivity: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Habilitar huella digital?")
            .setMessage("¿Desea habilitar la autenticación por huella digital para futuros accesos?")
            .setPositiveButton("Sí") { dialog, which ->
                // Guardar credenciales para uso futuro con huella
                sharedPreferences.edit()
                    .putString("saved_user", user)
                    .putString("saved_pass", pass)
                    .putBoolean("has_saved_credentials", true)
                    .apply()

                Toast.makeText(this, "Huella digital habilitada. La próxima vez se activará automáticamente.", Toast.LENGTH_LONG).show()
                navigateToNextActivity(user, userType, nextActivity)
            }
            .setNegativeButton("No") { dialog, which ->
                navigateToNextActivity(user, userType, nextActivity)
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToNextActivity(user: String, userType: String, nextActivity: String) {
        startActivity(Intent(this, SplashActivity::class.java).apply {
            putExtra("next", nextActivity)
            putExtra("user_type", userType)
            putExtra("username", user)
        })
        finish()
    }
}