package com.example.remindwatch.sync

import android.content.Context
import data.database.entity.Recordatorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase que maneja la sincronización de recordatorios entre el teléfono y el reloj
 */
class RecordatorioSynchronizer(context: Context) {

    private val recordatorioSync: RecordatorioSync = RecordatorioSync(context)

    /**
     * Sincroniza un recordatorio recién creado con el reloj
     */
    fun syncCreatedRecordatorio(recordatorio: Recordatorio) {
        CoroutineScope(Dispatchers.IO).launch {
            recordatorioSync.sendRecordatorio(
                recordatorio.id,
                recordatorio.titulo,
                recordatorio.descripcion ?: "",
                recordatorio.fechaHora,
                recordatorio.vencimiento ?: 0L,
                recordatorio.recordatorio ?: 0L
            )
        }
    }

    /**
     * Sincroniza un recordatorio actualizado con el reloj
     */
    fun syncUpdatedRecordatorio(recordatorio: Recordatorio) {
        CoroutineScope(Dispatchers.IO).launch {
            recordatorioSync.sendRecordatorio(
                recordatorio.id,
                recordatorio.titulo,
                recordatorio.descripcion ?: "",
                recordatorio.fechaHora,
                recordatorio.vencimiento ?: 0L,
                recordatorio.recordatorio ?: 0L
            )
        }
    }

    /**
     * Sincroniza la eliminación de un recordatorio con el reloj
     */
    fun syncDeletedRecordatorio(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            recordatorioSync.sendDeleteRecordatorio(id)
        }
    }

    /**
     * Sincroniza toda la lista de recordatorios con el reloj
     */
    fun syncAllRecordatorios(recordatorios: List<Recordatorio>) {
        val mappedRecordatorios = recordatorios.map { recordatorio ->
            mapOf(
                "id" to recordatorio.id,
                "titulo" to recordatorio.titulo,
                "descripcion" to (recordatorio.descripcion ?: ""),
                "fechaHora" to recordatorio.fechaHora,
                "vencimiento" to (recordatorio.vencimiento ?: 0L),
                "recordatorio" to (recordatorio.recordatorio ?: 0L)
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            recordatorioSync.sendRecordatoriosList(mappedRecordatorios)
        }
    }
}
