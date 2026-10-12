package ke.don.intellisoft_patient_registration.di

import android.content.Context
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.data.VitalsRepository
import ke.don.intellisoft_patient_registration.work.AppWorkerFactory
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Takes a Context as a Koin runtime parameter, same as repositoryModule - each repository it
 * wires into AppWorkerFactory needs one too.
 */
val workManagerModule =
    module {
        single { (context: Context) ->
            AppWorkerFactory(
                patientRepository = get<PatientRepository> { parametersOf(context) },
                vitalsRepository = get<VitalsRepository> { parametersOf(context) },
                generalAssessmentRepository = get<GeneralAssessmentRepository> { parametersOf(context) },
                overweightAssessmentRepository = get<OverweightAssessmentRepository> { parametersOf(context) },
            )
        }
    }
