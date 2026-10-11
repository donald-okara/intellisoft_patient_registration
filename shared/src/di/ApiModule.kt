package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.datasource.remote.AuthApi
import ke.don.intellisoft_patient_registration.datasource.remote.KtorAuthApi
import ke.don.intellisoft_patient_registration.datasource.remote.KtorPatientApi
import ke.don.intellisoft_patient_registration.datasource.remote.KtorVisitApi
import ke.don.intellisoft_patient_registration.datasource.remote.KtorVitalsApi
import ke.don.intellisoft_patient_registration.datasource.remote.PatientApi
import ke.don.intellisoft_patient_registration.datasource.remote.VisitApi
import ke.don.intellisoft_patient_registration.datasource.remote.VitalsApi
import org.koin.dsl.module

val apiModule =
    module {
        single<AuthApi> { KtorAuthApi(get()) }
        single<PatientApi> { KtorPatientApi(get()) }
        single<VitalsApi> { KtorVitalsApi(get()) }
        single<VisitApi> { KtorVisitApi(get()) }
    }
