/**
 * Helper class para manejar las notificaciones de recordatorios
 * Se encarga de programar, cancelar y mostrar notificaciones basadas en los recordatorios de la BD
 */
package com.example.remindwatch.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.remindwatch.MainActivity
import com.example.remindwatch.R
import data.database.entity.Recordatorio
import java.util.Date

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "recordatorio_channel"
        const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    /**
     * Crea el canal de notificación requerido para Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios"
            val descriptionText = "Notificaciones de recordatorios programados"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Programa una notificación para un recordatorio específico
     * @param recordatorio El recordatorio para el cual programar la notificación
     */
    fun scheduleNotification(recordatorio: Recordatorio) {
        // Solo programar si tiene tiempo de recordatorio establecido
        recordatorio.recordatorio?.let { tiempoRecordatorio ->
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("recordatorio_id", recordatorio.id)
                putExtra("recordatorio_titulo", recordatorio.titulo)
                putExtra("recordatorio_descripcion", recordatorio.descripcion ?: "")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                recordatorio.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Verificar si la fecha es en el futuro
            if (tiempoRecordatorio > System.currentTimeMillis()) {
                Log.d("NotificationHelper", "Programando alarma para el recordatorio ID: ${recordatorio.id} a las ${Date(tiempoRecordatorio)}")
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        tiempoRecordatorio,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // En caso de que no tenga permisos para alarmas exactas
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        tiempoRecordatorio,
                        pendingIntent
                    )
                }
            }
        }
    }

    /**
     * Cancela una notificación programada
     * @param recordatorioId ID del recordatorio cuya notificación cancelar
     */
    fun cancelNotification(recordatorioId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            recordatorioId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    /**
     * Muestra una notificación inmediata
     * @param recordatorioId ID del recordatorio
     * @param titulo Título del recordatorio
     * @param descripcion Descripción del recordatorio
     */
    fun showNotification(recordatorioId: Int, titulo: String, descripcion: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Necesitarás agregar este icono
            .setContentTitle("Recordatorio: $titulo")
            .setContentText(descripcion.ifEmpty { "Tienes un recordatorio pendiente" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + recordatorioId,
                notification
            )
        } catch (e: SecurityException) {
            // Manejar caso donde no hay permisos de notificación
            e.printStackTrace()
        }
    }

    /**
     * Programa notificaciones para todos los recordatorios con tiempo de recordatorio
     * @param recordatorios Lista de recordatorios
     */
    fun scheduleAllNotifications(recordatorios: List<Recordatorio>) {
        recordatorios.forEach { recordatorio ->
            if (recordatorio.status && recordatorio.recordatorio != null) {
                scheduleNotification(recordatorio)
            }
        }
    }

    /**
     * Cancela todas las notificaciones de una lista de recordatorios
     * @param recordatorios Lista de recordatorios
     */
    fun cancelAllNotifications(recordatorios: List<Recordatorio>) {
        recordatorios.forEach { recordatorio ->
            cancelNotification(recordatorio.id)
        }
    }
}
