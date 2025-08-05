package com.example.watch.data.dao

import androidx.room.*
import com.example.watch.data.entity.PendingSync

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY timestamp ASC")
    suspend fun getAll(): List<PendingSync>

    @Query("SELECT * FROM pending_sync WHERE recordatorioId = :recordatorioId")
    suspend fun getByRecordatorioId(recordatorioId: Int): List<PendingSync>

    @Insert
    suspend fun insert(pendingSync: PendingSync)

    @Delete
    suspend fun delete(pendingSync: PendingSync)

    @Query("DELETE FROM pending_sync WHERE recordatorioId = :recordatorioId")
    suspend fun deleteByRecordatorioId(recordatorioId: Int)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()
}
