package com.example.smarttv.data.dao

import androidx.room.*
import com.example.smarttv.data.entity.Recordatorio
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {

    @Query("SELECT * FROM recordatorios_tv WHERE status = 1 ORDER BY fechaHora ASC")
    fun getAllRecordatorios(): Flow<List<Recordatorio>>

    @Query("SELECT * FROM recordatorios_tv WHERE id = :id")
    suspend fun getRecordatorioById(id: Int): Recordatorio?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordatorio(recordatorio: Recordatorio)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordatorios(recordatorios: List<Recordatorio>)

    @Update
    suspend fun updateRecordatorio(recordatorio: Recordatorio)

    @Query("DELETE FROM recordatorios_tv WHERE id = :id")
    suspend fun deleteRecordatorioById(id: Int)

    @Query("DELETE FROM recordatorios_tv")
    suspend fun deleteAllRecordatorios()

    @Query("SELECT COUNT(*) FROM recordatorios_tv WHERE status = 1")
    suspend fun getActiveRecordatoriosCount(): Int

    @Query("SELECT * FROM recordatorios_tv WHERE fechaHora >= :startTime AND fechaHora <= :endTime ORDER BY fechaHora ASC")
    fun getRecordatoriosByDateRange(startTime: Long, endTime: Long): Flow<List<Recordatorio>>
}
