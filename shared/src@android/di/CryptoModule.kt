package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.datasource.local.AesGcmEncryptor
import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor
import org.koin.dsl.bind
import org.koin.dsl.module

val cryptoModule =
    module {
        single(tokenEncryptorQualifier) { AesGcmEncryptor(keyAlias = "auth_token_key") } bind FieldEncryptor::class
        single(piiEncryptorQualifier) { AesGcmEncryptor(keyAlias = "pii_key") } bind FieldEncryptor::class
    }
