/**
 * BroadcastReceiver que se ejecuta cuando llega el momento de mostrar una notificación
 * Se activa por el AlarmManager cuando es hora de recordar algo al usuario
 */
package com.example.remindwatch.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    /**
     * Se ejecuta cuando el AlarmManager activa la alarma programada
     * Extrae los datos del recordatorio y muestra la notificación
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Recibida alarma de notificación")

        try {
            val recordatorioId = intent.getIntExtra("recordatorio_id", -1)
            val titulo = intent.getStringExtra("recordatorio_titulo") ?: "Recordatorio"
            val descripcion = intent.getStringExtra("recordatorio_descripcion") ?: ""

            if (recordatorioId != -1) {
                Log.d(TAG, "Mostrando notificación para recordatorio ID: $recordatorioId")
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification(recordatorioId, titulo, descripcion)
            } else {
                Log.e(TAG, "ID de recordatorio inválido")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar notificación: ${e.message}")
        }
    }
}
