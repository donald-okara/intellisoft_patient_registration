package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.datasource.local.AppleFieldEncryptor
import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor
import org.koin.dsl.bind
import org.koin.dsl.module

val cryptoModule =
    module {
        single(tokenEncryptorQualifier) { AppleFieldEncryptor(keyAccount = "auth_token_key") } bind
            FieldEncryptor::class
        single(piiEncryptorQualifier) { AppleFieldEncryptor(keyAccount = "pii_key") } bind FieldEncryptor::class
    }
