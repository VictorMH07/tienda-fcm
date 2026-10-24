package com.example.tiendafcm

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf

data class Order(val id: String, val producto: String, val estado: String)

/** Repositorio en memoria. La lista es observable por Compose */
object OrderRepository {
    val orders = mutableStateListOf<Order>()
    private var contador = 1001
    private val main = Handler(Looper.getMainLooper())

    fun crearPedido(producto: String): Order {
        val o = Order((contador++).toString(), producto, "Pedido recibido")
        orders.add(0, o)
        return o
    }

    // Se llama desde el servicio FCM (hilo en segundo plano) -> se pasa al hilo principal
    fun actualizarEstado(id: String, estado: String) {
        main.post {
            val i = orders.indexOfFirst { it.id == id }
            if (i >= 0) orders[i] = orders[i].copy(estado = estado)
        }
    }
}