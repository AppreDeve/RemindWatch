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
    override fun onMessageReceived(messageEvent: MessageEvent) {
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
        }
    }

    private fun guardarRecordatorio(context: Context, recordatorio: Recordatorio) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = RecordatorioDatabase.getDatabase(context)
            db.recordatorioDao().insert(recordatorio)
            Log.d("RecordatorioSyncService", "Recordatorio guardado desde Wear: ${recordatorio.titulo}")
        }
    }

    private fun eliminarRecordatorio(context: Context, id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = RecordatorioDatabase.getDatabase(context)
            db.recordatorioDao().deleteById(id)
            Log.d("RecordatorioSyncService", "Recordatorio eliminado desde Wear: $id")
        }
    }
}

