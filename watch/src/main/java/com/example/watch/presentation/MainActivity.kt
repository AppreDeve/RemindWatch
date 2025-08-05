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
import android.widget.DatePicker
import android.widget.TimePicker
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
        adapter = RecordatorioAdapter()
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

    private fun showDatePicker(onDateSelected: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()

        // Crear un layout personalizado para el date picker
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val datePicker = DatePicker(this).apply {
            init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null)
            // Ajustar el tamaño para pantallas pequeñas
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }

        val buttonLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        val btnCancel = Button(this).apply {
            text = "Cancelar"
            textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#757575"))
        }

        val btnOk = Button(this).apply {
            text = "Aceptar"
            textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#601E8C"))
        }

        buttonLayout.addView(btnCancel)
        buttonLayout.addView(btnOk)

        layout.addView(datePicker)
        layout.addView(buttonLayout)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Seleccionar Fecha")
            .setView(layout)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnOk.setOnClickListener {
            val year = datePicker.year
            val month = datePicker.month
            val day = datePicker.dayOfMonth

            cal.set(year, month, day, 0, 0, 0)
            val formattedDate = "%04d-%02d-%02d".format(year, month + 1, day)
            onDateSelected(cal.timeInMillis, formattedDate)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDateTimePicker(onDateTimeSelected: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()

        // Primero mostrar selector de fecha
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val datePicker = DatePicker(this).apply {
            init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }

        val buttonLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        val btnCancel = Button(this).apply {
            text = "Cancelar"
            textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#757575"))
        }

        val btnNext = Button(this).apply {
            text = "Siguiente"
            textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#601E8C"))
        }

        buttonLayout.addView(btnCancel)
        buttonLayout.addView(btnNext)

        layout.addView(datePicker)
        layout.addView(buttonLayout)

        val dateDialog = AlertDialog.Builder(this)
            .setTitle("Seleccionar Fecha")
            .setView(layout)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dateDialog.dismiss()
        }

        btnNext.setOnClickListener {
            val year = datePicker.year
            val month = datePicker.month
            val day = datePicker.dayOfMonth

            dateDialog.dismiss()

            // Ahora mostrar selector de hora
            showTimePicker(year, month, day, onDateTimeSelected)
        }

        dateDialog.show()
    }

    private fun showTimePicker(year: Int, month: Int, day: Int, onDateTimeSelected: (Long, String) -> Unit) {
        val cal = Calendar.getInstance()

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val timePicker = TimePicker(this).apply {
            setIs24HourView(true)
            hour = cal.get(Calendar.HOUR_OF_DAY)
            minute = cal.get(Calendar.MINUTE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }

        val buttonLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        val btnCancel = Button(this).apply {
            text = "Cancelar"
            textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#757575"))
        }

        val btnOk = Button(this).apply {
            text = "Aceptar"
            textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#601E8C"))
        }

        buttonLayout.addView(btnCancel)
        buttonLayout.addView(btnOk)

        layout.addView(timePicker)
        layout.addView(buttonLayout)

        val timeDialog = AlertDialog.Builder(this)
            .setTitle("Seleccionar Hora")
            .setView(layout)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            timeDialog.dismiss()
        }

        btnOk.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute

            cal.set(year, month, day, hour, minute, 0)
            val formattedDateTime = "%04d-%02d-%02d %02d:%02d".format(year, month + 1, day, hour, minute)
            onDateTimeSelected(cal.timeInMillis, formattedDateTime)
            timeDialog.dismiss()
        }

        timeDialog.show()
    }
}
