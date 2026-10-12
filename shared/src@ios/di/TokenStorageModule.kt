package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.datasource.local.KeychainTokenStorage
import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import org.koin.dsl.module

/** No Context needed on iOS - Keychain storage is parameterless, unlike Android's. */
val tokenStorageModule =
    module {
        single<TokenStorage> { KeychainTokenStorage() }
    }
