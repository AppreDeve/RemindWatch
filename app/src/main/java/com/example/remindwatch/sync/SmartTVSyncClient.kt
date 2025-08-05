package com.example.remindwatch.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente para enviar recordatorios a la Smart TV
 */
class SmartTVSyncClient(private val context: Context) {

    private val TAG = "SmartTVSyncClient"
    private val DEFAULT_TV_PORT = 8080

    /**
     * Envía todos los recordatorios a la Smart TV
     */
    suspend fun syncRecordatoriosToTV(
        tvIpAddress: String,
        recordatorios: List<Map<String, Any>>,
        port: Int = DEFAULT_TV_PORT
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$tvIpAddress:$port/sync_recordatorios")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 10000
            }

            // Crear JSON con los recordatorios
            val jsonArray = JSONArray()
            recordatorios.forEach { recordatorio ->
                val jsonObject = JSONObject().apply {
                    put("id", recordatorio["id"] as Int)
                    put("titulo", recordatorio["titulo"] as String)
                    put("descripcion", recordatorio["descripcion"] as? String ?: "")
                    put("fechaHora", recordatorio["fechaHora"] as Long)
                    put("vencimiento", recordatorio["vencimiento"] as? Long ?: 0L)
                    put("recordatorio", recordatorio["recordatorio"] as? Long ?: 0L)
                    put("status", recordatorio["status"] as? Boolean ?: true)
                }
                jsonArray.put(jsonObject)
            }

            // Enviar datos
            val outputWriter = OutputStreamWriter(connection.outputStream)
            outputWriter.write(jsonArray.toString())
            outputWriter.flush()
            outputWriter.close()

            val responseCode = connection.responseCode
            Log.d(TAG, "Respuesta de la TV: $responseCode")

            responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            Log.e(TAG, "Error al sincronizar con Smart TV: ${e.message}")
            false
        }
    }

    /**
     * Elimina un recordatorio específico en la Smart TV
     */
    suspend fun deleteRecordatorioInTV(
        tvIpAddress: String,
        recordatorioId: Int,
        port: Int = DEFAULT_TV_PORT
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$tvIpAddress:$port/delete_recordatorio")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "text/plain")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 10000
            }

            // Enviar ID del recordatorio
            val outputWriter = OutputStreamWriter(connection.outputStream)
            outputWriter.write(recordatorioId.toString())
            outputWriter.flush()
            outputWriter.close()

            val responseCode = connection.responseCode
            Log.d(TAG, "Eliminación en TV - Respuesta: $responseCode")

            responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar recordatorio en Smart TV: ${e.message}")
            false
        }
    }

    /**
     * Verifica si la Smart TV está disponible
     */
    suspend fun checkTVAvailability(
        tvIpAddress: String,
        port: Int = DEFAULT_TV_PORT
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$tvIpAddress:$port/status")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 5000
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Estado de la TV: $responseCode")

            responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            Log.d(TAG, "Smart TV no disponible: ${e.message}")
            false
        }
    }

    /**
     * Descubre automáticamente Smart TVs en la red local
     */
    suspend fun discoverSmartTVs(): List<String> = withContext(Dispatchers.IO) {
        val discoveredTVs = mutableListOf<String>()

        try {
            // Intentar con IPs comunes en redes locales
            val commonIPs = listOf(
                "192.168.1.", "192.168.0.", "10.0.0.", "172.16.0."
            )

            for (baseIP in commonIPs) {
                for (i in 2..254) {
                    val ip = "$baseIP$i"
                    if (checkTVAvailability(ip)) {
                        discoveredTVs.add(ip)
                        Log.d(TAG, "Smart TV encontrada en: $ip")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en descobrimiento de TVs: ${e.message}")
        }

        discoveredTVs
    }
}
