package data.database.entity.dao
import androidx.room.Dao
import androidx.room.*
import data.database.entity.Recordatorio


@Dao
    interface RecordatorioDao {
        // Aquí puedes definir las operaciones de acceso a datos (DAO) para la entidad Recordatorio
        // Por ejemplo, insertar, actualizar, eliminar y consultar recordatorios.
        // Ejemplo:
         @Insert
         suspend fun insert(recordatorio: Recordatorio): Long

        @Query("SELECT * FROM Recordatorios")
        suspend fun getAll(): List<Recordatorio>

        // Consultas para filtrado por status
        @Query("SELECT * FROM Recordatorios WHERE status = 1 ORDER BY fechaHora ASC")
        suspend fun getPendientes(): List<Recordatorio>

        @Query("SELECT * FROM Recordatorios WHERE status = 0 ORDER BY fechaHora ASC")
        suspend fun getCompletados(): List<Recordatorio>

        // Consulta para recordatorios expirados (fecha de vencimiento pasada)
        @Query("SELECT * FROM Recordatorios WHERE vencimiento < :currentTime AND vencimiento > 0 ORDER BY fechaHora ASC")
        suspend fun getExpirados(currentTime: Long): List<Recordatorio>

        @Update
        suspend fun update(recordatorio: Recordatorio)

        @Delete
        suspend fun delete(recordatorio: Recordatorio)

        @Query("DELETE FROM Recordatorios WHERE id = :id")
        suspend fun deleteById(id: Int)
}
