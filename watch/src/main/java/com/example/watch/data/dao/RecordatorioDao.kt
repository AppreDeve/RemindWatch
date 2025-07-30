package com.example.watch.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.watch.data.entity.Recordatorio

/**
 * DAO (Data Access Object) para la entidad Recordatorio.
 * Define las operaciones de acceso a la base de datos para la tabla 'recordatorio'.
 */
@Dao
interface RecordatorioDao {
    /**
     * Obtiene todos los recordatorios almacenados en la base de datos.
     * @return Lista de recordatorios.
     */
    @Query("SELECT * FROM recordatorio")
    suspend fun getAll(): List<Recordatorio>

    /**
     * Inserta un recordatorio en la base de datos.
     * Si ya existe un recordatorio con el mismo id, lo reemplaza.
     * @param recordatorio El recordatorio a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recordatorio: Recordatorio)

    /**
     * Actualiza un recordatorio existente en la base de datos.
     * @param recordatorio El recordatorio a actualizar.
     */
    @Update
    suspend fun update(recordatorio: Recordatorio)

    /**
     * Elimina un recordatorio por su id.
     * @param id Identificador del recordatorio a eliminar.
     */
    @Query("DELETE FROM recordatorio WHERE id = :id")
    suspend fun deleteById(id: Int)
}
