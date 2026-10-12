package ke.don.intellisoft_patient_registration.di

import android.content.Context
import ke.don.intellisoft_patient_registration.data.DefaultGeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.DefaultOverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.DefaultPatientListingRepository
import ke.don.intellisoft_patient_registration.data.DefaultPatientRepository
import ke.don.intellisoft_patient_registration.data.DefaultVitalsRepository
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.PatientListingRepository
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
                patientDao = get<AppDatabase> { parametersOf(context) }.patientDao(),
            )
        }
        single<GeneralAssessmentRepository> { (context: Context) ->
            DefaultGeneralAssessmentRepository(
                generalAssessmentDao = get<AppDatabase> { parametersOf(context) }.generalAssessmentDao(),
                visitApi = get(),
                generalAssessmentMapper = get(),
                patientDao = get<AppDatabase> { parametersOf(context) }.patientDao(),
            )
        }
        single<OverweightAssessmentRepository> { (context: Context) ->
            DefaultOverweightAssessmentRepository(
                overweightAssessmentDao = get<AppDatabase> { parametersOf(context) }.overweightAssessmentDao(),
                visitApi = get(),
                overweightAssessmentMapper = get(),
                patientDao = get<AppDatabase> { parametersOf(context) }.patientDao(),
            )
        }
        single<PatientListingRepository> { (context: Context) ->
            DefaultPatientListingRepository(
                vitalsDao = get<AppDatabase> { parametersOf(context) }.vitalsDao(),
                patientRepository = get { parametersOf(context) },
            )
        }
    }
