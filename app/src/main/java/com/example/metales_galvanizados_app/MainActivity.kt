package com.example.metales_galvanizados_app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.metales_galvanizados_app.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPieChart(binding.pieChartStock)

        binding.btnCotizacion.setOnClickListener {
            startActivity(Intent(this, CotizacionActivity::class.java))
        }

        binding.btnMapa.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.btnNotaVenta.setOnClickListener {
            startActivity(Intent(this, NotaVenta::class.java))
        }

        binding.btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }

    private fun setupPieChart(pieChart: PieChart) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(4f, "Producción"))  // Ejemplo: 4 bobinas
        entries.add(PieEntry(6f, "Sellado"))     // Ejemplo: 6 bobinas

        val dataSet = PieDataSet(entries, "Stock de Bobinas")

        // Corrección en los colores
        val colors = intArrayOf(
            Color.parseColor("#1E90FF"), // azul eléctrico
            Color.parseColor("#87CEFA")  // azul claro
        )
        dataSet.colors = colors.toMutableList()

        // Configuración adicional recomendada
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setCenterText("Stock") // Método actualizado
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    // Manejo del botón de atrás
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            // Si se presiona atrás dos veces en menos de 2 segundos, salir
            super.onBackPressed()
            finish()
        } else {
            // Mostrar mensaje para presionar de nuevo
            Toast.makeText(this, "Presiona de nuevo para salir", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}