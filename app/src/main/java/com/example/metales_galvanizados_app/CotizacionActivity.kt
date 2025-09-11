package com.example.metales_galvanizados_app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.metales_galvanizados_app.databinding.ActivityCotizacionBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

class CotizacionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCotizacionBinding
    private var totalCalaminas = 0f
    private var totalCumbreras = 0f

    //region Constantes y Claves
    private val MAX_CALAMINAS_CANTIDAD = 1000
    private val MAX_CALAMINAS_LARGO = 12f
    private val MAX_CUMBRERAS_CANTIDAD = 100
    private val MAX_CUMBRERAS_LARGO = 50f
    private val MAX_DECIMALES = 3 // Máximo 3 decimales

    private val KEY_CLIENTE = "cliente"
    private val KEY_PRODUCTO = "producto"
    private val KEY_COLOR = "color"
    private val KEY_CUMBRERA_TIPO = "cumbrera_tipo"
    private val KEY_CALAMINA_CHECKED = "calamina_checked"
    private val KEY_CUMBRERA_CHECKED = "cumbrera_checked"
    private val KEY_CALAMINAS_DATA = "calaminas_data"
    private val KEY_CUMBRERAS_DATA = "cumbreras_data"
    //endregion

    //region Ciclo de Vida
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCotizacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }

        setupSectionToggles()
        setupAddRowButtons()
        setupActionButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }
    //endregion

    //region Configuración Inicial
    private fun setupSpinners() {
        // Configuración de spinners para producto, color y tipo de cumbrera
        val productos = listOf("Ondulado", "Trapezoidal", "Teja Colonial", "Teja América")
        binding.spinnerProducto.adapter = createAdapter(productos)

        val colores = listOf("Azul", "Rojo", "Naranja", "Turquesa", "Verde", "Vino Shingle", "Café Shingle", "Rojo Shingle", "Naranja Shingle", "Zincalum")
        binding.spinnerColor.adapter = createAdapter(colores)

        val cumbreras = listOf("Corte 33", "Corte 50")
        binding.spinnerCumbreras.adapter = createAdapter(cumbreras)
    }

    private fun setupSectionToggles() {
        // Mostrar/ocultar secciones según checkboxes
        binding.checkCalamina.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCalaminas.visibility = if (isChecked) View.VISIBLE else View.GONE
            recalcTotalGeneral()
        }

        binding.checkPlanas.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCumbreras.visibility = if (isChecked) View.VISIBLE else View.GONE
            recalcTotalGeneral()
        }
    }

    private fun setupAddRowButtons() {
        // Configurar listeners para agregar filas
        binding.btnAgregarCalamina.setOnClickListener { addCalaminaRow("", "") }
        binding.btnAgregarCumbrera.setOnClickListener { addCumbreraRow("", "") }
    }

    private fun setupActionButtons() {
        // Configurar botones de limpiar y copiar
        binding.btnLimpiar.setOnClickListener { clearAllData() }
        binding.btnCopiar.setOnClickListener { copySummaryToClipboard() }
    }
    //endregion

    //region Gestión de Filas - Calaminas
    private fun addCalaminaRow(cantidad: String, longitud: String) {
        val row = TableRow(this)

        // Número de fila
        val tvNumero = TextView(this).apply {
            text = ""
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
        }
        row.addView(tvNumero)

        // Campo cantidad
        val etCant = createCantidadEditText(cantidad, MAX_CALAMINAS_CANTIDAD, "Calamina")
        row.addView(etCant)

        // Campo longitud con validación mejorada
        val etLong = createLongitudEditText(longitud, MAX_CALAMINAS_LARGO, "Calamina")
        row.addView(etLong)

        // Total de la fila
        val tvTotal = TextView(this).apply {
            text = calculateInitialTotal(cantidad, longitud)
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
        }
        row.addView(tvTotal)

        // Botón eliminar
        val btnDelete = Button(this).apply {
            text = "✕"
            setPadding(4, 4, 4, 4)
        }
        btnDelete.setOnClickListener {
            binding.tableDetalleCalaminas.removeView(row)
            actualizarNumeros(binding.tableDetalleCalaminas)
            recalcTotalCalaminas()
        }
        row.addView(btnDelete)

        binding.tableDetalleCalaminas.addView(row)
        actualizarNumeros(binding.tableDetalleCalaminas)

        // Watchers para cálculos automáticos
        addTextWatchers(etCant, etLong, ::recalcTotalCalaminas, MAX_CALAMINAS_CANTIDAD, "Calamina")
    }
    //endregion

    //region Gestión de Filas - Cumbreras
    private fun addCumbreraRow(cantidad: String, longitud: String) {
        val row = TableRow(this)

        // Número de fila
        val tvNumero = TextView(this).apply {
            text = ""
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
        }
        row.addView(tvNumero)

        // Campo cantidad
        val etCant = createCantidadEditText(cantidad, MAX_CUMBRERAS_CANTIDAD, "Cumbrera")
        row.addView(etCant)

        // Campo longitud con validación mejorada
        val etLong = createLongitudEditText(longitud, MAX_CUMBRERAS_LARGO, "Cumbrera")
        row.addView(etLong)

        // Total de la fila
        val tvTotal = TextView(this).apply {
            text = calculateInitialTotal(cantidad, longitud)
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
        }
        row.addView(tvTotal)

        // Botón eliminar
        val btnDelete = Button(this).apply {
            text = "✕"
            setPadding(4, 4, 4, 4)
        }
        btnDelete.setOnClickListener {
            binding.tableDetalleCumbreras.removeView(row)
            actualizarNumeros(binding.tableDetalleCumbreras)
            recalcTotalCumbreras()
        }
        row.addView(btnDelete)

        binding.tableDetalleCumbreras.addView(row)
        actualizarNumeros(binding.tableDetalleCumbreras)

        // Watchers para cálculos automáticos
        addTextWatchers(etCant, etLong, ::recalcTotalCumbreras, MAX_CUMBRERAS_CANTIDAD, "Cumbrera")
    }
    //endregion

    //region Utilidades de UI
    private fun createAdapter(items: List<String>): ArrayAdapter<String> {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    private fun createCantidadEditText(text: String, maxValue: Int, tipo: String): EditText {
        return EditText(this).apply {
            hint = "Cant."
            setText(text)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.BLACK)
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(8, 8, 8, 8)
            minWidth = 120
        }
    }

    private fun createLongitudEditText(text: String, maxValue: Float, tipo: String): EditText {
        return EditText(this).apply {
            hint = "Long."
            setText(text)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.BLACK)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(8, 8, 8, 8)
            minWidth = 120

            // Validador mejorado para decimales
            addTextChangedListener(createAdvancedLengthValidator(this, maxValue, tipo))
        }
    }

    private fun calculateInitialTotal(cantidad: String, longitud: String): String {
        return if (cantidad.isNotEmpty() && longitud.isNotEmpty()) {
            try {
                val cant = cantidad.toFloat()
                val long = longitud.toFloat()
                "%.3f".format(cant * long)
            } catch (e: NumberFormatException) {
                "0.000"
            }
        } else {
            "0.000"
        }
    }
    //endregion

    //region Validaciones Mejoradas
    private fun createAdvancedLengthValidator(editText: EditText, maxValue: Float, tipo: String): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val text = s?.toString() ?: ""
                if (text.isNotEmpty() && text != ".") {
                    try {
                        // Validar formato decimal
                        val validatedText = validateDecimalFormat(text, maxValue)

                        if (validatedText != text) {
                            isUpdating = true
                            editText.setText(validatedText)
                            editText.setSelection(validatedText.length)
                            isUpdating = false

                            if (validatedText.toFloat() >= maxValue) {
                                showToast("Longitud máxima $tipo: $maxValue mts")
                            }
                        }
                    } catch (e: NumberFormatException) {
                        // Si no es un número válido, limpiar
                        if (text != "." && text.isNotEmpty()) {
                            isUpdating = true
                            editText.setText("")
                            isUpdating = false
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun validateDecimalFormat(input: String, maxValue: Float): String {
        var text = input

        // Evitar múltiples puntos decimales
        val dotCount = text.count { it == '.' }
        if (dotCount > 1) {
            val firstDotIndex = text.indexOf('.')
            text = text.substring(0, firstDotIndex + 1) + text.substring(firstDotIndex + 1).replace(".", "")
        }

        // Validar máximo de decimales
        if (text.contains('.')) {
            val parts = text.split('.')
            if (parts.size == 2 && parts[1].length > MAX_DECIMALES) {
                text = parts[0] + '.' + parts[1].substring(0, MAX_DECIMALES)
            }
        }

        // Validar valor máximo
        try {
            val value = text.toFloat()
            if (value > maxValue) {
                text = maxValue.toString()
            }
        } catch (e: NumberFormatException) {
            // Si no se puede convertir, mantener el texto original si es válido parcialmente
        }

        return text
    }

    private fun addTextWatchers(etCant: EditText, etLong: EditText, recalcFunction: () -> Unit, maxCantidad: Int, tipo: String) {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateCantidad(etCant, maxCantidad, tipo)
                updateRowTotal(etCant, etLong)
                recalcFunction()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etCant.addTextChangedListener(watcher)
        etLong.addTextChangedListener(watcher)
    }

    private fun updateRowTotal(etCant: EditText, etLong: EditText) {
        try {
            val row = etCant.parent as TableRow
            val tvTotal = row.getChildAt(3) as TextView

            val cantText = etCant.text.toString()
            val longText = etLong.text.toString()

            if (cantText.isNotEmpty() && longText.isNotEmpty()) {
                val cant = cantText.toFloatOrNull() ?: 0f
                val long = longText.toFloatOrNull() ?: 0f
                val total = cant * long
                tvTotal.text = "%.3f".format(total)
            } else {
                tvTotal.text = "0.000"
            }
        } catch (e: Exception) {
            Log.e("CotizacionActivity", "Error updating row total", e)
        }
    }

    private fun validateCantidad(editText: EditText, maxValue: Int, tipo: String) {
        val text = editText.text?.toString() ?: ""
        if (text.isNotEmpty()) {
            try {
                val value = text.toInt()
                if (value > maxValue) {
                    editText.setText(maxValue.toString())
                    editText.setSelection(editText.text.length)
                    showToast("Cantidad máxima $tipo: $maxValue")
                }
            } catch (e: NumberFormatException) {
                // Si no es un número válido, limpiar
                editText.setText("")
            }
        }
    }
    //endregion

    //region Cálculos y Totalizadores
    private fun recalcTotalCalaminas() {
        totalCalaminas = calculateTotal(binding.tableDetalleCalaminas, MAX_CALAMINAS_CANTIDAD, MAX_CALAMINAS_LARGO)
        binding.tvTotalCalaminas.text = "TOTAL MTS.: %.3f".format(totalCalaminas)
        recalcTotalGeneral()
    }

    private fun recalcTotalCumbreras() {
        totalCumbreras = calculateTotal(binding.tableDetalleCumbreras, MAX_CUMBRERAS_CANTIDAD, MAX_CUMBRERAS_LARGO)
        binding.tvTotalCumbreras.text = "TOTAL MTS.: %.3f".format(totalCumbreras)
        recalcTotalGeneral()
    }

    private fun recalcTotalGeneral() {
        val totalGeneral = totalCalaminas + totalCumbreras
        binding.tvTotalGeneral.text = "TOTAL GENERAL: %.3f".format(totalGeneral)
    }

    private fun calculateTotal(table: TableLayout, maxCantidad: Int, maxLongitud: Float): Float {
        var total = 0f
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            val etCant = row.getChildAt(1) as? EditText
            val etLong = row.getChildAt(2) as? EditText

            val cantVal = etCant?.text.toString().toFloatOrNull() ?: 0f
            val longVal = etLong?.text.toString().toFloatOrNull() ?: 0f

            // Aplicar límites
            val cantLimited = cantVal.coerceAtMost(maxCantidad.toFloat())
            val longLimited = longVal.coerceAtMost(maxLongitud)

            val subtotal = cantLimited * longLimited
            (row.getChildAt(3) as? TextView)?.text = "%.3f".format(subtotal)
            total += subtotal
        }
        return total
    }
    //endregion

    //region Utilidades Generales
    private fun actualizarNumeros(table: TableLayout) {
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            (row.getChildAt(0) as TextView).text = (i + 1).toString()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    //endregion

    //region Acciones Principales
    private fun clearAllData() {
        // Limpiar todos los campos y tablas
        binding.tableDetalleCalaminas.removeAllViews()
        binding.tableDetalleCumbreras.removeAllViews()
        binding.etCliente.setText("")
        binding.spinnerProducto.setSelection(0)
        binding.spinnerColor.setSelection(0)
        binding.spinnerCumbreras.setSelection(0)
        binding.checkCalamina.isChecked = false
        binding.checkPlanas.isChecked = false
        totalCalaminas = 0f
        totalCumbreras = 0f
        binding.tvTotalCalaminas.text = "TOTAL MTS.: 0.000"
        binding.tvTotalCumbreras.text = "TOTAL MTS.: 0.000"
        binding.tvTotalGeneral.text = "TOTAL GENERAL: 0.000"
    }

    private fun copySummaryToClipboard() {
        // Generar y copiar resumen al portapapeles
        val resumen = buildString {
            append("COTIZADOR - MEGACERO S.R.L.\n")
            append("Cliente: ${binding.etCliente.text}\n")
            append("Producto: ${binding.spinnerProducto.selectedItem}\n")
            append("Color: ${binding.spinnerColor.selectedItem}\n\n")

            if (binding.checkCalamina.isChecked) {
                append("=== CALAMINAS ===\n")
                appendTableDetails(binding.tableDetalleCalaminas)
                append("Total Calaminas: %.3f mts\n\n".format(totalCalaminas))
            }

            if (binding.checkPlanas.isChecked) {
                append("=== CUMBRERAS ===\n")
                appendTableDetails(binding.tableDetalleCumbreras)
                append("Total Cumbreras: %.3f mts\n\n".format(totalCumbreras))
            }

            append("TOTAL GENERAL: %.3f mts\n".format(totalCalaminas + totalCumbreras))
        }

        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
            ClipData.newPlainText("cotizacion", resumen)
        )
        showToast("Cotización copiada al portapapeles")
    }

    private fun StringBuilder.appendTableDetails(table: TableLayout) {
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            val etCant = row.getChildAt(1) as EditText
            val etLong = row.getChildAt(2) as EditText
            val tvTotal = row.getChildAt(3) as TextView

            append("${i + 1}. Cant: ${etCant.text}, Long: ${etLong.text}, Total: ${tvTotal.text}\n")
        }
    }
    //endregion

    //region Persistencia de Estado Mejorada
    private fun saveState(outState: Bundle) {
        try {
            // Guardar estado actual en bundle
            outState.putString(KEY_CLIENTE, binding.etCliente.text.toString())
            outState.putInt(KEY_PRODUCTO, binding.spinnerProducto.selectedItemPosition)
            outState.putInt(KEY_COLOR, binding.spinnerColor.selectedItemPosition)
            outState.putInt(KEY_CUMBRERA_TIPO, binding.spinnerCumbreras.selectedItemPosition)
            outState.putBoolean(KEY_CALAMINA_CHECKED, binding.checkCalamina.isChecked)
            outState.putBoolean(KEY_CUMBRERA_CHECKED, binding.checkPlanas.isChecked)

            outState.putString(KEY_CALAMINAS_DATA, serializeTableData(binding.tableDetalleCalaminas))
            outState.putString(KEY_CUMBRERAS_DATA, serializeTableData(binding.tableDetalleCumbreras))

            Log.d("CotizacionActivity", "Estado guardado correctamente")
        } catch (e: Exception) {
            Log.e("CotizacionActivity", "Error guardando estado", e)
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        try {
            // Restaurar estado desde bundle
            binding.etCliente.setText(savedInstanceState.getString(KEY_CLIENTE, ""))
            binding.spinnerProducto.setSelection(savedInstanceState.getInt(KEY_PRODUCTO, 0))
            binding.spinnerColor.setSelection(savedInstanceState.getInt(KEY_COLOR, 0))
            binding.spinnerCumbreras.setSelection(savedInstanceState.getInt(KEY_CUMBRERA_TIPO, 0))

            // Restaurar checkboxes
            val calaminaChecked = savedInstanceState.getBoolean(KEY_CALAMINA_CHECKED, false)
            val cumbreraChecked = savedInstanceState.getBoolean(KEY_CUMBRERA_CHECKED, false)

            binding.checkCalamina.isChecked = calaminaChecked
            binding.checkPlanas.isChecked = cumbreraChecked

            // IMPORTANTE: Forzar la visibilidad de las secciones según el estado de los checkboxes
            binding.layoutCalaminas.visibility = if (calaminaChecked) View.VISIBLE else View.GONE
            binding.layoutCumbreras.visibility = if (cumbreraChecked) View.VISIBLE else View.GONE

            // Restaurar datos de las tablas DESPUÉS de configurar los toggles
            val calaminasData = savedInstanceState.getString(KEY_CALAMINAS_DATA)
            val cumbrerasData = savedInstanceState.getString(KEY_CUMBRERAS_DATA)

            // Usar post para asegurar que la UI esté completamente inicializada
            binding.root.post {
                restoreTableData(calaminasData, ::addCalaminaRow)
                restoreTableData(cumbrerasData, ::addCumbreraRow)

                // Recalcular totales después de restaurar
                recalcTotalCalaminas()
                recalcTotalCumbreras()
            }

            Log.d("CotizacionActivity", "Estado restaurado correctamente")
        } catch (e: Exception) {
            Log.e("CotizacionActivity", "Error restaurando estado", e)
        }
    }

    private fun serializeTableData(table: TableLayout): String {
        val jsonArray = JSONArray()
        try {
            for (i in 0 until table.childCount) {
                val row = table.getChildAt(i) as TableRow
                val etCant = row.getChildAt(1) as EditText
                val etLong = row.getChildAt(2) as EditText

                val jsonObject = JSONObject().apply {
                    put("cantidad", etCant.text.toString())
                    put("longitud", etLong.text.toString())
                }
                jsonArray.put(jsonObject)
            }
        } catch (e: Exception) {
            Log.e("CotizacionActivity", "Error serializando tabla", e)
        }
        return jsonArray.toString()
    }

    private fun restoreTableData(jsonData: String?, addRowFunction: (String, String) -> Unit) {
        if (!jsonData.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(jsonData)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val cantidad = item.getString("cantidad")
                    val longitud = item.getString("longitud")
                    addRowFunction(cantidad, longitud)
                }
                Log.d("CotizacionActivity", "Tabla restaurada: ${jsonArray.length()} filas")
            } catch (e: Exception) {
                Log.e("CotizacionActivity", "Error restaurando datos de tabla", e)
            }
        }
    }
    //endregion
}