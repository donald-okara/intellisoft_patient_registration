package ke.don.intellisoft_patient_registration.di

import android.content.Context
import ke.don.intellisoft_patient_registration.data.DefaultPatientRepository
import ke.don.intellisoft_patient_registration.data.DefaultVitalsRepository
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.data.VitalsRepository
import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/** Takes a Context as a Koin runtime parameter, threaded into databaseModule's AppDatabase single. */
val repositoryModule =
    module {
        single<PatientRepository> { (context: Context) ->
            DefaultPatientRepository(
                patientDao = get<AppDatabase> { parametersOf(context) }.patientDao(),
                patientApi = get(),
                patientMapper = get(),
            )
        }
        single<VitalsRepository> { (context: Context) ->
            DefaultVitalsRepository(
                vitalsDao = get<AppDatabase> { parametersOf(context) }.vitalsDao(),
                vitalsApi = get(),
            )
        }
    }
