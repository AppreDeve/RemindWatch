package com.example.remindwatch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.example.remindwatch.sync.RecordatorioSynchronizer
import com.example.remindwatch.notifications.NotificationManager

import com.google.android.material.floatingactionbutton.FloatingActionButton
import data.database.RecordatorioDatabase
import data.database.entity.Recordatorio
import kotlinx.coroutines.launch
import java.util.Calendar

import android.os.Build


class MainActivity : AppCompatActivity() {

    // Instancia de la base de datos Room
    private lateinit var db: RecordatorioDatabase

    // Instancia del sincronizador con el reloj
    private lateinit var synchronizer: RecordatorioSynchronizer

    // SwipeRefreshLayout para el gesto de deslizar hacia abajo
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Variable para controlar el filtro actual
    private var filtroActual = "PENDIENTES" // "PENDIENTES", "COMPLETADOS", "EXPIRADOS"

    // Adapter global para poder actualizarlo
    private lateinit var adapter: RecordatorioAdapter

    // Timestamps para los campos de fecha y hora
    private var recordatorioTimestamp: Long = 0L
    private var vencimientoTimestamp: Long = 0L

    // Sensor para detectar sacudidas
    private lateinit var sensorManager: SensorManager
    private var shakeListener: SensorEventListener? = null
    private var lastShakeTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔒 Solicitar permiso para notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Inicializa la base de datos
        db = RecordatorioDatabase.getDatabase(this)

        // Inicializa el sincronizador
        synchronizer = RecordatorioSynchronizer(this)

        // Inicializa la interfaz y el RecyclerView
        inicializarUI()
        cargarRecordatorios()

        // Sincroniza todos los recordatorios al iniciar
        sincronizarTodosLosRecordatorios()

        // Inicializar sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        // Forzar sincronización completa cuando la app vuelve a primer plano
        // Esto ayuda a mantener sincronizados los dispositivos después de estar offline
        lifecycleScope.launch {
            // Pequeño delay para asegurar que la conexión esté establecida
            kotlinx.coroutines.delay(1000)
            synchronizer.forceSyncAll()
        }

