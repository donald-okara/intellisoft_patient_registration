package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import ke.don.intellisoft_patient_registration.datasource.local.buildAppDatabase
import org.koin.dsl.module

val databaseModule =
    module {
        single<AppDatabase> { buildAppDatabase() }
    }
