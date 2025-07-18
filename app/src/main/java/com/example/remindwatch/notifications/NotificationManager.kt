/**
 * Manager principal para coordinar las notificaciones con la base de datos
 * Se encarga de sincronizar automáticamente las notificaciones cuando cambian los recordatorios
 */
package com.example.remindwatch.notifications

import android.content.Context
import data.database.RecordatorioDatabase
import data.database.entity.Recordatorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class NotificationManager(private val context: Context) {

    private val notificationHelper = NotificationHelper(context)
    private val database = RecordatorioDatabase.getDatabase(context)
    private val recordatorioDao = database.recordatorioDao()

    companion object {
        private const val TAG = "NotificationManager"
    }

    /**
     * Programa notificaciones para todos los recordatorios activos con fecha de recordatorio
     */
    fun syncAllNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val recordatorios = recordatorioDao.getAll()
                val recordatoriosActivos = recordatorios.filter { rec ->
                    rec.status && rec.recordatorio != null && rec.recordatorio > System.currentTimeMillis()
                }

                Log.d(TAG, "Sincronizando "+recordatoriosActivos.size+" notificaciones")
                notificationHelper.scheduleAllNotifications(recordatoriosActivos)
            } catch (e: Exception) {
                Log.e(TAG, "Error al sincronizar notificaciones: ${e.message}")
            }
        }
    }

    /**
     * Programa notificación para un recordatorio específico
     * @param recordatorio El recordatorio para programar
     */
    fun scheduleNotificationForRecordatorio(recordatorio: Recordatorio) {
        try {
            if (recordatorio.status && recordatorio.recordatorio != null && recordatorio.recordatorio!! > System.currentTimeMillis()) {
                Log.d(TAG, "Programando notificación para: ${recordatorio.titulo}")
                notificationHelper.scheduleNotification(recordatorio)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar notificación: ${e.message}")
        }
    }

    /**
     * Cancela la notificación de un recordatorio específico
     * @param recordatorioId ID del recordatorio
     */
    fun cancelNotificationForRecordatorio(recordatorioId: Int) {
        try {
            Log.d(TAG, "Cancelando notificación para recordatorio ID: $recordatorioId")
            notificationHelper.cancelNotification(recordatorioId)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar notificación: ${e.message}")
        }
    }

    /**
     * Actualiza la notificación cuando se modifica un recordatorio
     * @param recordatorio El recordatorio modificado
     */
    fun updateNotificationForRecordatorio(recordatorio: Recordatorio) {
        try {
            // Primero cancelar la notificación existente
            notificationHelper.cancelNotification(recordatorio.id)

            // Luego programar la nueva si aplica
            scheduleNotificationForRecordatorio(recordatorio)
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar notificación: ${e.message}")
        }
    }

    /**
     * Limpia todas las notificaciones programadas
     */
    fun clearAllNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val recordatorios = recordatorioDao.getAll()
                notificationHelper.cancelAllNotifications(recordatorios)
                Log.d(TAG, "Todas las notificaciones han sido canceladas")
            } catch (e: Exception) {
                Log.e(TAG, "Error al limpiar notificaciones: ${e.message}")
            }
        }
    }
}
