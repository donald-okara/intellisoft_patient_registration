package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor

/**
 * Not a real cipher - a reversible, obviously-not-plaintext transform (char codes shifted by
 * one, reversed) so mapper tests can assert "this isn't the plaintext" without needing a real
 * platform encryptor (AndroidKeyStore, Keychain) in common test code.
 */
class FakeFieldEncryptor : FieldEncryptor {
    override suspend fun encrypt(plainText: String): String = plainText.map { it + 1 }.joinToString("").reversed()

    override suspend fun decrypt(value: String): String = value.reversed().map { it - 1 }.joinToString("")
}
