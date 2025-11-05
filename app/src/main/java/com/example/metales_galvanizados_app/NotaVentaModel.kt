package com.example.metales_galvanizados_app

data class NotaVentaModel(
    val nroProforma: String,
    val cliente: String,
    val vendedor: String,
    val fecha: String,
    val producto: String,
    val color: String,
    val cantidad: String,
    val longitud: String,
    val precioUnitario: String,
    val total: String,
    var fotoRespaldoUri: String? = null
)