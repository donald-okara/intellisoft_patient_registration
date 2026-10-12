package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.DefaultGeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.DefaultOverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.DefaultPatientRepository
import ke.don.intellisoft_patient_registration.data.DefaultVitalsRepository
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.data.VitalsRepository
import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import org.koin.dsl.module

val repositoryModule =
    module {
        single<PatientRepository> {
            DefaultPatientRepository(
                patientDao = get<AppDatabase>().patientDao(),
                patientApi = get(),
                patientMapper = get(),
            )
        }
        single<VitalsRepository> {
            DefaultVitalsRepository(
                vitalsDao = get<AppDatabase>().vitalsDao(),
                vitalsApi = get(),
            )
        }
        single<GeneralAssessmentRepository> {
            DefaultGeneralAssessmentRepository(
                generalAssessmentDao = get<AppDatabase>().generalAssessmentDao(),
                visitApi = get(),
                generalAssessmentMapper = get(),
            )
        }
        single<OverweightAssessmentRepository> {
            DefaultOverweightAssessmentRepository(
                overweightAssessmentDao = get<AppDatabase>().overweightAssessmentDao(),
                visitApi = get(),
                overweightAssessmentMapper = get(),
            )
        }
    }
