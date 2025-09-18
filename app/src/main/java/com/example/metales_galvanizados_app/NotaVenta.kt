package com.example.metales_galvanizados_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

class NotaVenta : AppCompatActivity() {

    private var previewView: androidx.camera.view.PreviewView? = null
    private var btnCapture: Button? = null
    private var txtResult: TextView? = null
    private var formLayout: LinearLayout? = null

    // Campos principales
    private var edtNroProforma: EditText? = null
    private var edtCliente: EditText? = null
    private var edtCel: EditText? = null
    private var edtVendedor: EditText? = null
    private var edtFecha: EditText? = null

    // Campos de producto
    private var edtProducto: EditText? = null
    private var edtColor: EditText? = null
    private var edtCantidad: EditText? = null
    private var edtLongitud: EditText? = null
    private var edtPrecioUnitario: EditText? = null
    private var edtImporte: EditText? = null

    // Campos de totales
    private var edtSubtotal: EditText? = null
    private var edtAnticipo: EditText? = null
    private var edtSaldo: EditText? = null
    private var edtTotal: EditText? = null

    // Campos adicionales
    private var edtFechaEntrega: EditText? = null
    private var edtNombreCliente: EditText? = null
    private var edtNit: EditText? = null
    private var edtFirmaCaja: EditText? = null
    private var edtFirmaCliente: EditText? = null

    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara para usar esta función", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_nota_venta)

