package com.example.remindwatch

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import data.database.RecordatorioDatabase
import data.database.entity.Recordatorio
import kotlinx.coroutines.launch
import java.util.Calendar
import com.google.android.material.floatingactionbutton.FloatingActionButton




class MainActivity : AppCompatActivity() {

    // Instancia de la base de datos Room
    private lateinit var db: RecordatorioDatabase

    // Timestamps para los campos de fecha y hora
    private var recordatorioTimestamp: Long = 0L
    private var vencimientoTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)

        val button5 = findViewById<Button>(R.id.button5)
        val button6 = findViewById<Button>(R.id.button6)
        val button7 = findViewById<Button>(R.id.button7)

        button5.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        button6.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        button7.setOnClickListener {
            val intent = Intent(this, ExpiradosActivity::class.java)
            startActivity(intent)
        }


        db = RecordatorioDatabase.getDatabase(this)
        cargarRecordatorios()

        // Botón para abrir el diálogo
        val btnAgregar = findViewById<FloatingActionButton>(R.id.btnAgregar)
        btnAgregar.setOnClickListener {
            inicializarUI()
        }
    }


    // Configura los elementos de la interfaz y sus listeners
    private fun inicializarUI() {
        // Solo inicializa el RecyclerView y carga los recordatorios
        cargarRecordatorios()

        // Si tienes un botón para agregar, aquí puedes poner el listener:
        val agregarButton = findViewById<FloatingActionButton>(R.id.dialogButton)
        agregarButton.setOnClickListener {
            mostrarDialogoAgregarRecordatorio()
        }
    }
    // Muestra el diálogo para agregar un nuevo recordatorio
    private fun mostrarDialogoAgregarRecordatorio() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val tituloEditText = dialogView.findViewById<EditText>(R.id.tituloEditText)
        val descripcionEditText = dialogView.findViewById<EditText>(R.id.descripcionEditText)
        val recordatorioEditText = dialogView.findViewById<EditText>(R.id.recordatorioEditText)
        val vencimientoEditText = dialogView.findViewById<EditText>(R.id.vencimientoEditText)
        val guardarButton = dialogView.findViewById<Button>(R.id.guardarButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

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

        guardarButton.setOnClickListener {
            guardarRecordatorio(
                tituloEditText,
                descripcionEditText,
                recordatorioEditText,
                vencimientoEditText
            )
            dialog.dismiss()
        }
    }

    // Carga los recordatorios desde la base de datos y los muestra en el RecyclerView
    private fun cargarRecordatorios() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewRecordatorios)
        val adapter = RecordatorioAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val lista = db.recordatorioDao().getAll()
            adapter.submitList(lista)
        }
    }

    // Guarda un nuevo recordatorio en la base de datos
    private fun guardarRecordatorio(
        tituloEditText: EditText,
        descripcionEditText: EditText,
        recordatorioEditText: EditText,
        vencimientoEditText: EditText
    ) {
        val titulo = tituloEditText.text.toString()
        val descripcion = descripcionEditText.text.toString()

        if (titulo.isNotEmpty()) {
            val recordatorio = Recordatorio(
                titulo = titulo,
                descripcion = descripcion,
                fechaHora = recordatorioTimestamp,
                vencimiento = if (vencimientoTimestamp != 0L) vencimientoTimestamp else 0L,
                recordatorio = if (recordatorioTimestamp != 0L) recordatorioTimestamp else 0L
            )

            lifecycleScope.launch {
                db.recordatorioDao().insert(recordatorio)
                cargarRecordatorios() // Actualiza la lista
            }

            // Limpia los campos de texto
            listOf(
                tituloEditText,
                descripcionEditText,
                recordatorioEditText,
                vencimientoEditText
            ).forEach {
                it.text.clear()
            }
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
}

class ExpiradosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_expirados)

        val button5 = findViewById<Button>(R.id.button5)
        val button6 = findViewById<Button>(R.id.button6)
        val button7 = findViewById<Button>(R.id.button7)

        button5.setOnClickListener {
            val intent = Intent(this, CompletadosActivity::class.java)
            startActivity(intent)
        }

        button6.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        button7.setOnClickListener {
            val intent = Intent(this, ExpiradosActivity::class.java)
            startActivity(intent)
        }

    }
}

class CompletadosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_completados)

        val button5 = findViewById<Button>(R.id.button5)
        val button6 = findViewById<Button>(R.id.button6)
        val button7 = findViewById<Button>(R.id.button7)

        button5.setOnClickListener {
            val intent = Intent(this, CompletadosActivity::class.java)
            startActivity(intent)
        }

        button6.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        button7.setOnClickListener {
            val intent = Intent(this, ExpiradosActivity::class.java)
            startActivity(intent)
        }


    }
}