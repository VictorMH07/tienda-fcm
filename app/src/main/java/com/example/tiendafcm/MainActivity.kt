package com.example.tiendafcm

import android.Manifest
import android.app.ComponentCaller
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val permisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        {concedido ->
            val msg = if (concedido) "Notificaciones activas"
            else "Sin permiso: no se mostrarán notificaciones"
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyFirebaseMessagingService.crearcanal(this)
        pedirPermisoNotificaciones()
        procesarIntent(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TiendaScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarIntent(intent)
    }

    /** Si se abrió desde una notificación (app en segundo plano), aplica los satos recibidos */
    private fun procesarIntent(i: Intent) {
        val id = i?.getStringExtra("pedidio_id")
        val estado = i?.getStringExtra("estado")
        if (id != null && estado != null) OrderRepository.actualizarEstado(id, estado)
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun TiendaScreen() {
    val context = LocalContext.current
    var token by remember { mutableStateOf("Obteniendo token...") }
    var ofertas by remember { mutableStateOf(false) }

    // Obtener el token de regidtro de FCM al abrir la pantalla
    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            token = if (task.isSuccessful) task.result else "No se puede obtener el token"
            Log.d("FCM", "Token: $token")
        }
    }

    val productos = listOf("Audifonos Bluetooth", "Teclado mecánico", "Mouse inalambrico")

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.pd),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Tienda FCM", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Token de registro (cópialo para probar en Firebase Console):",
                fontWeight = FontWeight.Bold)
            Text(token, fontSize = 12.sp)
            Button(onClick = {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("token", token))
                Toast.makeText(context, "Token copiado", Toast.LENGTH_SHORT).show()
            }) { Text("Copiar token") }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recibir ofertas (tema: ofertas)", modifier = Modifier.weight(1f))
                Switch(checked = Ofertas, onCheckedChange = { activo ->
                    ofertas = activo
                    val fm = FirebaseMessaging.getInstance()
                    if (activo) fm.subscribeToTopic("ofertas").addOnSuccessListener {
                        Toast.makeText(context, "Suscrito al tema 'ofertas'",
                            Toast.LENGTH_SHORT).show()
                    } else fm.unsubscribeFromTopic("ofertas").addOnSuccessListener {
                        Toast.makeText(context, "Suscripción cancelada",
                            Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        item { Text("Productos", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        items(productos) { nombre ->
            Button(
                onClick = {
                    val o = OrderRepository.crearPedido(nombre)
                    Toast.makeText(context, "Pedido #${o.id} creado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Comprar: $nombre") }
        }

        item { Text("Mis pedidos", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        items(OrderRepository.orders, key = { it.id }) { o ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Pedido #${o.id} - ${o.producto}", fontWeight = FontWeight.Bold)
                    Text("Estado: ${o.estado}")
                }
            }
        }
    }
}