            if (initializeViews()) {
                setupCameraExecutor()
                checkCameraPermission()
            } else {
                Toast.makeText(this, "Error al cargar la interfaz", Toast.LENGTH_LONG).show()
                finish()
            }

        } catch (e: Exception) {
            Log.e("NotaVenta", "Error crítico en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al inicializar la aplicación", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews(): Boolean {
        return try {
            previewView = findViewById(R.id.previewView)
            btnCapture = findViewById(R.id.btnCapture)
            txtResult = findViewById(R.id.txtResult)
            formLayout = findViewById(R.id.formLayout)

            // Campos principales
            edtNroProforma = findViewById(R.id.edtNroProforma)
            edtCliente = findViewById(R.id.edtCliente)
            edtCel = findViewById(R.id.edtCel)
            edtVendedor = findViewById(R.id.edtVendedor)
            edtFecha = findViewById(R.id.edtFecha)

            // Campos de producto
            edtProducto = findViewById(R.id.edtProducto)
            edtColor = findViewById(R.id.edtColor)
            edtCantidad = findViewById(R.id.edtCantidad)
            edtLongitud = findViewById(R.id.edtLongitud)
            edtPrecioUnitario = findViewById(R.id.edtPrecioUnitario)
            edtImporte = findViewById(R.id.edtImporte)

            // Campos de totales
            edtSubtotal = findViewById(R.id.edtSubtotal)
            edtAnticipo = findViewById(R.id.edtAnticipo)
            edtSaldo = findViewById(R.id.edtSaldo)
            edtTotal = findViewById(R.id.edtTotal)

            // Campos adicionales
            edtFechaEntrega = findViewById(R.id.edtFechaEntrega)
            edtNombreCliente = findViewById(R.id.edtNombreCliente)
            edtNit = findViewById(R.id.edtNit)
            edtFirmaCaja = findViewById(R.id.edtFirmaCaja)
            edtFirmaCliente = findViewById(R.id.edtFirmaCliente)

            if (previewView == null || btnCapture == null) {
                Log.e("NotaVenta", "Vistas esenciales no encontradas")
                return false
            }

            btnCapture?.setOnClickListener {
                capturePhotoToFile()
            }

            // Botón guardar
            val btnGuardar: Button? = findViewById(R.id.btnGuardar)
            btnGuardar?.setOnClickListener {
                guardarNotaVenta()
            }

            // Botón limpiar
            val btnLimpiar: Button? = findViewById(R.id.btnLimpiar)
            btnLimpiar?.setOnClickListener {
                limpiarFormulario()
            }

            // Auto-calcular importe cuando cambian cantidad o precio
            edtCantidad?.setOnFocusChangeListener { _, _ -> calcularImporte() }
            edtPrecioUnitario?.setOnFocusChangeListener { _, _ -> calcularImporte() }

            txtResult?.text = "Presiona 'Escanear' para capturar la proforma"
            formLayout?.visibility = LinearLayout.VISIBLE

            true
        } catch (e: Exception) {
            Log.e("NotaVenta", "Error al inicializar vistas: ${e.message}", e)
            false
        }
    }

    private fun setupCameraExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Log.e("NotaVenta", "Error al inicializar cámara", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        val previewView = previewView ?: return

        try {
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)

            runOnUiThread {
                btnCapture?.isEnabled = true
                txtResult?.text = "Cámara lista. Apunta a la proforma y presiona 'Escanear'"
            }

        } catch (exc: Exception) {
            Log.e("NotaVenta", "Error al configurar cámara", exc)
        }
    }

    private fun capturePhotoToFile() {
        val imageCapture = imageCapture ?: return
        val btnCapture = btnCapture ?: return

        val photoFile = File(externalCacheDir, "temp_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        btnCapture.isEnabled = false
        btnCapture.text = "Escaneando..."

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        val image = InputImage.fromFilePath(this@NotaVenta, android.net.Uri.fromFile(photoFile))
                        runOCR(image)
                        photoFile.delete()
                    } catch (e: Exception) {
                        Log.e("NotaVenta", "Error al procesar imagen", e)
                        resetCaptureButton()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("NotaVenta", "Error al capturar", exception)
                    resetCaptureButton()
                    photoFile.delete()
                }
            }
        )
    }

    private fun runOCR(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                runOnUiThread {
                    if (visionText.text.isNotEmpty()) {
                        txtResult?.text = "Texto detectado y procesado"
                        parseFormData(visionText.text)
                        Toast.makeText(this@NotaVenta, "Datos extraídos. Revisa y completa los campos", Toast.LENGTH_LONG).show()
                    } else {
                        txtResult?.text = "No se detectó texto. Completa manualmente"
                    }
                    resetCaptureButton()
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    txtResult?.text = "Error OCR: ${e.message}"
                    resetCaptureButton()
                }
            }
    }

    private fun parseFormData(text: String) {
        try {
            Log.d("NotaVenta", "Parseando: $text")

            // Número de proforma
            val numeroPattern = Pattern.compile("N[oº°]\\s*(\\d+)", Pattern.CASE_INSENSITIVE)
            numeroPattern.matcher(text).takeIf { it.find() }?.let {
                edtNroProforma?.setText(it.group(1))
            }

            // Cliente
            val clientePattern = Pattern.compile("CLIENTE:\\s*([^\\n\\r]+)", Pattern.CASE_INSENSITIVE)
            clientePattern.matcher(text).takeIf { it.find() }?.let {
                val cliente = it.group(1)?.trim()
                edtCliente?.setText(cliente)
                edtNombreCliente?.setText(cliente) // Mismo valor para ambos campos
            }

            // Celular/Teléfono
            val celPattern = Pattern.compile("(?:CEL|CELULAR|TLF|TELEFONO)[:\\s]*([0-9\\s-]+)", Pattern.CASE_INSENSITIVE)
            celPattern.matcher(text).takeIf { it.find() }?.let {
                edtCel?.setText(it.group(1)?.trim()?.replace("\\s".toRegex(), ""))
            }

            // Vendedor
            val vendedorPattern = Pattern.compile("VENDEDOR[:\\s]*([^\\n\\r]+)", Pattern.CASE_INSENSITIVE)
            vendedorPattern.matcher(text).takeIf { it.find() }?.let {
                edtVendedor?.setText(it.group(1)?.trim())
            }

            // Fecha
            val fechaPattern = Pattern.compile("FECHA:\\s*([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})", Pattern.CASE_INSENSITIVE)
            fechaPattern.matcher(text).takeIf { it.find() }?.let {
                edtFecha?.setText(it.group(1))
            }

            // Productos detectar tipos comunes
            val productos = listOf("ONDULADO", "TEJA COLONIAL", "TRAPEZOIDAL", "TEJA AMERICANA", "GALVANIZADA", "ZINCALUM")
            for (producto in productos) {
                if (text.contains(producto, ignoreCase = true)) {
                    edtProducto?.setText(producto)
                    break
                }
            }

            // Colores comunes
            val colores = listOf("ROJO", "VERDE", "AZUL", "BLANCO", "GRIS", "NEGRO", "MARRON", "BEIGE")
            for (color in colores) {
                if (text.contains(color, ignoreCase = true)) {
                    edtColor?.setText(color)
                    break
                }
            }

            // Cantidad (buscar números seguidos de unidades)
            val cantidadPattern = Pattern.compile("(?:CANT|CANTIDAD)[:\\s]*([0-9]+)", Pattern.CASE_INSENSITIVE)
            cantidadPattern.matcher(text).takeIf { it.find() }?.let {
                edtCantidad?.setText(it.group(1))
            }

            // Longitud (buscar números con metros)
            val longitudPattern = Pattern.compile("([0-9]+[.,]?[0-9]*)\\s*[mM]", Pattern.CASE_INSENSITIVE)
            longitudPattern.matcher(text).takeIf { it.find() }?.let {
                edtLongitud?.setText(it.group(1)?.replace(",", "."))
            }

            // Precios y totales
            val precioPattern = Pattern.compile("P[.]?U[.]?[:\\s]*([0-9,]+[.]?[0-9]*)", Pattern.CASE_INSENSITIVE)
            precioPattern.matcher(text).takeIf { it.find() }?.let {
                edtPrecioUnitario?.setText(it.group(1)?.replace(",", ""))
            }

            val importePattern = Pattern.compile("IMPORTE[\\s]*BS[.\\s]*([0-9,]+)", Pattern.CASE_INSENSITIVE)
            importePattern.matcher(text).takeIf { it.find() }?.let {
                edtImporte?.setText(it.group(1)?.replace(",", ""))
            }

            val subtotalPattern = Pattern.compile("SUB[\\s-]*TOTAL[\\s]*BS[.\\s-]*([0-9,]+)", Pattern.CASE_INSENSITIVE)
            subtotalPattern.matcher(text).takeIf { it.find() }?.let {
                edtSubtotal?.setText(it.group(1)?.replace(",", ""))
            }

            val totalPattern = Pattern.compile("TOTAL[\\s]*BS[.\\s-]*([0-9,]+)", Pattern.CASE_INSENSITIVE)
            totalPattern.matcher(text).takeIf { it.find() }?.let {
                edtTotal?.setText(it.group(1)?.replace(",", ""))
            }

            val anticipoPattern = Pattern.compile("ANTICIPO[\\s]*BS[.\\s-]*([0-9,]+)", Pattern.CASE_INSENSITIVE)
            anticipoPattern.matcher(text).takeIf { it.find() }?.let {
                edtAnticipo?.setText(it.group(1)?.replace(",", ""))
            }

            val saldoPattern = Pattern.compile("SALDO[\\s]*BS[.\\s-]*([0-9,]+)", Pattern.CASE_INSENSITIVE)
            saldoPattern.matcher(text).takeIf { it.find() }?.let {
                edtSaldo?.setText(it.group(1)?.replace(",", ""))
            }

            // NIT
            val nitPattern = Pattern.compile("NIT[:\\s]*([0-9-]+)", Pattern.CASE_INSENSITIVE)
            nitPattern.matcher(text).takeIf { it.find() }?.let {
                edtNit?.setText(it.group(1))
            }

        } catch (e: Exception) {
            Log.e("NotaVenta", "Error al parsear: ${e.message}", e)
        }
    }

    private fun calcularImporte() {
        try {
            val cantidad = edtCantidad?.text.toString().toDoubleOrNull() ?: 0.0
            val precio = edtPrecioUnitario?.text.toString().toDoubleOrNull() ?: 0.0
            val importe = cantidad * precio

            if (importe > 0) {
                edtImporte?.setText(String.format("%.2f", importe))
            }
        } catch (e: Exception) {
            Log.e("NotaVenta", "Error al calcular importe", e)
        }
    }

    private fun guardarNotaVenta() {
        // Aquí implementarás la conexión a base de datos
        val datos = mapOf(
            "nro_proforma" to edtNroProforma?.text.toString(),
            "cliente" to edtCliente?.text.toString(),
            "cel" to edtCel?.text.toString(),
            "vendedor" to edtVendedor?.text.toString(),
            "fecha" to edtFecha?.text.toString(),
            "producto" to edtProducto?.text.toString(),
            "color" to edtColor?.text.toString(),
            "cantidad" to edtCantidad?.text.toString(),
            "longitud" to edtLongitud?.text.toString(),
            "precio_unitario" to edtPrecioUnitario?.text.toString(),
            "importe" to edtImporte?.text.toString(),
            "subtotal" to edtSubtotal?.text.toString(),
            "anticipo" to edtAnticipo?.text.toString(),
            "saldo" to edtSaldo?.text.toString(),
            "total" to edtTotal?.text.toString(),
            "fecha_entrega" to edtFechaEntrega?.text.toString(),
            "nombre_cliente" to edtNombreCliente?.text.toString(),
            "nit" to edtNit?.text.toString(),
            "firma_caja" to edtFirmaCaja?.text.toString(),
            "firma_cliente" to edtFirmaCliente?.text.toString()
        )

        Log.d("NotaVenta", "Datos para guardar: $datos")
        Toast.makeText(this, "Datos preparados para guardar en BD", Toast.LENGTH_LONG).show()
    }

    private fun limpiarFormulario() {
        val campos = listOf(
            edtNroProforma, edtCliente, edtCel, edtVendedor, edtFecha,
            edtProducto, edtColor, edtCantidad, edtLongitud, edtPrecioUnitario,
            edtImporte, edtSubtotal, edtAnticipo, edtSaldo, edtTotal,
            edtFechaEntrega, edtNombreCliente, edtNit, edtFirmaCaja, edtFirmaCliente
        )

        campos.forEach { it?.setText("") }
        Toast.makeText(this, "Formulario limpiado", Toast.LENGTH_SHORT).show()
    }

    private fun resetCaptureButton() {
        btnCapture?.let {
            it.isEnabled = true
            it.text = "Escanear Proforma"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        cameraProvider?.unbindAll()
    }
}