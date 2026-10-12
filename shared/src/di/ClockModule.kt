package ke.don.intellisoft_patient_registration.di

import kotlinx.datetime.Clock
import org.koin.dsl.module

/**
 * Binds the real system clock so ViewModels can take a [Clock] as a constructor parameter.
 * Tests inject a fixed clock instead, which keeps date-dependent logic deterministic.
 */
val clockModule =
    module {
        single<Clock> { Clock.System }
    }
