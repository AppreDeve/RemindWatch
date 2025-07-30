package com.example.watch.di

import com.example.watch.data.database.RecordatorioDatabase
import com.example.watch.presentation.RecordatorioViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Base de datos
    single { RecordatorioDatabase.getDatabase(get()) }

    // DAOs
    single { get<RecordatorioDatabase>().recordatorioDao() }

    // ViewModels
    viewModel { RecordatorioViewModel(get()) }
}
