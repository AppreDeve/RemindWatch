package com.example.watch.presentation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableRecyclerView
import com.example.watch.R
import org.koin.androidx.viewmodel.ext.android.viewModel

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
    }

    override fun onResume() {
        super.onResume()
        // Recargar los datos cuando la actividad vuelva a primer plano
        viewModel.loadRecordatorios()
    }
}
