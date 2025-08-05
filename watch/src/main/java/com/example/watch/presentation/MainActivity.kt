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
import com.example.watch.R
import com.example.watch.data.entity.Recordatorio
import com.example.watch.data.sync.RecordatorioSyncHelper
import com.example.watch.data.database.RecordatorioDatabase
import com.example.watch.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        // Botón de agregar
        findViewById<View>(R.id.btn_add).setOnClickListener {
            mostrarDialogoAgregarRecordatorio()
        }

        // Botón de sincronización
        findViewById<View>(R.id.btn_update).setOnClickListener {
            refreshData()
        }

        // Acelerómetro
        //sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        //acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Sincronización inicial
        requestInitialSync()
    }

    private fun requestInitialSync() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RecordatorioSyncHelper.notifyWatchConnected(applicationContext)
                RecordatorioSyncHelper.requestFullSync(applicationContext)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en sincronización inicial: ${e.message}")
            }
        }
    }

    private fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RecordatorioSyncHelper.requestFullSync(applicationContext)
                runOnUiThread {
                    viewModel.loadRecordatorios()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al refrescar datos: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRecordatorios()
        acelerometro?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = System.currentTimeMillis()
        val rotationX = Math.atan2(y.toDouble(), Math.sqrt((x * x + z * z).toDouble())) * 180 / Math.PI
        if (rotationX > 30 && lastForwardTimestamp + 1500 < now) {
            lastForwardTimestamp = now
            runOnUiThread {
                mostrarDialogoAgregarRecordatorio()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun mostrarDialogoAgregarRecordatorio() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_recordatorio, null)

        val tituloEditText = dialogView.findViewById<EditText>(R.id.tituloEditText)
        val descripcionEditText = dialogView.findViewById<EditText>(R.id.descripcionEditText)
        val vencimientoTextView = dialogView.findViewById<TextView>(R.id.vencimientoTextView)
        val recordatorioTextView = dialogView.findViewById<TextView>(R.id.recordatorioTextView)
        val iconoVencimiento = dialogView.findViewById<View>(R.id.iconoVencimiento)
        val iconoRecordatorio = dialogView.findViewById<View>(R.id.iconoRecordatorio)
        val guardarButton = dialogView.findViewById<Button>(R.id.guardarButton)

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
                // Guardar en base de datos local
                RecordatorioDatabase.getDatabase(applicationContext).recordatorioDao().insert(recordatorio)

                // ✅ Programar notificación en el reloj
                NotificationHelper(applicationContext).scheduleNotification(recordatorio)

                // Sincronizar con el celular
                RecordatorioSyncHelper.syncRecordatorioConMovil(applicationContext, recordatorio)
            }
            dialog.dismiss()
        }

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
