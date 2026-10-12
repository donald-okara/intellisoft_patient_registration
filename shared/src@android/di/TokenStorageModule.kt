package ke.don.intellisoft_patient_registration.di

import android.content.Context
import ke.don.intellisoft_patient_registration.datasource.local.EncryptedTokenStorage
import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import ke.don.intellisoft_patient_registration.datasource.local.buildTokenDataStore
import org.koin.dsl.module

/** Takes a Context as a Koin runtime parameter - resolved via `get<TokenStorage> { parametersOf(context) }`. */
val tokenStorageModule =
    module {
        single<TokenStorage> { (context: Context) ->
            EncryptedTokenStorage(dataStore = buildTokenDataStore(context), encryptor = get(tokenEncryptorQualifier))
        }
    }
