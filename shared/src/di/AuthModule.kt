package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.AuthRepository
import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.data.DefaultAuthRepository
import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import ke.don.intellisoft_patient_registration.datasource.remote.AuthTokenProvider
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Takes TokenStorage as a Koin runtime parameter - tokenStorageModule now provides a real
 * TokenStorage binding, but it also needs a Context on Android, so PatientRegistrationApp
 * resolves this with that TokenStorage explicitly rather than relying on Koin to chain it.
 * Also bound as AuthTokenProvider so httpClientModule's `get()` resolves this same instance.
 *
 * AuthRepository's own `get()` for CachedAuthTokenProvider only resolves once
 * PatientRegistrationApp.onCreate() has rebound it context-free (see Review Focus in the auth
 * sign-in flow plan) - fine in practice since SignInViewModel/SignUpViewModel are only resolved
 * lazily on navigation, well after onCreate() finishes, but it means AuthRepository can't be
 * context-free until that rebind happens, same constraint as httpClientModule.
 */
val authModule =
    module {
        single { (tokenStorage: TokenStorage) -> CachedAuthTokenProvider(tokenStorage) } bind AuthTokenProvider::class
        single<AuthRepository> { DefaultAuthRepository(get(), get()) }
    }
