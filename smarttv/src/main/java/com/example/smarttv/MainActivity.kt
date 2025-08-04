package com.example.smarttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.smarttv.data.database.SmartTVDatabase
import com.example.smarttv.network.RecordatorioReceiver
import com.example.smarttv.repository.RecordatorioRepository
import com.example.smarttv.ui.screens.SmartTVHomeScreen
import com.example.smarttv.ui.theme.RemindWatchTheme
import com.example.smarttv.viewmodel.RecordatorioViewModel

class MainActivity : ComponentActivity() {

    private lateinit var recordatorioReceiver: RecordatorioReceiver

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el receptor de recordatorios
        recordatorioReceiver = RecordatorioReceiver(this)
        recordatorioReceiver.startServer()

        setContent {
            RemindWatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Crear repository y viewmodel
                    val database = SmartTVDatabase.getDatabase(this@MainActivity)
                    val repository = RecordatorioRepository(database.recordatorioDao())
                    val viewModel = RecordatorioViewModel(repository)

                    SmartTVHomeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recordatorioReceiver.stopServer()
    }
}
