package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.feature.generalassessment.GeneralAssessmentViewModel
import ke.don.intellisoft_patient_registration.feature.overweightassessment.OverweightAssessmentViewModel
import ke.don.intellisoft_patient_registration.feature.patientlisting.PatientListingViewModel
import ke.don.intellisoft_patient_registration.feature.patientregistration.PatientRegistrationViewModel
import ke.don.intellisoft_patient_registration.feature.signin.SignInViewModel
import ke.don.intellisoft_patient_registration.feature.signup.SignUpViewModel
import ke.don.intellisoft_patient_registration.feature.splash.SplashViewModel
import ke.don.intellisoft_patient_registration.feature.vitals.VitalsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModelOf(::SplashViewModel)
        viewModelOf(::SignInViewModel)
        viewModelOf(::SignUpViewModel)
        viewModelOf(::PatientRegistrationViewModel)
        viewModelOf(::PatientListingViewModel)
        viewModel { params -> VitalsViewModel(get(), get(), get(), patientId = params.get()) }
        viewModel { params ->
            GeneralAssessmentViewModel(
                get(),
                get(),
                get(),
                patientId = params.get(0),
                vitalsId = params.get(1),
            )
        }
        viewModel { params ->
            OverweightAssessmentViewModel(
                get(),
                get(),
                get(),
                patientId = params.get(0),
                vitalsId = params.get(1),
            )
        }
    }
