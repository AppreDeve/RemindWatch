package com.example.remindwatch.sync

import android.content.Context
import data.database.entity.Recordatorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase que maneja la sincronización de recordatorios entre el teléfono y el reloj
 * Actualizada para manejar estados offline y sincronización bidireccional
 */
class RecordatorioSynchronizer(context: Context) {

    private val syncManager: SyncManager = SyncManager(context)

    /**
     * Sincroniza un recordatorio recién creado con el reloj
     */
    fun syncCreatedRecordatorio(recordatorio: Recordatorio) {
        CoroutineScope(Dispatchers.IO).launch {
            syncManager.scheduleCreateOrUpdate(recordatorio, "CREATE")
        }
    }

    /**
     * Sincroniza un recordatorio actualizado con el reloj
     */
    fun syncUpdatedRecordatorio(recordatorio: Recordatorio) {
        CoroutineScope(Dispatchers.IO).launch {
            syncManager.scheduleCreateOrUpdate(recordatorio, "UPDATE")
        }
    }

    /**
     * Sincroniza la eliminación de un recordatorio con el reloj
     */
    fun syncDeletedRecordatorio(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            syncManager.scheduleDelete(id)
        }
    }

    /**
     * Sincroniza toda la lista de recordatorios con el reloj
     */
    fun syncAllRecordatorios(recordatorios: List<Recordatorio>) {
        CoroutineScope(Dispatchers.IO).launch {
            // Primero programar todas las operaciones individuales
            recordatorios.forEach { recordatorio ->
                syncManager.scheduleCreateOrUpdate(recordatorio, "CREATE")
            }
            // Luego sincronizar el estado completo
            syncManager.syncCompleteState()
        }
    }

    /**
     * Fuerza una sincronización completa
     */
    fun forceSyncAll() {
        CoroutineScope(Dispatchers.IO).launch {
            syncManager.syncCompleteState()
        }
    }

    /**
     * Maneja la reconexión del reloj
     */
    fun onWearReconnected() {
        syncManager.onWearReconnected()
    }
}
