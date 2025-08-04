package com.example.smarttv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttv.data.entity.Recordatorio
import com.example.smarttv.repository.RecordatorioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RecordatorioViewModel(private val repository: RecordatorioRepository) : ViewModel() {

    private val _recordatorios = MutableStateFlow<List<Recordatorio>>(emptyList())
    val recordatorios: StateFlow<List<Recordatorio>> = _recordatorios.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recordatoriosCount = MutableStateFlow(0)
    val recordatoriosCount: StateFlow<Int> = _recordatoriosCount.asStateFlow()

    init {
        loadRecordatorios()
        loadRecordatoriosCount()
    }

    private fun loadRecordatorios() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllRecordatorios().collect { recordatoriosList ->
                _recordatorios.value = recordatoriosList
                _isLoading.value = false
            }
        }
    }

    private fun loadRecordatoriosCount() {
        viewModelScope.launch {
            val count = repository.getActiveRecordatoriosCount()
            _recordatoriosCount.value = count
        }
    }

    fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun getRecordatoriosForToday(): List<Recordatorio> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return _recordatorios.value.filter { recordatorio ->
            recordatorio.fechaHora in startOfDay until endOfDay
        }
    }

    fun getUpcomingRecordatorios(): List<Recordatorio> {
        val now = System.currentTimeMillis()
        return _recordatorios.value.filter { recordatorio ->
            recordatorio.fechaHora > now
        }.sortedBy { it.fechaHora }.take(5)
    }

    fun refreshData() {
        loadRecordatorios()
        loadRecordatoriosCount()
    }
}
