package com.example.watch

import android.app.Application
import com.example.watch.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class WatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar Koin para inyección de dependencias
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@WatchApplication)
            modules(appModule)
        }
    }
}
