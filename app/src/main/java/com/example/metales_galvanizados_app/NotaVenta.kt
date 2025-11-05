package com.example.metales_galvanizados_app

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NotaVenta : AppCompatActivity(), NotaVentaAdapter.OnItemClickListener {

    // Vistas
    private lateinit var edtNroProforma: EditText
    private lateinit var edtCliente: EditText
    private lateinit var edtVendedor: EditText
    private lateinit var edtFecha: EditText
    private lateinit var spinnerProducto: Spinner // <-- CAMBIO: Ahora es un Spinner
    private lateinit var edtColor: EditText
    private lateinit var edtCantidad: EditText
    private lateinit var edtLongitud: EditText
    private lateinit var edtPrecioUnitario: EditText
    private lateinit var edtTotal: EditText

    // Lista y Adaptador
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotaVentaAdapter
    private val notasList = mutableListOf<NotaVentaModel>()

    // Para la cámara
    private var fotoUriParaRespaldo: Uri? = null
    private var posicionItemParaFoto: Int = -1

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) lanzarCamaraParaRespaldo()
        else Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_LONG).show()
    }

    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            if (posicionItemParaFoto != -1 && posicionItemParaFoto < notasList.size) {
                notasList[posicionItemParaFoto].fotoRespaldoUri = fotoUriParaRespaldo.toString()
                adapter.notifyItemChanged(posicionItemParaFoto)
                guardarListaEnPrefs()
                Toast.makeText(this, "Respaldo fotográfico guardado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nota_venta)

        inicializarVistas()
        setupSpinners() // <-- NUEVO: Configurar el Spinner
        setupDatePicker() // <-- NUEVO: Configurar el selector de fecha
        cargarListaDesdePrefs()
        setupRecyclerView()
        setupBotones()
    }

    private fun inicializarVistas() {
        edtNroProforma = findViewById(R.id.edtNroProforma)
        edtCliente = findViewById(R.id.edtCliente)
        edtVendedor = findViewById(R.id.edtVendedor)
        edtFecha = findViewById(R.id.edtFecha)
        spinnerProducto = findViewById(R.id.spinnerProducto) // <-- CAMBIO: Referencia al Spinner
        edtColor = findViewById(R.id.edtColor)
        edtCantidad = findViewById(R.id.edtCantidad)
        edtLongitud = findViewById(R.id.edtLongitud)
        edtPrecioUnitario = findViewById(R.id.edtPrecioUnitario)
        edtTotal = findViewById(R.id.edtTotal)
        recyclerView = findViewById(R.id.recyclerViewNotas)

        // <-- VALIDACIÓN: Nro Proforma solo acepta números
        edtNroProforma.inputType = InputType.TYPE_CLASS_NUMBER
    }

    private fun setupSpinners() {
        val productos = listOf("CALAMINA ONDULADA", "CALAMINA TRAPEZOIDAL", "TEJA COLONIAL", "TEJA AMERICANA", "CUMBRERA CORTE 33", "CUMBRERA CORTE 50")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productos).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerProducto.adapter = spinnerAdapter
    }

    private fun setupDatePicker() {
        edtFecha.isFocusable = false // Evita que el teclado aparezca
        edtFecha.isClickable = true
        edtFecha.setOnClickListener {
            val calendar = Calendar.getInstance()

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    edtFecha.setText(format.format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Establecer límites de fecha: 5 años atrás y 5 años adelante
            val minDate = Calendar.getInstance().apply { add(Calendar.YEAR, -5) }
            val maxDate = Calendar.getInstance().apply { add(Calendar.YEAR, 5) }

            datePickerDialog.datePicker.minDate = minDate.timeInMillis
            datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

            datePickerDialog.show()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotaVentaAdapter(notasList, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupBotones() {
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarNotaVenta() }
        findViewById<Button>(R.id.btnLimpiar).setOnClickListener { limpiarFormulario() }
    }

    private fun guardarNotaVenta() {
        // --- VALIDACIONES MEJORADAS ---
        if (edtNroProforma.text.isBlank()) {
            edtNroProforma.error = "Este campo es obligatorio"
            return
        }
        if (edtCliente.text.isBlank()) {
            edtCliente.error = "Este campo es obligatorio"
            return
        }
        if (spinnerProducto.selectedItemPosition == 0) {
            Toast.makeText(this, "Debes seleccionar un producto", Toast.LENGTH_SHORT).show()
            return
        }
        // --- FIN DE VALIDACIONES ---

        val nuevaNota = NotaVentaModel(
            nroProforma = edtNroProforma.text.toString(),
            cliente = edtCliente.text.toString(),
            vendedor = edtVendedor.text.toString(),
            fecha = edtFecha.text.toString(),
            producto = spinnerProducto.selectedItem.toString(), // <-- CAMBIO: Obtener del Spinner
            color = edtColor.text.toString(),
            cantidad = edtCantidad.text.toString(),
            longitud = edtLongitud.text.toString(),
            precioUnitario = edtPrecioUnitario.text.toString(),
            total = edtTotal.text.toString()
        )

        notasList.add(nuevaNota)
        adapter.notifyItemInserted(notasList.size - 1)
        limpiarFormulario()
        recyclerView.smoothScrollToPosition(notasList.size - 1)
        guardarListaEnPrefs()
        Toast.makeText(this, "Nota de venta guardada", Toast.LENGTH_SHORT).show()
    }

    private fun limpiarFormulario() {
        val campos = listOf(edtNroProforma, edtCliente, edtVendedor, edtFecha, edtColor, edtCantidad, edtLongitud, edtPrecioUnitario, edtTotal)
        campos.forEach { it.text.clear() }
        spinnerProducto.setSelection(0)
        edtNroProforma.requestFocus()
    }

    // --- Lógica para la Cámara y Persistencia (Sin cambios) ---
    override fun onRespaldoButtonClick(position: Int) {
        if (position >= 0 && position < notasList.size) {
            val notaSeleccionada = notasList[position]
            if (!notaSeleccionada.fotoRespaldoUri.isNullOrEmpty()) {
                verFotoDeRespaldo(notaSeleccionada.fotoRespaldoUri!!.toUri())
            } else {
                posicionItemParaFoto = position
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun verFotoDeRespaldo(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/jpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se encontró una aplicación para ver imágenes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarListaEnPrefs() {
        val prefs = getSharedPreferences("notas_prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(notasList)
        editor.putString("lista_notas", json)
        editor.apply()
    }

    private fun cargarListaDesdePrefs() {
        try {
            val prefs = getSharedPreferences("notas_prefs", MODE_PRIVATE)
            val json = prefs.getString("lista_notas", null)
            if (json != null) {
                val gson = Gson()
                val type = object : TypeToken<MutableList<NotaVentaModel>>() {}.type
                val listaGuardada: MutableList<NotaVentaModel> = gson.fromJson(json, type)
                notasList.clear()
                notasList.addAll(listaGuardada)
            }
        } catch (e: Exception) {
            Log.e("NotaVenta", "Error al cargar datos", e)
            notasList.clear()
            getSharedPreferences("notas_prefs", MODE_PRIVATE).edit().clear().apply()
        }
    }

    private fun lanzarCamaraParaRespaldo() {
        try {
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                Toast.makeText(this, "Almacenamiento no disponible.", Toast.LENGTH_LONG).show()
                return
            }
            val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir == null) {
                Toast.makeText(this, "No se pudo acceder al almacenamiento.", Toast.LENGTH_LONG).show()
                return
            }
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val archivoFoto = File.createTempFile("respaldo_${System.currentTimeMillis()}", ".jpg", storageDir)
            val authority = "$packageName.provider"
            fotoUriParaRespaldo = FileProvider.getUriForFile(this, authority, archivoFoto)
            tomarFotoLauncher.launch(fotoUriParaRespaldo)
        } catch (e: Exception) {
            Log.e("NotaVenta", "Error al lanzar cámara", e)
            Toast.makeText(this, "Error inesperado al iniciar la cámara.", Toast.LENGTH_LONG).show()
        }
    }
}