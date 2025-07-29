package com.example.watch.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.watch.data.dao.RecordatorioDao
import com.example.watch.data.entity.Recordatorio

/**
 * Base de datos Room para almacenar recordatorios en el dispositivo Wear.
 * Implementa el patrón Singleton para evitar múltiples instancias.
 */
@Database(entities = [Recordatorio::class], version = 1, exportSchema = false)
abstract class RecordatorioDatabase : RoomDatabase() {
    /**
     * Proporciona acceso al DAO de recordatorios.
     */
    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        @Volatile
        private var INSTANCE: RecordatorioDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos.
         * @param context Contexto de la aplicación.
         */
        fun getDatabase(context: Context): RecordatorioDatabase {
            return INSTANCE ?: synchronized(this) {
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

