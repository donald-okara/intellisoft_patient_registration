package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import org.koin.dsl.module

/**
 * Binds the single app-wide [Navigator], starting the back stack on the Patient Registration
 * screen - the app's actual entry point per the product spec.
 */
val navigationModule =
    module {
        single { Navigator(startDestination = Route.PatientRegistration) }
    }
