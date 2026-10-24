package com.example.tiendafcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.channels.Channel

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "pedidos"
        const val TAG = "FCM"

        fun crearCanal(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    CHANNEL_ID, "Pedidos y ofertas", NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Estado de pedidos y promociones" }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(canal)
            }
        }
    }

    /** Se llama cuando llega un mensaje con la app en primer plano (o mensaje de datos). */
    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: message.data["titulo"] ?: "Tienda FCM"
        val cuerpo = message.notification?.body ?: message.data["mensaje"] ?: ""

        // Datos personalizados enviados en el mensaje
        val pedidoId = message.data["pedido_id"]
        val estado = message.data["estado"]
        if (pedidoId != null && estado != null) {
            OrderRepository.actualizarEstado(pedidoId, estado)
        }

        mostrarNotificacion(titulo, cuerpo, pedidoId, estado)
    }

    private fun mostrarNotificacion(titulo: String, cuerpo: String, pedidoId: String?,
                                    estado: String?) {
        crearCanal(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("pedido_id", pedidoId)
            putExtra("estado", estado)
        }
        val pending = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // Android 13+: verificar permiso antes de mostrar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(),
            notif)
    }
}