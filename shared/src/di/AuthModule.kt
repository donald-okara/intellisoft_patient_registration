package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import ke.don.intellisoft_patient_registration.datasource.remote.AuthTokenProvider
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Takes TokenStorage as a Koin runtime parameter - tokenStorageModule now provides a real
 * TokenStorage binding, but it also needs a Context on Android, so PatientRegistrationApp
 * resolves this with that TokenStorage explicitly rather than relying on Koin to chain it.
 * Also bound as AuthTokenProvider so httpClientModule's `get()` resolves this same instance.
 */
val authModule =
    module {
        single { (tokenStorage: TokenStorage) -> CachedAuthTokenProvider(tokenStorage) } bind AuthTokenProvider::class
    }
