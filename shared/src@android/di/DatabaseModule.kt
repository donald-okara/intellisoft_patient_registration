package ke.don.intellisoft_patient_registration.di

import android.content.Context
import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import ke.don.intellisoft_patient_registration.datasource.local.buildAppDatabase
import org.koin.dsl.module

/** Takes a Context as a Koin runtime parameter - resolved via `get<AppDatabase> { parametersOf(context) }`. */
val databaseModule =
    module {
        single<AppDatabase> { (context: Context) -> buildAppDatabase(context) }
    }
