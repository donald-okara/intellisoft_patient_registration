package ke.don.intellisoft_patient_registration.infra

import org.koin.dsl.module

val workManagerModule =
    module {
        single { AppWorkerFactory() }
    }
