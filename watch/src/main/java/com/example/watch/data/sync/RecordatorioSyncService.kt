package com.example.watch.data.sync

import android.content.Context
import android.util.Log
import com.example.watch.data.database.RecordatorioDatabase
import com.example.watch.data.dao.RecordatorioDao
import com.example.watch.data.entity.Recordatorio
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray

class RecordatorioSyncService : WearableListenerService() {
    private val TAG = "RecordatorioSyncService"

    init {
        Log.d(TAG, "RecordatorioSyncService constructor llamado")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RecordatorioSyncService onCreate llamado")
    }

    private val recordatorioDao: RecordatorioDao by lazy {
        RecordatorioDatabase.getDatabase(applicationContext).recordatorioDao()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Mensaje recibido en el path: ${messageEvent.path}")

        when (messageEvent.path) {
            "/sync_recordatorio" -> handleSingleRecordatorio(messageEvent)
            "/sync_recordatorios_list" -> handleRecordatoriosList(messageEvent)
            "/delete_recordatorio" -> handleDeleteRecordatorio(messageEvent)
        }
    }

    private fun handleSingleRecordatorio(messageEvent: MessageEvent) {
        val data = String(messageEvent.data)
        Log.d(TAG, "Datos recibidos: $data")

        try {
            val json = JSONObject(data)
            val recordatorio = parseRecordatorioFromJson(json)

            // Guardar en la base de datos en un hilo secundario
            // Guardar en la base de datos en un hilo secundario
            CoroutineScope(Dispatchers.IO).launch {
                recordatorioDao.insert(recordatorio)
                Log.d(TAG, "Recordatorio guardado con éxito: ${recordatorio.id} - ${recordatorio.titulo}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar el recordatorio: ${e.message}", e)
        }
    }

    /**
     * Procesa una lista de recordatorios recibida y los guarda en la base de datos.
     * @param messageEvent Evento de mensaje recibido con los datos en formato JSON Array.
     */
    private fun handleRecordatoriosList(messageEvent: MessageEvent) {
        val data = String(messageEvent.data)
        Log.d(TAG, "Lista de recordatorios recibida")

        try {
            val jsonArray = JSONArray(data)
            val recordatorios = mutableListOf<Recordatorio>()

            // Convierte cada objeto JSON en un Recordatorio y lo agrega a la lista
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                recordatorios.add(parseRecordatorioFromJson(json))
            }

            // Guarda todos los recordatorios en la base de datos en un hilo secundario
            CoroutineScope(Dispatchers.IO).launch {
                recordatorios.forEach { recordatorio ->
                    recordatorioDao.insert(recordatorio)
                }
                Log.d(TAG, "Se guardaron ${recordatorios.size} recordatorios en la base de datos")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar la lista de recordatorios: ${e.message}", e)
        }
    }

    private fun handleDeleteRecordatorio(messageEvent: MessageEvent) {
        try {
            val data = String(messageEvent.data)
            val id = data.toInt()
            CoroutineScope(Dispatchers.IO).launch {
                // Asumiendo que tienes un metodo delete en tu DAO
                recordatorioDao.deleteById(id)
                Log.d(TAG, "Recordatorio eliminado: $id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar el recordatorio: ${e.message}", e)
        }
    }

    private fun parseRecordatorioFromJson(json: JSONObject): Recordatorio {
        return Recordatorio(
            id = json.optInt("id", 0),
            titulo = json.getString("titulo"),
            descripcion = json.optString("descripcion", ""),
            fechaHora = json.optLong("fechaHora", 0L),
            vencimiento = json.optLong("vencimiento", 0L),
            recordatorio = json.optLong("recordatorio", 0L)
        )
    }
}