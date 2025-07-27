package data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import data.database.entity.dao.RecordatorioDao
import data.database.entity.Recordatorio

//Definimos la entidad Recordatorio, version de la base de datos y exportSchema como false
@Database(entities = [Recordatorio::class], version = 1, exportSchema = false)
//clase de la base de datos
abstract class RecordatorioDatabase : RoomDatabase() {
    // Definimos el DAO para acceder a los recordatorios
    abstract fun recordatorioDao(): RecordatorioDao
    // Metodo para obtener una instancia de la base de datos(singleton)
    companion object {
        //visible para toda la aplicacion
        @Volatile
        private var INSTANCE: RecordatorioDatabase? = null
        // Metodo para obtener la instancia de la base de datos
        fun getDatabase(context: Context): RecordatorioDatabase {
            // Si la instancia ya existe, la retornamos
            return INSTANCE ?: synchronized(this) {
                // Si no existe, la creamos
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecordatorioDatabase::class.java,
                    "recordatorio_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}