package ke.don.intellisoft_patient_registration.datasource.local

/**
 * Encrypts/decrypts a single String value for at-rest storage (e.g. a PII column, or the auth
 * token). Platform implementations differ in how they protect the key - see
 * `AesGcmEncryptor` (Android, AndroidKeyStore) and the iOS implementation (Keychain-protected
 * key + a multiplatform AES/GCM cipher).
 */
interface FieldEncryptor {
    suspend fun encrypt(plainText: String): String

    suspend fun decrypt(value: String): String
}
