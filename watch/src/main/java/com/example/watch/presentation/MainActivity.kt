package com.example.watch.presentation

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    private val viewModel: RecordatorioViewModel by viewModel()
    private lateinit var adapter: RecordatorioAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

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
    }

    override fun onResume() {
        super.onResume()
        // Recargar los datos cuando la actividad vuelva a primer plano
        viewModel.loadRecordatorios()
    }

    private fun mostrarDialogoAgregarRecordatorio() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_recordatorio, null)
        val tituloEditText = dialogView.findViewById<EditText>(R.id.tituloEditText)
        val descripcionEditText = dialogView.findViewById<EditText>(R.id.descripcionEditText)
        val vencimientoEditText = dialogView.findViewById<EditText>(R.id.vencimientoEditText)
        val recordatorioEditText = dialogView.findViewById<EditText>(R.id.recordatorioEditText)
        val guardarButton = dialogView.findViewById<Button>(R.id.guardarButton)

        var vencimientoTimestamp = 0L
        var recordatorioTimestamp = 0L

        vencimientoEditText.setOnClickListener {
            showDatePicker { timestamp, formattedDate ->
                vencimientoTimestamp = timestamp
                vencimientoEditText.setText(formattedDate)
            }
        }
        recordatorioEditText.setOnClickListener {
            showDateTimePicker { timestamp, formattedDateTime ->
                recordatorioTimestamp = timestamp
                recordatorioEditText.setText(formattedDateTime)
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
