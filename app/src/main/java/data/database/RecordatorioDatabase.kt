package data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import data.database.entity.dao.RecordatorioDao
import data.database.entity.Recordatorio
import com.example.remindwatch.data.entity.PendingSync
import com.example.remindwatch.data.dao.PendingSyncDao

//Definimos las entidades, version de la base de datos y exportSchema como false
@Database(
    entities = [Recordatorio::class, PendingSync::class],
    version = 2,
    exportSchema = false
)
//clase de la base de datos
abstract class RecordatorioDatabase : RoomDatabase() {
    // Definimos los DAOs para acceder a los recordatorios y sincronización pendiente
    abstract fun recordatorioDao(): RecordatorioDao
    abstract fun pendingSyncDao(): PendingSyncDao

    // Metodo para obtener una instancia de la base de datos(singleton)
    companion object {
        //visible para toda la aplicacion
        @Volatile
        private var INSTANCE: RecordatorioDatabase? = null

        // Migración de la versión 1 a la 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_sync` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recordatorioId` INTEGER NOT NULL,
                        `operation` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `data` TEXT
                    )
                """.trimIndent())
            }
        }

        // Metodo para obtener la instancia de la base de datos
        fun getDatabase(context: Context): RecordatorioDatabase {
            // Si la instancia ya existe, la retornamos
            return INSTANCE ?: synchronized(this) {
                // Si no existe, la creamos
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecordatorioDatabase::class.java,
                    "recordatorio_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}