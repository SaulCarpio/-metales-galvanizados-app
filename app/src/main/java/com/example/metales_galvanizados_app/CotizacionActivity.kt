package com.example.metales_galvanizados_app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.metales_galvanizados_app.databinding.ActivityCotizacionBinding

class CotizacionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCotizacionBinding
    private var contadorCalaminas = 1
    private var contadorCumbreras = 1
    private var totalCalaminas = 0f
    private var totalCumbreras = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCotizacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Producto
        val productos = listOf("Ondulado", "Trapezoidal", "Teja Colonial", "Teja América")
        val adapterProducto = ArrayAdapter(this, R.layout.spinner_item, productos)
        adapterProducto.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProducto.adapter = adapterProducto

        // Color
        val colores = listOf(
            "Azul", "Rojo", "Naranja", "Turquesa", "Verde",
            "Vino Shingle", "Café Shingle", "Rojo Shingle",
            "Naranja Shingle", "Zincalum"
        )
        val adapterColor = ArrayAdapter(this, R.layout.spinner_item, colores)
        adapterColor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerColor.adapter = adapterColor

        // Cumbreras
        val cumbreras = listOf("Corte 33", "Corte 50")
        val adapterCumbreras = ArrayAdapter(this, R.layout.spinner_item, cumbreras)
        adapterCumbreras.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCumbreras.adapter = adapterCumbreras

        // Mostrar / ocultar secciones
        binding.checkCalamina.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCalaminas.visibility = if (isChecked) View.VISIBLE else View.GONE
            recalcTotalGeneral()
        }
        binding.checkPlanas.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCumbreras.visibility = if (isChecked) View.VISIBLE else View.GONE
            recalcTotalGeneral()
        }

        // Agregar fila Calamina
        binding.btnAgregarCalamina.setOnClickListener {
            val row = TableRow(this)

            // Número de fila (se actualizará después)
            row.addView(TextView(this).apply {
                text = "" // vacío por ahora
                setTextColor(Color.BLACK)
            })

            val etCant = EditText(this).apply {
                hint = "Cant."
                setTextColor(Color.BLACK)
                setHintTextColor(Color.BLACK)
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            row.addView(etCant)

            val etLong = EditText(this).apply {
                hint = "Long."
                setTextColor(Color.BLACK)
                setHintTextColor(Color.BLACK)
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            row.addView(etLong)

            val tvTotal = TextView(this).apply {
                text = "0.00"
                setTextColor(Color.BLACK)
            }
            row.addView(tvTotal)

            val btnDelete = Button(this).apply { text = "✕" }
            btnDelete.setOnClickListener {
                binding.tableDetalleCalaminas.removeView(row)
                actualizarNumeros(binding.tableDetalleCalaminas)
                recalcTotalCalaminas()
            }
            row.addView(btnDelete)

            binding.tableDetalleCalaminas.addView(row)
            actualizarNumeros(binding.tableDetalleCalaminas)

            val watcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { recalcTotalCalaminas() }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            etCant.addTextChangedListener(watcher)
            etLong.addTextChangedListener(watcher)
        }


        // Agregar fila Cumbrera (ACTUALIZADO con la nueva lógica)
        binding.btnAgregarCumbrera.setOnClickListener {
            val row = TableRow(this)

            // Número de fila (se actualizará después)
            row.addView(TextView(this).apply {
                text = "" // vacío por ahora
                setTextColor(Color.BLACK)
            })

            val etCant = EditText(this).apply {
                hint = "Cant."
                setTextColor(Color.BLACK)
                setHintTextColor(Color.BLACK)
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            row.addView(etCant)

            val etLong = EditText(this).apply {
                hint = "Long."
                setTextColor(Color.BLACK)
                setHintTextColor(Color.BLACK)
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            row.addView(etLong)

            val tvTotal = TextView(this).apply {
                text = "0.00"
                setTextColor(Color.BLACK)
            }
            row.addView(tvTotal)

            val btnDelete = Button(this).apply { text = "✕" }
            btnDelete.setOnClickListener {
                binding.tableDetalleCumbreras.removeView(row)
                actualizarNumeros(binding.tableDetalleCumbreras) // Usar la misma función
                recalcTotalCumbreras()
            }
            row.addView(btnDelete)

            binding.tableDetalleCumbreras.addView(row)
            actualizarNumeros(binding.tableDetalleCumbreras) // Usar la misma función

            val watcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { recalcTotalCumbreras() }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            etCant.addTextChangedListener(watcher)
            etLong.addTextChangedListener(watcher)
        }

        // Limpiar
        binding.btnLimpiar.setOnClickListener {
            binding.tableDetalleCalaminas.removeAllViews()
            binding.tableDetalleCumbreras.removeAllViews()
            contadorCalaminas = 1
            contadorCumbreras = 1
            totalCalaminas = 0f
            totalCumbreras = 0f
            binding.tvTotalCalaminas.text = "TOTAL MTS.: 0.00"
            binding.tvTotalCumbreras.text = "TOTAL MTS.: 0.00"
            binding.tvTotalGeneral.text = "TOTAL GENERAL: 0.00"
        }

        // Copiar resumen
        binding.btnCopiar.setOnClickListener {
            val resumen = StringBuilder()
            resumen.append("COTIZADOR - MEGACERO S.R.L.\n")
            resumen.append("Cliente: ${binding.etCliente.text}\n")
            resumen.append("Producto: ${binding.spinnerProducto.selectedItem}\n")
            resumen.append("Color: ${binding.spinnerColor.selectedItem}\n")

            if (binding.checkCalamina.isChecked) {
                resumen.append("Total Calaminas: %.2f mts\n".format(totalCalaminas))
            }
            if (binding.checkPlanas.isChecked) {
                resumen.append("Total Cumbreras: %.2f mts\n".format(totalCumbreras))
            }
            resumen.append("TOTAL GENERAL: %.2f mts\n".format(totalCalaminas + totalCumbreras))

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("cotizacion", resumen.toString())
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Cotización copiada al portapapeles", Toast.LENGTH_SHORT).show()
        }
    }

    private fun recalcTotalCalaminas() {
        totalCalaminas = 0f
        for (i in 0 until binding.tableDetalleCalaminas.childCount) {
            val row = binding.tableDetalleCalaminas.getChildAt(i) as TableRow
            val etCant = row.getChildAt(1) as? EditText
            val etLong = row.getChildAt(2) as? EditText
            val cantVal = etCant?.text.toString().toFloatOrNull() ?: 0f
            val longVal = etLong?.text.toString().toFloatOrNull() ?: 0f
            val subtotal = cantVal * longVal

            val tvTotal = row.getChildAt(3) as? TextView
            tvTotal?.text = "%.2f".format(subtotal)

            totalCalaminas += subtotal
        }
        binding.tvTotalCalaminas.text = "TOTAL MTS.: %.2f".format(totalCalaminas)
        recalcTotalGeneral()
    }

    private fun recalcTotalCumbreras() {
        totalCumbreras = 0f
        for (i in 0 until binding.tableDetalleCumbreras.childCount) {
            val row = binding.tableDetalleCumbreras.getChildAt(i) as TableRow
            val etCant = row.getChildAt(1) as? EditText
            val etLong = row.getChildAt(2) as? EditText
            val cantVal = etCant?.text.toString().toFloatOrNull() ?: 0f
            val longVal = etLong?.text.toString().toFloatOrNull() ?: 0f
            val subtotal = cantVal * longVal

            val tvTotal = row.getChildAt(3) as? TextView
            tvTotal?.text = "%.2f".format(subtotal)

            totalCumbreras += subtotal
        }
        binding.tvTotalCumbreras.text = "TOTAL MTS.: %.2f".format(totalCumbreras)
        recalcTotalGeneral()
    }

    private fun recalcTotalGeneral() {
        val totalGeneral = totalCalaminas + totalCumbreras
        binding.tvTotalGeneral.text = "TOTAL GENERAL: %.2f".format(totalGeneral)
    }

    private fun actualizarNumeros(table: TableLayout) {
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            val tvNumero = row.getChildAt(0) as TextView
            tvNumero.text = (i + 1).toString()
        }
    }

}
