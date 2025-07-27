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
         suspend fun insert(recordatorio: Recordatorio)

        @Query("SELECT * FROM Recordatorios")
        suspend fun getAll(): List<Recordatorio>

        @Update
        suspend fun update(recordatorio: Recordatorio)

        @Delete
        suspend fun delete(recordatorio: Recordatorio)
    }