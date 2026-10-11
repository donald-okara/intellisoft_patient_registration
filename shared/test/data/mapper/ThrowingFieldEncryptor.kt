package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor

/** Simulates an unexpected encryption failure - e.g. a Keystore/Keychain error - for repository tests. */
class ThrowingFieldEncryptor : FieldEncryptor {
    override suspend fun encrypt(plainText: String): String = throw RuntimeException("encryption failed")

    override suspend fun decrypt(value: String): String = throw RuntimeException("decryption failed")
}
