package com.example.smarttv.network

import com.google.gson.annotations.SerializedName

data class RecordatorioDto(
    @SerializedName("id") val id: Int,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fechaHora") val fechaHora: Long,
    @SerializedName("vencimiento") val vencimiento: Long? = null,
    @SerializedName("recordatorio") val recordatorio: Long? = null,
    @SerializedName("status") val status: Boolean = true
)

data class RecordatoriosResponse(
    @SerializedName("recordatorios") val recordatorios: List<RecordatorioDto>,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class SyncRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("lastSync") val lastSync: Long
)
