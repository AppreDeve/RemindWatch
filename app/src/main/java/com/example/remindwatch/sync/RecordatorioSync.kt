package com.example.remindwatch.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Clase responsable de la sincronización de recordatorios entre el teléfono y el reloj.
 * Actualmente solo envía datos del teléfono al reloj.
 * Preparada para futura sincronización bidireccional.
 */
class RecordatorioSync(private val context: Context) {

    private val TAG = "RecordatorioSync"

    /**
     * Envía un solo recordatorio al reloj mediante la API de Wearable.
     * @param id ID del recordatorio
     * @param titulo Título del recordatorio
     * @param descripcion Descripción del recordatorio
     * @param fechaHora Fecha y hora del recordatorio (timestamp)
     * @param vencimiento Fecha de vencimiento (timestamp)
     * @param recordatorio Momento de recordatorio (timestamp)
     */
    suspend fun sendRecordatorio(
        id: Int,
        titulo: String,
        descripcion: String,
        fechaHora: Long,
        vencimiento: Long,
        recordatorio: Long
    ) {
        val jsonObject = JSONObject().apply {
            put("id", id)
            put("titulo", titulo)
            put("descripcion", descripcion)
            put("fechaHora", fechaHora)
            put("vencimiento", vencimiento)
            put("recordatorio", recordatorio)
        }
        val data = jsonObject.toString().toByteArray(StandardCharsets.UTF_8)
        sendMessage("/sync_recordatorio", data)
    }

    /**
     * Envía una lista de recordatorios al reloj
     */
    suspend fun sendRecordatoriosList(recordatorios: List<Map<String, Any>>) {
        val jsonArray = JSONArray()

        recordatorios.forEach { recordatorio ->
            val jsonObject = JSONObject().apply {
                put("id", recordatorio["id"] as Int)
                put("titulo", recordatorio["titulo"] as String)
                put("descripcion", recordatorio["descripcion"] as String)
                put("fechaHora", recordatorio["fechaHora"] as Long)
                put("vencimiento", recordatorio["vencimiento"] as Long)
                put("recordatorio", recordatorio["recordatorio"] as Long)
            }
            jsonArray.put(jsonObject)
        }

        val data = jsonArray.toString().toByteArray(StandardCharsets.UTF_8)
        sendMessage("/sync_recordatorios_list", data)
    }

    /**
     * Envía un mensaje para eliminar un recordatorio en el reloj
     */
    suspend fun sendDeleteRecordatorio(id: Int) {
        val data = id.toString().toByteArray(StandardCharsets.UTF_8)
        sendMessage("/delete_recordatorio", data)
    }

    /**
     * Método auxiliar para enviar mensajes a todos los nodos conectados
     */
    private suspend fun sendMessage(path: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Preparando para enviar mensaje al path: $path")
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            Log.d(TAG, "Nodos conectados encontrados: ${nodes.size}")
            for (node in nodes) {
                val messageClient = Wearable.getMessageClient(context)
                Log.d(TAG, "Enviando mensaje a nodo: ${node.displayName} (${node.id})")
                Tasks.await(messageClient.sendMessage(node.id, path, data))
                Log.d(TAG, "Mensaje enviado a ${node.displayName} en el path $path")
            }
            if (nodes.isEmpty()) {
                Log.w(TAG, "No hay nodos conectados. El mensaje no se envió.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar mensaje: ${e.message}", e)
        }
    }
}
