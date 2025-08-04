package com.example.remindwatch.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import data.database.RecordatorioDatabase
import data.database.entity.Recordatorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class RecordatorioSyncService : WearableListenerService() {

    private val TAG = "RecordatorioSyncService"
    private lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        syncManager = SyncManager(applicationContext)
        Log.d(TAG, "RecordatorioSyncService creado")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Mensaje recibido en el path: ${messageEvent.path}")

        when (messageEvent.path) {
            "/sync_recordatorio" -> {
                val data = String(messageEvent.data)
                val json = JSONObject(data)
                val recordatorio = Recordatorio(
                    id = json.optInt("id", 0),
                    titulo = json.getString("titulo"),
                    descripcion = json.getString("descripcion"),
                    fechaHora = json.getLong("fechaHora"),
                    vencimiento = json.getLong("vencimiento"),
                    recordatorio = json.getLong("recordatorio")
                )
                guardarRecordatorio(applicationContext, recordatorio)
            }
            "/delete_recordatorio" -> {
                val id = String(messageEvent.data).toIntOrNull()
                if (id != null) {
                    eliminarRecordatorio(applicationContext, id)
                }
            }
            "/request_full_sync" -> {
                // El reloj solicita una sincronización completa
                Log.d(TAG, "Solicitud de sincronización completa recibida desde el reloj")
                CoroutineScope(Dispatchers.IO).launch {
                    syncManager.syncCompleteState()
                }
            }
            "/watch_connected" -> {
                // El reloj se ha conectado
                Log.d(TAG, "Reloj conectado, iniciando sincronización")
                syncManager.onWearReconnected()
            }
        }
    }

    private fun guardarRecordatorio(context: Context, recordatorio: Recordatorio) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = RecordatorioDatabase.getDatabase(context)
            db.recordatorioDao().insert(recordatorio)
            Log.d(TAG, "Recordatorio guardado desde Wear: ${recordatorio.titulo}")

            // Programar sincronización con Smart TV
            syncManager.scheduleCreateOrUpdate(recordatorio, "UPDATE")
        }
    }

    private fun eliminarRecordatorio(context: Context, id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = RecordatorioDatabase.getDatabase(context)
            db.recordatorioDao().deleteById(id)
            Log.d(TAG, "Recordatorio eliminado desde Wear: $id")

            // Programar sincronización con Smart TV
            syncManager.scheduleDelete(id)
        }
    }
}
