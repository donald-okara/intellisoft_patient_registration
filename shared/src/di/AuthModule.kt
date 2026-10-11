package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import org.koin.dsl.module

/**
 * Takes TokenStorage as a Koin runtime parameter - there's no TokenStorage binding yet because
 * nothing but this needs one (no real sign-in flow exists yet either).
 */
val authModule =
    module {
        single { (tokenStorage: TokenStorage) -> CachedAuthTokenProvider(tokenStorage) }
    }