        // Registrar listener de sacudida
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            shakeListener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                    val now = System.currentTimeMillis()
                    if (gForce > 2.5 && now - lastShakeTime > 1000) { // Umbral y anti-rebote
                        lastShakeTime = now
                        refreshData()
                    }
                }
            }
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Liberar listener de sacudida
        shakeListener?.let { sensorManager.unregisterListener(it) }
    }


    // Configura los elementos de la interfaz y sus listeners
    private fun inicializarUI() {
        // Inicializar SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("MainActivity", "Gesto de deslizar hacia abajo detectado")
            refreshData()
        }

        // Configurar colores del SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Configurar botones de filtrado
        configurarBotonesFiltrado()

        // Inicializar RecyclerView con adapter
        configurarRecyclerView()

        // Si tienes un botón para agregar, aquí puedes poner el listener:
        val agregarButton = findViewById<FloatingActionButton>(R.id.dialogButton)
        agregarButton.setOnClickListener {
            mostrarDialogoAgregarRecordatorio()
        }
    }

    private fun configurarBotonesFiltrado() {
        val btnPendientes = findViewById<Button>(R.id.button6)
        val btnCompletados = findViewById<Button>(R.id.button5)
        val btnExpirados = findViewById<Button>(R.id.button7)

        btnPendientes.setOnClickListener {
            filtroActual = "PENDIENTES"
            actualizarEstadoBotones()
            cargarRecordatorios()
        }

        btnCompletados.setOnClickListener {
            filtroActual = "COMPLETADOS"
            actualizarEstadoBotones()
            cargarRecordatorios()
        }

        btnExpirados.setOnClickListener {
            filtroActual = "EXPIRADOS"
            actualizarEstadoBotones()
            cargarRecordatorios()
        }

        // Establecer estado inicial
        actualizarEstadoBotones()
    }

    private fun actualizarEstadoBotones() {
        val btnPendientes = findViewById<Button>(R.id.button6)
        val btnCompletados = findViewById<Button>(R.id.button5)
        val btnExpirados = findViewById<Button>(R.id.button7)

        // Resetear todos los botones
        btnPendientes.alpha = 0.6f
        btnCompletados.alpha = 0.6f
        btnExpirados.alpha = 0.6f

        // Resaltar el botón activo
        when (filtroActual) {
            "PENDIENTES" -> btnPendientes.alpha = 1.0f
            "COMPLETADOS" -> btnCompletados.alpha = 1.0f
            "EXPIRADOS" -> btnExpirados.alpha = 1.0f
        }
    }

    private fun configurarRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewRecordatorios)
        adapter = RecordatorioAdapter(
            onDeleteClick = { recordatorio ->
                eliminarRecordatorio(recordatorio)
            },
            onEditClick = { recordatorio ->
                editarRecordatorio(recordatorio)
            },
            onItemClick = { recordatorio ->
                editarRecordatorio(recordatorio)
            },
            onStatusChange = { recordatorio ->
                cambiarStatusRecordatorio(recordatorio)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Refresca los datos solicitando sincronización completa y recargando la vista
     */
    private fun refreshData() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Iniciando refresco de datos...")

                // Forzar sincronización completa
                synchronizer.forceSyncAll()

                // Pequeño delay para permitir que la sincronización se procese
                kotlinx.coroutines.delay(1000)

                // Recargar datos en el hilo principal
                cargarRecordatorios()

                // Detener la animación de refresco
                swipeRefreshLayout.isRefreshing = false

                Log.d("MainActivity", "Datos refrescados exitosamente")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al refrescar datos: ${e.message}")
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // Muestra el diálogo para agregar un nuevo recordatorio
    private fun mostrarDialogoAgregarRecordatorio() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val tituloEditText = dialogView.findViewById<EditText>(R.id.tituloEditText)
        val descripcionEditText = dialogView.findViewById<EditText>(R.id.descripcionEditText)

        val vencimientoLayout = dialogView.findViewById<View>(R.id.vencimientoLayout)
        val recordatorioLayout = dialogView.findViewById<View>(R.id.recordatorioDateLayout)
        val vencimientoTextView = dialogView.findViewById<TextView>(R.id.vencimientoTextView)
        val recordatorioTextView = dialogView.findViewById<TextView>(R.id.recordatorioTextView)
        val guardarButton = dialogView.findViewById<Button>(R.id.guardarButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        vencimientoLayout.setOnClickListener {
            showDatePicker { timestamp, formattedDate ->
                vencimientoTimestamp = timestamp
                vencimientoTextView.text = formattedDate
            }
        }

        recordatorioLayout.setOnClickListener {
            showDateTimePicker { timestamp, formattedDateTime ->
                recordatorioTimestamp = timestamp
                recordatorioTextView.text = formattedDateTime
            }
        }

        guardarButton.setOnClickListener {
            guardarRecordatorio(
                tituloEditText,
                descripcionEditText,
                vencimientoTextView,
                recordatorioTextView
            )
            dialog.dismiss()
        }
    }

    // Carga los recordatorios desde la base de datos y los muestra en el RecyclerView
    private fun cargarRecordatorios() {
        lifecycleScope.launch {
            val lista = when (filtroActual) {
                "PENDIENTES" -> db.recordatorioDao().getPendientes()
                "COMPLETADOS" -> db.recordatorioDao().getCompletados()
                "EXPIRADOS" -> db.recordatorioDao().getExpirados(System.currentTimeMillis())
                else -> db.recordatorioDao().getAll()
            }
            adapter.submitList(lista)
        }
    }

    // Guarda un nuevo recordatorio en la base de datos
    private fun guardarRecordatorio(
        tituloEditText: EditText,
        descripcionEditText: EditText,
        vencimientoTextView: TextView,
        recordatorioTextView: TextView
    ) {
        val titulo = tituloEditText.text.toString()
        val descripcion = descripcionEditText.text.toString()

        if (titulo.isNotEmpty()) {
            val recordatorio = Recordatorio(
                titulo = titulo,
                descripcion = descripcion,
                fechaHora = recordatorioTimestamp,
                vencimiento = if (vencimientoTimestamp != 0L) vencimientoTimestamp else 0L,
                recordatorio = if (recordatorioTimestamp != 0L) recordatorioTimestamp else 0L,
                status = true
            )

            lifecycleScope.launch {
                val id = db.recordatorioDao().insert(recordatorio)
                val recordatorioConId = recordatorio.copy(id = id.toInt())

                // Programar la notificación
                NotificationManager(applicationContext).scheduleNotificationForRecordatorio(recordatorioConId)

                // Sincronizar con el reloj
                synchronizer.syncCreatedRecordatorio(recordatorioConId)
                cargarRecordatorios()
            }

            // Limpia los campos de texto
            listOf(
                tituloEditText,
                descripcionEditText
            ).forEach {
                it.text.clear()
            }

            vencimientoTextView.text = "Seleccionar fecha de vencimiento"
            recordatorioTextView.text = "Seleccionar fecha y hora de recordatorio"
            vencimientoTimestamp = 0L
            recordatorioTimestamp = 0L
        }
    }

    // Función para eliminar un recordatorio
    private fun eliminarRecordatorio(recordatorio: Recordatorio) {
        lifecycleScope.launch {
            // Eliminar de la base de datos local
            db.recordatorioDao().delete(recordatorio)

            // Sincronizar la eliminación con el reloj
            synchronizer.syncDeletedRecordatorio(recordatorio.id)

            cargarRecordatorios() // Actualiza la lista
        }
    }

    // Función para editar un recordatorio existente
    private fun editarRecordatorio(recordatorio: Recordatorio) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_recordatorio, null)

        val tituloEditText = dialogView.findViewById<EditText>(R.id.tituloEditText)
        val descripcionEditText = dialogView.findViewById<EditText>(R.id.descripcionEditText)

        // Cambiar a usar los nuevos elementos con iconos
        val vencimientoLayout = dialogView.findViewById<View>(R.id.vencimientoLayout)
        val recordatorioLayout = dialogView.findViewById<View>(R.id.recordatorioLayout)
        val vencimientoTextView = dialogView.findViewById<TextView>(R.id.vencimientoTextView)
        val recordatorioTextView = dialogView.findViewById<TextView>(R.id.recordatorioTextView)

        // Variables para almacenar los nuevos valores de timestamp
        var nuevoVencimiento = recordatorio.vencimiento ?: 0L
        var nuevoRecordatorio = recordatorio.recordatorio ?: 0L

        // Rellenar los campos con los datos actuales
        tituloEditText.setText(recordatorio.titulo)
        descripcionEditText.setText(recordatorio.descripcion)

        // Mostrar las fechas actuales en los TextViews
        if (nuevoVencimiento != 0L) {
            vencimientoTextView.text = android.text.format.DateFormat.format("yyyy-MM-dd", nuevoVencimiento)
        } else {
            vencimientoTextView.text = "Seleccionar fecha de vencimiento"
        }

        if (nuevoRecordatorio != 0L) {
            recordatorioTextView.text = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", nuevoRecordatorio)
        } else {
            recordatorioTextView.text = "Seleccionar fecha y hora de recordatorio"
        }

        // Configurar click listeners para los iconos
        vencimientoLayout.setOnClickListener {
            showDatePicker { timestamp, formattedDate ->
                nuevoVencimiento = timestamp
                vencimientoTextView.text = formattedDate
            }
        }

        recordatorioLayout.setOnClickListener {
            showDateTimePicker { timestamp, formattedDateTime ->
                nuevoRecordatorio = timestamp
                recordatorioTextView.text = formattedDateTime
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar recordatorio")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoTitulo = tituloEditText.text.toString()
                val nuevaDescripcion = descripcionEditText.text.toString()
                val recordatorioActualizado = recordatorio.copy(
                    titulo = nuevoTitulo,
                    descripcion = nuevaDescripcion,
                    vencimiento = nuevoVencimiento,
                    recordatorio = nuevoRecordatorio
                )
                lifecycleScope.launch {
                    db.recordatorioDao().update(recordatorioActualizado)
                    synchronizer.syncUpdatedRecordatorio(recordatorioActualizado)
                    cargarRecordatorios()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    // Función para sincronizar todos los recordatorios con el reloj
    private fun sincronizarTodosLosRecordatorios() {
        lifecycleScope.launch {
            val recordatorios = db.recordatorioDao().getAll()
            synchronizer.syncAllRecordatorios(recordatorios)
        }
    }

    // Muestra un selector de fecha y retorna el timestamp y la fecha formateada
    private fun showDatePicker(onDateSelected: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day, 0, 0, 0)
            val formattedDate = "%04d-%02d-%02d".format(year, month + 1, day)
            onDateSelected(cal.timeInMillis, formattedDate)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Muestra un selector de fecha y hora y retorna el timestamp y la fecha/hora formateada
    private fun showDateTimePicker(onDateTimeSelected: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                cal.set(year, month, day, hour, minute, 0)
                val formattedDateTime =
                    "%04d-%02d-%02d %02d:%02d".format(year, month + 1, day, hour, minute)
                onDateTimeSelected(cal.timeInMillis, formattedDateTime)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun cambiarStatusRecordatorio(recordatorio: Recordatorio) {
        lifecycleScope.launch {
            // Actualizar en la base de datos local
            db.recordatorioDao().update(recordatorio)

            // Sincronizar con el reloj
            synchronizer.syncUpdatedRecordatorio(recordatorio)

            // Recargar la lista con el filtro actual
            cargarRecordatorios()
        }
    }
}
