package com.example.smarttv.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.smarttv.data.dao.RecordatorioDao
import com.example.smarttv.data.entity.Recordatorio

@Database(
    entities = [Recordatorio::class],
    version = 1,
    exportSchema = false
)
abstract class SmartTVDatabase : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        @Volatile
        private var INSTANCE: SmartTVDatabase? = null

        fun getDatabase(context: Context): SmartTVDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartTVDatabase::class.java,
                    "smarttv_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
