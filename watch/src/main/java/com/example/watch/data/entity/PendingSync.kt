package com.example.watch.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para manejar las operaciones de sincronización pendientes en el reloj
 */
@Entity(tableName = "pending_sync")
data class PendingSync(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val recordatorioId: Int,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val timestamp: Long = System.currentTimeMillis(),
    val data: String? = null // JSON con los datos del recordatorio si es necesario
)
