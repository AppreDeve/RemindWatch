package com.example.remindwatch.sync

import android.content.Context
import android.util.Log
import com.example.remindwatch.data.dao.PendingSyncDao
import com.example.remindwatch.data.entity.PendingSync
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import data.database.RecordatorioDatabase
import data.database.entity.Recordatorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Manager mejorado para manejar la sincronización bidireccional y el estado offline
 * Ahora incluye sincronización con Smart TV
 */
class SyncManager(private val context: Context) {

    private val TAG = "SyncManager"
    private val db = RecordatorioDatabase.getDatabase(context)
    private val pendingSyncDao = db.pendingSyncDao()
    private val recordatorioDao = db.recordatorioDao()
    private val recordatorioSync = RecordatorioSync(context)
    private val smartTVSyncClient = SmartTVSyncClient(context)

    // IP de la Smart TV
    private val smartTVIP = "192.168.100.25"

    /**
     * Programa una eliminación para sincronizar más tarde
     */
    suspend fun scheduleDelete(recordatorioId: Int) {
        try {
            // Agregar a operaciones pendientes
            val pendingSync = PendingSync(
                recordatorioId = recordatorioId,
                operation = "DELETE",
                timestamp = System.currentTimeMillis()
            )
            pendingSyncDao.insert(pendingSync)
            Log.d(TAG, "Eliminación programada para recordatorio: $recordatorioId")

            // Intentar sincronizar inmediatamente
            attemptSync()

            // Sincronizar con Smart TV
            attemptSmartTVSync()
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar eliminación: ${e.message}")
        }
    }

    /**
     * Programa una creación/actualización para sincronizar más tarde
     */
    suspend fun scheduleCreateOrUpdate(recordatorio: Recordatorio, operation: String) {
        try {
            val data = JSONObject().apply {
                put("id", recordatorio.id)
                put("titulo", recordatorio.titulo)
                put("descripcion", recordatorio.descripcion)
                put("fechaHora", recordatorio.fechaHora)
                put("vencimiento", recordatorio.vencimiento ?: 0L)
                put("recordatorio", recordatorio.recordatorio ?: 0L)
            }.toString()

            val pendingSync = PendingSync(
                recordatorioId = recordatorio.id,
                operation = operation,
                timestamp = System.currentTimeMillis(),
                data = data
            )
            pendingSyncDao.insert(pendingSync)
            Log.d(TAG, "Operación $operation programada para recordatorio: ${recordatorio.id}")

            // Intentar sincronizar inmediatamente
            attemptSync()

            // Sincronizar con Smart TV
            attemptSmartTVSync()
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar $operation: ${e.message}")
        }
    }

    /**
     * Intenta sincronizar todas las operaciones pendientes
     */
    suspend fun attemptSync() {
        try {
            if (!isWearConnected()) {
                Log.d(TAG, "Reloj no conectado, sincronización pospuesta")
                return
            }

            val pendingOperations = pendingSyncDao.getAll()
            Log.d(TAG, "Sincronizando ${pendingOperations.size} operaciones pendientes")

            for (operation in pendingOperations) {
                when (operation.operation) {
                    "DELETE" -> {
                        recordatorioSync.sendDeleteRecordatorio(operation.recordatorioId)
                        pendingSyncDao.delete(operation)
                        Log.d(TAG, "Eliminación sincronizada: ${operation.recordatorioId}")
                    }
                    "CREATE", "UPDATE" -> {
                        operation.data?.let { jsonData ->
                            val json = JSONObject(jsonData)
                            recordatorioSync.sendRecordatorio(
                                json.getInt("id"),
                                json.getString("titulo"),
                                json.getString("descripcion"),
                                json.getLong("fechaHora"),
                                json.getLong("vencimiento"),
                                json.getLong("recordatorio")
                            )
                            pendingSyncDao.delete(operation)
                            Log.d(TAG, "${operation.operation} sincronizada: ${operation.recordatorioId}")
                        }
                    }
                }
            }

            // Enviar sincronización completa para asegurar consistencia
            syncCompleteState()

        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización: ${e.message}")
        }
    }

    /**
     * Sincroniza el estado completo de recordatorios
     */
    suspend fun syncCompleteState() {
        try {
            val allRecordatorios = recordatorioDao.getAll()
            val mappedRecordatorios = allRecordatorios.map { recordatorio ->
                mapOf(
                    "id" to recordatorio.id,
                    "titulo" to recordatorio.titulo,
                    "descripcion" to (recordatorio.descripcion ?: ""),
                    "fechaHora" to recordatorio.fechaHora,
                    "vencimiento" to (recordatorio.vencimiento ?: 0L),
                    "recordatorio" to (recordatorio.recordatorio ?: 0L)
                )
            }

            recordatorioSync.sendRecordatoriosList(mappedRecordatorios)
            Log.d(TAG, "Estado completo sincronizado: ${allRecordatorios.size} recordatorios")
        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar estado completo: ${e.message}")
        }
    }

    /**
     * Verifica si el reloj está conectado
     */
    private suspend fun isWearConnected(): Boolean {
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar conexión del reloj: ${e.message}")
            false
        }
    }

    /**
     * Limpia todas las operaciones pendientes (usar con cuidado)
     */
    suspend fun clearPendingOperations() {
        pendingSyncDao.deleteAll()
        Log.d(TAG, "Todas las operaciones pendientes han sido limpiadas")
    }

    /**
     * Inicia la sincronización completa cuando se detecta reconexión
     */
    fun onWearReconnected() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Reloj reconectado, iniciando sincronización completa")
            attemptSync()
        }
    }

    /**
     * Sincroniza todos los recordatorios con la Smart TV
     */
    suspend fun attemptSmartTVSync() {
        try {
            // Verificar si la TV está disponible
            if (!smartTVSyncClient.checkTVAvailability(smartTVIP)) {
                Log.d(TAG, "Smart TV no disponible en $smartTVIP")
                return
            }

            // Obtener todos los recordatorios activos
            val recordatorios = recordatorioDao.getAll()
            val recordatoriosList = recordatorios.map { recordatorio ->
                mapOf(
                    "id" to recordatorio.id,
                    "titulo" to recordatorio.titulo,
                    "descripcion" to (recordatorio.descripcion ?: ""),
                    "fechaHora" to recordatorio.fechaHora,
                    "vencimiento" to (recordatorio.vencimiento ?: 0L),
                    "recordatorio" to (recordatorio.recordatorio ?: 0L),
                    "status" to recordatorio.status
                )
            }

            // Enviar a la Smart TV
            val success = smartTVSyncClient.syncRecordatoriosToTV(smartTVIP, recordatoriosList)
            if (success) {
                Log.d(TAG, "Sincronización exitosa con Smart TV: ${recordatorios.size} recordatorios")
            } else {
                Log.w(TAG, "Error en sincronización con Smart TV")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar con Smart TV: ${e.message}")
        }
    }

    /**
     * Elimina un recordatorio específico en la Smart TV
     */
    suspend fun deleteRecordatorioInSmartTV(recordatorioId: Int) {
        try {
            if (smartTVSyncClient.checkTVAvailability(smartTVIP)) {
                val success = smartTVSyncClient.deleteRecordatorioInTV(smartTVIP, recordatorioId)
                if (success) {
                    Log.d(TAG, "Recordatorio $recordatorioId eliminado en Smart TV")
                } else {
                    Log.w(TAG, "Error al eliminar recordatorio $recordatorioId en Smart TV")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar en Smart TV: ${e.message}")
        }
    }
}
