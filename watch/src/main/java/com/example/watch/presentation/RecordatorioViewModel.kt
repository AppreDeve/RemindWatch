package com.example.watch.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watch.data.dao.RecordatorioDao
import com.example.watch.data.entity.Recordatorio
import kotlinx.coroutines.launch

class RecordatorioViewModel(private val recordatorioDao: RecordatorioDao) : ViewModel() {

    private val _recordatorios = MutableLiveData<List<Recordatorio>>()
    val recordatorios: LiveData<List<Recordatorio>> = _recordatorios

    init {
        loadRecordatorios()
    }

    fun loadRecordatorios() {
        viewModelScope.launch {
            val lista = recordatorioDao.getAll()
            _recordatorios.postValue(lista)
        }
    }

}
