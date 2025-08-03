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
            "/sync_recordatorios_list" -> handleRecordatoriosListReplacement(messageEvent)
            "/delete_recordatorio" -> handleDeleteRecordatorio(messageEvent)
            "/mobile_connected" -> handleMobileConnected()
            else -> Log.d(TAG, "Path desconocido: ${messageEvent.path}")
        }
    }

    private fun handleSingleRecordatorio(messageEvent: MessageEvent) {
        val data = String(messageEvent.data)
        Log.d(TAG, "Datos recibidos: $data")

        try {
            val json = JSONObject(data)
            val recordatorio = parseRecordatorioFromJson(json)

            CoroutineScope(Dispatchers.IO).launch {
                recordatorioDao.insert(recordatorio)
                Log.d(TAG, "Recordatorio guardado con éxito: ${recordatorio.id} - ${recordatorio.titulo}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar el recordatorio: ${e.message}", e)
        }
    }

    /**
     * Procesa una lista de recordatorios recibida y REEMPLAZA completamente el estado local.
     * Esto asegura que el reloj tenga exactamente los mismos recordatorios que el móvil.
     */
    private fun handleRecordatoriosListReplacement(messageEvent: MessageEvent) {
        val data = String(messageEvent.data)
        Log.d(TAG, "Lista de recordatorios para reemplazo recibida")

        try {
            val jsonArray = JSONArray(data)
            val recordatoriosNuevos = mutableListOf<Recordatorio>()

            // Convierte cada objeto JSON en un Recordatorio
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                recordatoriosNuevos.add(parseRecordatorioFromJson(json))
            }

            // Reemplaza completamente el estado local
            CoroutineScope(Dispatchers.IO).launch {
                // 1. Eliminar todos los recordatorios existentes
                recordatorioDao.deleteAll()
                Log.d(TAG, "Todos los recordatorios locales eliminados")

                // 2. Insertar los nuevos recordatorios
                recordatoriosNuevos.forEach { recordatorio ->
                    recordatorioDao.insert(recordatorio)
                }
                Log.d(TAG, "Estado reemplazado: ${recordatoriosNuevos.size} recordatorios sincronizados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar el reemplazo de recordatorios: ${e.message}", e)
        }
    }

    private fun handleDeleteRecordatorio(messageEvent: MessageEvent) {
        val data = String(messageEvent.data)

        try {
            val id = data.toInt()
            CoroutineScope(Dispatchers.IO).launch {
                recordatorioDao.deleteById(id)
                Log.d(TAG, "Recordatorio eliminado: $id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar el recordatorio: ${e.message}", e)
        }
    }

    /**
     * Maneja cuando el móvil se conecta y solicita sincronización completa
     */
    private fun handleMobileConnected() {
        Log.d(TAG, "Móvil conectado, enviando notificación de conexión del reloj")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RecordatorioSyncHelper.notifyWatchConnected(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error al notificar conexión del reloj: ${e.message}")
            }
        }
    }

    private fun parseRecordatorioFromJson(json: JSONObject): Recordatorio {
        return Recordatorio(
            id = json.optInt("id", 0),
            titulo = json.getString("titulo"),
            descripcion = json.getString("descripcion"),
            fechaHora = json.getLong("fechaHora"),
            vencimiento = json.getLong("vencimiento"),
            recordatorio = json.getLong("recordatorio")
        )
    }
}
