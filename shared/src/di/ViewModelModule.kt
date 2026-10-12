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

/**
 * `params.get()` (no index) resolves by the parameter's reified type, not position - required
 * here since AppNavHost always passes `navigator` as the first parametersOf() argument, ahead of
 * the route's own args. An indexed `params.get(0)`/`params.get(1)` would silently read the wrong
 * slot (navigator instead of patientId) and only fail with a ClassCastException at resolution
 * time, which Module.verify() can't catch - it only checks declared constructor types.
 */
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
                patientId = params.get(),
                vitalsId = params.get(),
            )
        }
        viewModel { params ->
            OverweightAssessmentViewModel(
                get(),
                get(),
                get(),
                patientId = params.get(),
                vitalsId = params.get(),
            )
        }
    }
