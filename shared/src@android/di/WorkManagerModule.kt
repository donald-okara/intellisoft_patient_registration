package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.work.AppWorkerFactory
import org.koin.dsl.module

val workManagerModule =
    module {
        single { AppWorkerFactory() }
    }
