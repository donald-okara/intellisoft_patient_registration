package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import org.koin.dsl.module

/**
 * Binds the single app-wide [Navigator], starting the back stack on the Splash screen - it
 * decides at runtime whether Sign In or Patient Listing comes next, based on a saved token.
 */
val navigationModule =
    module {
        single { Navigator(startDestination = Route.Splash) }
    }
