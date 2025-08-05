package com.example.smarttv.network

import android.content.Context
import android.util.Log
import com.example.smarttv.data.entity.Recordatorio
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.*
import java.net.*
import kotlin.concurrent.thread

class RecordatorioReceiver(private val context: Context) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val port = 8080
    private val gson = Gson()

    companion object {
        private const val TAG = "RecordatorioReceiver"
    }

    fun startServer() {
        if (isRunning) return

        thread {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.d(TAG, "Servidor iniciado en puerto $port")

                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { socket ->
                            thread { handleClient(socket) }
                        }
                    } catch (e: SocketException) {
                        if (isRunning) {
                            Log.e(TAG, "Error en servidor: ${e.message}")
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error iniciando servidor: ${e.message}")
            }
        }
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val writer = PrintWriter(clientSocket.getOutputStream(), true)

            val requestLine = reader.readLine()
            Log.d(TAG, "Petición recibida: $requestLine")

            if (requestLine.startsWith("POST /sync")) {
                handleSyncRequest(reader, writer)
            } else {
                sendResponse(writer, 404, "Not Found")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error manejando cliente: ${e.message}")
        } finally {
            try {
                clientSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error cerrando socket: ${e.message}")
            }
        }
    }

    private fun handleSyncRequest(reader: BufferedReader, writer: PrintWriter) {
        try {
            // Leer headers HTTP
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                if (line!!.startsWith("Content-Length:")) {
                    contentLength = line!!.substring(15).trim().toInt()
                }
            }

            // Leer body JSON
            val body = CharArray(contentLength)
            reader.read(body, 0, contentLength)
            val jsonData = String(body)

            Log.d(TAG, "Datos recibidos: $jsonData")

            val recordatoriosResponse = gson.fromJson(jsonData, RecordatoriosResponse::class.java)

            // Aquí puedes procesar los recordatorios recibidos
            // Por ejemplo, guardarlos en la base de datos local
            processRecordatorios(recordatoriosResponse.recordatorios)

            sendResponse(writer, 200, "OK")

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parseando JSON: ${e.message}")
            sendResponse(writer, 400, "Bad Request")
        } catch (e: Exception) {
            Log.e(TAG, "Error en sync: ${e.message}")
            sendResponse(writer, 500, "Internal Server Error")
        }
    }

    private fun processRecordatorios(recordatorios: List<RecordatorioDto>) {
        // Convertir DTOs a entidades y procesar
        recordatorios.forEach { dto ->
            val recordatorio = Recordatorio(
                id = dto.id,
                titulo = dto.titulo,
                descripcion = dto.descripcion,
                fechaHora = dto.fechaHora,
                vencimiento = dto.vencimiento,
                recordatorio = dto.recordatorio,
                status = dto.status
            )

            Log.d(TAG, "Procesando recordatorio: ${recordatorio.titulo}")
            // Aquí puedes guardar en base de datos local
        }
    }

    private fun sendResponse(writer: PrintWriter, statusCode: Int, statusText: String) {
        writer.println("HTTP/1.1 $statusCode $statusText")
        writer.println("Content-Type: application/json")
        writer.println("Connection: close")
        writer.println()
        writer.println("{\"status\": \"$statusText\"}")
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
            Log.d(TAG, "Servidor detenido")
        } catch (e: IOException) {
            Log.e(TAG, "Error deteniendo servidor: ${e.message}")
        }
    }

    fun getServerInfo(): String {
        return if (isRunning) {
            "Servidor activo en puerto $port"
        } else {
            "Servidor inactivo"
        }
    }
}
