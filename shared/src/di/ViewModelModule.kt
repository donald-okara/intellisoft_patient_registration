package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.feature.patientregistration.PatientRegistrationViewModel
import ke.don.intellisoft_patient_registration.feature.vitals.VitalsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModelOf(::PatientRegistrationViewModel)
        viewModel { params -> VitalsViewModel(get(), get(), get(), patientId = params.get()) }
    }
