package com.example.watch.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un recordatorio en la base de datos.
 * Cada campo corresponde a una columna en la tabla 'recordatorio'.
 */
@Entity(tableName = "recordatorio")
data class Recordatorio(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Identificador único autogenerado
    val titulo: String,        // Título del recordatorio
    val descripcion: String,   // Descripción del recordatorio
    val fechaHora: Long,       // Fecha y hora del evento (timestamp)
    val vencimiento: Long,     // Fecha de vencimiento (timestamp)
    val recordatorio: Long     // Momento para mostrar la notificación (timestamp)
)

