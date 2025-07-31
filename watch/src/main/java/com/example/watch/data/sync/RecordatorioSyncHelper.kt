package com.example.watch.data.sync

import android.content.Context
import android.util.Log
import com.example.watch.data.entity.Recordatorio
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object RecordatorioSyncHelper {
    private const val TAG = "RecordatorioSyncHelper"

    suspend fun syncRecordatorioConMovil(context: Context, recordatorio: Recordatorio) {
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("id", recordatorio.id)
                    put("titulo", recordatorio.titulo)
                    put("descripcion", recordatorio.descripcion)
                    put("fechaHora", recordatorio.fechaHora)
                    put("vencimiento", recordatorio.vencimiento)
                    put("recordatorio", recordatorio.recordatorio)
                }
                val messageClient: MessageClient = Wearable.getMessageClient(context)
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                for (node in nodes) {
                    val task = messageClient.sendMessage(
                        node.id,
                        "/sync_recordatorio",
                        json.toString().toByteArray()
                    )
                    Tasks.await(task)
                    Log.d(TAG, "Recordatorio enviado a nodo: ${node.displayName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al sincronizar recordatorio con móvil: ${e.message}", e)
            }
        }
    }
}

