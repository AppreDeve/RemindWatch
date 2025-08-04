package com.example.smarttv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordatorios_tv")
data class Recordatorio(
    @PrimaryKey val id: Int,
    val titulo: String,
    val descripcion: String? = null,
    val fechaHora: Long,
    val vencimiento: Long? = null,
    val recordatorio: Long? = null,
    val status: Boolean = true,
    val fechaSincronizacion: Long = System.currentTimeMillis()
)
