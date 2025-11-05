package com.example.metales_galvanizados_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotaVentaAdapter(
    private val notas: List<NotaVentaModel>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<NotaVentaAdapter.NotaVentaViewHolder>() {

    interface OnItemClickListener {
        fun onRespaldoButtonClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaVentaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota_venta, parent, false)
        return NotaVentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaVentaViewHolder, position: Int) {
        val nota = notas[position]
        holder.bind(nota)
    }

    override fun getItemCount(): Int = notas.size

    inner class NotaVentaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ... (las vistas no cambian)
        private val tvNroProforma: TextView = itemView.findViewById(R.id.tvItemNroProforma)
        private val tvCliente: TextView = itemView.findViewById(R.id.tvItemCliente)
        private val tvProducto: TextView = itemView.findViewById(R.id.tvItemProducto)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvItemTotal)
        private val btnRespaldo: Button = itemView.findViewById(R.id.btnTomarFotoRespaldo)

        init {
            btnRespaldo.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRespaldoButtonClick(position)
                }
            }
        }

        fun bind(nota: NotaVentaModel) {
            tvNroProforma.text = "Proforma Nro: ${nota.nroProforma}"
            tvCliente.text = "Cliente: ${nota.cliente}"
            tvProducto.text = "Producto: ${nota.producto} ${nota.color}"
            tvTotal.text = "Total: ${nota.total} Bs"

            if (!nota.fotoRespaldoUri.isNullOrEmpty()) { // Ahora verificamos si el String no es nulo o vacío
                btnRespaldo.text = "Ver Foto"
                btnRespaldo.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_view, 0, 0, 0)
            } else {
                btnRespaldo.text = "Respaldar con Foto"
                btnRespaldo.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_camera, 0, 0, 0)
            }
        }
    }
}