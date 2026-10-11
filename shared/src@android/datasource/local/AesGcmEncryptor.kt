package ke.don.intellisoft_patient_registration.datasource.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val GCM_IV_LENGTH_BYTES = 12

private fun androidKeystoreSecretKey(keyAlias: String): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
    val spec =
        KeyGenParameterSpec
            .Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
    keyGenerator.init(spec)
    return keyGenerator.generateKey()
}

/**
 * Encrypts/decrypts a single String with an AES-256/GCM key - in production, one kept in
 * AndroidKeyStore, whose key material never leaves the keystore. `encrypt`/`decrypt` both
 * operate on one Base64 string (IV prepended to ciphertext), so the result drops straight into a
 * single DataStore string preference or Room column. Used for both the auth token and PII
 * columns - callers pass a different `keyAlias` for each, isolating blast radius between them.
 *
 * The key lookup is injectable (`internal`) so tests can supply a plain in-memory AES key -
 * `AndroidKeyStore` is a real-device/emulator security provider with no Robolectric shadow, so
 * tests exercise the AES/GCM + IV logic here without going through it.
 */
class AesGcmEncryptor internal constructor(
    private val secretKeyProvider: () -> SecretKey,
) : FieldEncryptor {
    constructor(keyAlias: String) : this({ androidKeystoreSecretKey(keyAlias) })

    override suspend fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeyProvider())
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipher.iv + ciphertext)
    }

    override suspend fun decrypt(value: String): String {
        val bytes = Base64.getDecoder().decode(value)
        val iv = bytes.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = bytes.copyOfRange(GCM_IV_LENGTH_BYTES, bytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKeyProvider(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
