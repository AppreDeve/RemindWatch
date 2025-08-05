package com.example.watch.presentation

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.hardware.SensorManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableRecyclerView
import com.example.watch.R
import com.example.watch.data.entity.Recordatorio
import com.example.watch.data.sync.RecordatorioSyncHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.watch.data.database.RecordatorioDatabase
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import android.text.Editable
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import android.widget.ImageButton




class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: RecordatorioViewModel by viewModel()
    private lateinit var adapter: RecordatorioAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var baseRotationX: Float = 0f
    private var isBaseRotationSet = false
    private var lastForwardTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        recyclerView = findViewById(R.id.recordatorios_list)
        emptyView = findViewById(R.id.empty_view)

        // Configurar el adaptador
        adapter = RecordatorioAdapter(onEditClick = { recordatorio ->
            editarRecordatorio(recordatorio)
        })
        recyclerView.adapter = adapter

        // Observar los cambios en los recordatorios
        viewModel.recordatorios.observe(this) { recordatorios ->
            if (recordatorios.isNotEmpty()) {
                adapter.setRecordatorios(recordatorios)
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            }
        }

        // Cargar los datos
        viewModel.loadRecordatorios()

        // Conectar el botón de agregar
        findViewById<View>(R.id.btn_add).setOnClickListener {
            mostrarDialogoAgregarRecordatorio()
        }

        // Conectar el botón de sincronización
        findViewById<View>(R.id.btn_update).setOnClickListener {
            Log.d("MainActivity", "Botón de sincronización presionado")
            refreshData()
        }

        // Inicializa el sensor de acelerómetro
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (acelerometro != null) {
            Log.d("SensorDebug", "Acelerómetro disponible")
        } else {
            Log.e("SensorDebug", "Acelerómetro NO disponible")
        }

        // Solicitar sincronización completa al iniciar la app
        requestInitialSync()
    }

    /**
     * Solicita sincronización completa al iniciar la aplicación del reloj
     */
    private fun requestInitialSync() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Notificar al móvil que el reloj está conectado
                RecordatorioSyncHelper.notifyWatchConnected(applicationContext)
                // Solicitar sincronización completa
                RecordatorioSyncHelper.requestFullSync(applicationContext)
                Log.d("MainActivity", "Solicitud de sincronización inicial enviada")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en sincronización inicial: ${e.message}")
            }
        }
    }

    /**
     * Refresca los datos solicitando sincronización y recargando la vista
     */
    private fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "Iniciando refresco de datos...")
                // Solicitar sincronización completa del móvil
                RecordatorioSyncHelper.requestFullSync(applicationContext)

                // Recargar datos locales en el hilo principal
                runOnUiThread {
                    viewModel.loadRecordatorios()
                    Log.d("MainActivity", "Datos refrescados exitosamente")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al refrescar datos: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar los datos cuando la actividad vuelva a primer plano
        viewModel.loadRecordatorios()
        // Registrar el listener del sensor
        acelerometro?.also { sensor ->
            val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("SensorDebug", "Sensor registrado: $registered")
        }
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el listener del sensor
        sensorManager.unregisterListener(this)
        Log.d("SensorDebug", "Sensor desregistrado")
    }

    // Detecta el gesto de girar hacia adelante
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = System.currentTimeMillis()
        val rotationX = Math.atan2(y.toDouble(), Math.sqrt((x * x + z * z).toDouble())) * 180 / Math.PI
        // Solo captura la rotación X, sin comparar con baseRotationX
        Log.d("SensorDebug", "RotationX: $rotationX")
        if (rotationX > 30) {
            Log.d("SensorDebug", "Rotación X > 30 detectada")
            if (lastForwardTimestamp + 1500 < now) {
                lastForwardTimestamp = now
                Log.d("SensorDebug", "Gesto detectado: abrir ventana de agregar tarea")
                runOnUiThread {
                    try {
                        Log.d("SensorDebug", "Intentando mostrar diálogo...")
                        mostrarDialogoAgregarRecordatorio()
                        Log.d("SensorDebug", "Diálogo mostrado exitosamente")
                    } catch (e: Exception) {
                        Log.e("SensorDebug", "Error al mostrar diálogo: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario implementar para este caso
    }



    private fun mostrarDialogoAgregarRecordatorio() {
        Log.d("SensorDebug", "Iniciando mostrarDialogoAgregarRecordatorio")
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_recordatorio, null)
            Log.d("SensorDebug", "Layout inflado correctamente")

            val tituloEditText = dialogView.findViewById<EditText>(R.id.tituloEditText)
            val descripcionEditText = dialogView.findViewById<EditText>(R.id.descripcionEditText)
            val vencimientoTextView = dialogView.findViewById<TextView>(R.id.vencimientoTextView)
            val recordatorioTextView = dialogView.findViewById<TextView>(R.id.recordatorioTextView)
            val iconoVencimiento = dialogView.findViewById<View>(R.id.iconoVencimiento)
            val iconoRecordatorio = dialogView.findViewById<View>(R.id.iconoRecordatorio)
            val guardarButton = dialogView.findViewById<Button>(R.id.guardarButton)

            Log.d("SensorDebug", "Views encontradas correctamente")

            var vencimientoTimestamp = 0L
            var recordatorioTimestamp = 0L

            iconoVencimiento.setOnClickListener {
                showDatePicker { timestamp, formattedDate ->
                    vencimientoTimestamp = timestamp
                    vencimientoTextView.text = formattedDate
                }
            }
            iconoRecordatorio.setOnClickListener {
                showDateTimePicker { timestamp, formattedDateTime ->
                    recordatorioTimestamp = timestamp
                    recordatorioTextView.text = formattedDateTime
                }
            }

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            guardarButton.setOnClickListener {
                val titulo = tituloEditText.text.toString()
                val descripcion = descripcionEditText.text.toString()
                val recordatorio = Recordatorio(
                    titulo = titulo,
                    descripcion = descripcion,
                    fechaHora = System.currentTimeMillis(),
                    vencimiento = vencimientoTimestamp,
                    recordatorio = recordatorioTimestamp
                )
                CoroutineScope(Dispatchers.IO).launch {
                    RecordatorioDatabase.getDatabase(applicationContext).recordatorioDao().insert(recordatorio)
                    RecordatorioSyncHelper.syncRecordatorioConMovil(applicationContext, recordatorio)
                }
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("SensorDebug", "Error en mostrarDialogoAgregarRecordatorio: ${e.message}")
        }
    }

    private fun editarRecordatorio(recordatorio: Recordatorio) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_recordatorio, null)

        val tituloEditText = dialogView.findViewById<EditText>(R.id.tituloEditText)
        val descripcionEditText = dialogView.findViewById<EditText>(R.id.descripcionEditText)
        val iconoVencimiento = dialogView.findViewById<ImageButton>(R.id.iconoVencimiento)
        val vencimientoTextView = dialogView.findViewById<TextView>(R.id.vencimientoTextView)
        val iconoRecordatorio = dialogView.findViewById<ImageButton>(R.id.iconoRecordatorio)
        val recordatorioTextView = dialogView.findViewById<TextView>(R.id.recordatorioTextView)


        val formatterFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatterFechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        // Mostrar datos actuales
        tituloEditText.setText(recordatorio.titulo)
        descripcionEditText.setText(recordatorio.descripcion)

        if (recordatorio.vencimiento != null && recordatorio.vencimiento != 0L) {
            vencimientoTextView.text = formatterFecha.format(Date(recordatorio.vencimiento))
        } else {
            vencimientoTextView.text = "Sin fecha"
        }
        if (recordatorio.recordatorio != null && recordatorio.recordatorio != 0L) {
            recordatorioTextView.text = formatterFechaHora.format(Date(recordatorio.recordatorio))
        } else {
            recordatorioTextView.text = "Sin fecha"
        }

        var nuevoVencimiento = recordatorio.vencimiento ?: 0L
        var nuevoRecordatorio = recordatorio.recordatorio ?: 0L

        // Click para elegir fecha de vencimiento
        iconoVencimiento.setOnClickListener {
            showDatePicker { timestamp, formattedDate ->
                nuevoVencimiento = timestamp
                vencimientoTextView.text = formattedDate
            }
        }

        // Click para elegir fecha y hora de recordatorio
        iconoRecordatorio.setOnClickListener {
            showDateTimePicker { timestamp, formattedDateTime ->
                nuevoRecordatorio = timestamp
                recordatorioTextView.text = formattedDateTime
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Recordatorio")
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
                    RecordatorioDatabase.getDatabase(applicationContext)
                        .recordatorioDao()
                        .update(recordatorioActualizado)
                    RecordatorioSyncHelper.syncRecordatorioConMovil(applicationContext, recordatorioActualizado)
                    viewModel.loadRecordatorios()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun showDatePicker(onDateSelected: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day, 0, 0, 0)
            val formattedDate = "%04d-%02d-%02d".format(year, month + 1, day)
            onDateSelected(cal.timeInMillis, formattedDate)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showDateTimePicker(onDateTimeSelected: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                cal.set(year, month, day, hour, minute, 0)
                val formattedDateTime = "%04d-%02d-%02d %02d:%02d".format(year, month + 1, day, hour, minute)
                onDateTimeSelected(cal.timeInMillis, formattedDateTime)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}
