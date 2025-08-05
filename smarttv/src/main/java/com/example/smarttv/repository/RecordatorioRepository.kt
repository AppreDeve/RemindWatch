package com.example.smarttv.repository

import com.example.smarttv.data.dao.RecordatorioDao
import com.example.smarttv.data.entity.Recordatorio
import kotlinx.coroutines.flow.Flow

class RecordatorioRepository(private val recordatorioDao: RecordatorioDao) {

    fun getAllRecordatorios(): Flow<List<Recordatorio>> {
        return recordatorioDao.getAllRecordatorios()
    }

    suspend fun getRecordatorioById(id: Int): Recordatorio? {
        return recordatorioDao.getRecordatorioById(id)
    }

    suspend fun insertRecordatorio(recordatorio: Recordatorio) {
        recordatorioDao.insertRecordatorio(recordatorio)
    }

    suspend fun insertRecordatorios(recordatorios: List<Recordatorio>) {
        recordatorioDao.insertRecordatorios(recordatorios)
    }

    suspend fun deleteRecordatorio(id: Int) {
        recordatorioDao.deleteRecordatorioById(id)
    }

    suspend fun getActiveRecordatoriosCount(): Int {
        return recordatorioDao.getActiveRecordatoriosCount()
    }

    fun getRecordatoriosByDateRange(startTime: Long, endTime: Long): Flow<List<Recordatorio>> {
        return recordatorioDao.getRecordatoriosByDateRange(startTime, endTime)
    }
}
