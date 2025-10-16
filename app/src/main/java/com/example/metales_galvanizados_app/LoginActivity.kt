package com.example.metales_galvanizados_app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.metales_galvanizados_app.databinding.ActivityLoginBinding
import com.example.metales_galvanizados_app.network.LoginRequest
import com.example.metales_galvanizados_app.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.concurrent.Executor


// ===================================================================
// CLASE DE LA ACTIVIDAD
// (NOTA: Todo el código de red ha sido movido a la carpeta 'network')
// ===================================================================
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

        setupBiometricAuthentication()
        checkAndShowBiometricAuth()

        binding.btnLogin.setOnClickListener {
            val user = binding.etUser.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                authenticateUserWithApi(user, pass)
            } else {
                Toast.makeText(this, "Por favor, ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            binding.btnLogin.performClick()
            true
        }
    }

    private fun authenticateUserWithApi(user: String, pass: String) {
        // Asegúrate de tener un ProgressBar con id 'progressBar' en tu activity_login.xml
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                // Ahora usamos las clases importadas desde el paquete 'network'
                val request = LoginRequest(username = user, password = pass)
                val response = RetrofitClient.instance.login(request)

                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true) {
                        val userRole = loginResponse.role
                        val nextActivity = if (userRole == "admin") "main" else "map"

                        Toast.makeText(this@LoginActivity, loginResponse.message, Toast.LENGTH_SHORT).show()
                        handleSuccessfulLogin(user, pass, userRole ?: "user", nextActivity)
                    } else {
                        Toast.makeText(this@LoginActivity, loginResponse?.message ?: "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                        binding.etPassword.setText("")
                    }
                } else {
                    val errorMsg = "Error: ${response.code()} (${response.message()})"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                Toast.makeText(this@LoginActivity, "No se pudo conectar al servidor.", Toast.LENGTH_LONG).show()
                Log.e("LoginActivity", "Error de conexión: ${e.message}")
            }
        }
    }

    private fun setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val savedUser = sharedPreferences.getString("saved_user", "")
                    val savedPass = sharedPreferences.getString("saved_pass", "")
                    if (!savedUser.isNullOrEmpty() && !savedPass.isNullOrEmpty()) {
                        authenticateUserWithApi(savedUser, savedPass)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
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
        if (hasSavedCredentials && isBiometricAvailable) {
            binding.root.postDelayed({ biometricPrompt.authenticate(promptInfo) }, 500)
        }
    }

    private fun handleSuccessfulLogin(user: String, pass: String, userType: String, nextActivity: String) {
        val hasSavedCredentials = sharedPreferences.getBoolean("has_saved_credentials", false)
        val isBiometricAvailable = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (!hasSavedCredentials && isBiometricAvailable) {
            showEnableFingerprintDialog(user, pass, userType, nextActivity)
        } else {
            navigateToNextActivity(user, userType, nextActivity)
        }
    }

    private fun showEnableFingerprintDialog(user: String, pass: String, userType: String, nextActivity: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Habilitar huella digital?")
            .setMessage("¿Desea habilitar la autenticación por huella para futuros accesos?")
            .setPositiveButton("Sí") { _, _ ->
                sharedPreferences.edit()
                    .putString("saved_user", user)
                    .putString("saved_pass", pass)
                    .putBoolean("has_saved_credentials", true)
                    .apply()
                Toast.makeText(this, "Huella digital habilitada.", Toast.LENGTH_LONG).show()
                navigateToNextActivity(user, userType, nextActivity)
            }
            .setNegativeButton("No") { _, _ ->
                navigateToNextActivity(user, userType, nextActivity)
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToNextActivity(user: String, userType: String, nextActivity: String) {
        val intent = when (nextActivity) {
            "main" -> Intent(this, MainActivity::class.java)
            "map" -> Intent(this, MapActivity::class.java)
            else -> Intent(this, SplashActivity::class.java) // Fallback
        }
        intent.putExtra("user_type", userType)
        intent.putExtra("username", user)
        startActivity(intent)
        finish()
    }
}