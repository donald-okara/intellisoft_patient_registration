package ke.don.intellisoft_patient_registration.datasource.local

import javax.crypto.KeyGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Proves TokenEncryptor's AES/GCM + IV handling actually round-trips, using an in-memory AES key
 * instead of AndroidKeyStore (a real-device/emulator provider with no Robolectric shadow) - this
 * is the logic that can have real bugs; the keystore itself is Android OS infrastructure.
 */
class TokenEncryptorTest {
    private val inMemoryKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val encryptor = TokenEncryptor { inMemoryKey }

    @Test
    fun decryptReversesEncrypt() {
        val encrypted = encryptor.encrypt("super-secret-token")

        assertEquals("super-secret-token", encryptor.decrypt(encrypted))
    }

    @Test
    fun sameInputEncryptsDifferentlyEachTime() {
        val first = encryptor.encrypt("super-secret-token")
        val second = encryptor.encrypt("super-secret-token")

        assertNotEquals(first, second)
        assertEquals("super-secret-token", encryptor.decrypt(first))
        assertEquals("super-secret-token", encryptor.decrypt(second))
    }

    @Test
    fun encryptedValueDoesNotContainPlaintext() {
        val encrypted = encryptor.encrypt("super-secret-token")

        assertFalse(encrypted.contains("super-secret-token"))
    }
}
