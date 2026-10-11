package ke.don.intellisoft_patient_registration.datasource.local

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.cinterop.CFTypeRefVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Field-level AES-GCM encryption for iOS. Keychain alone isn't the right fit for per-column
 * ciphertext across many patient rows (it's built for a handful of named secrets, see
 * KeychainTokenStorage for where it's used directly instead) - so this generates a random AES
 * key once, stores *that* in Keychain (a single entry), and uses `cryptography-kotlin`
 * (CryptoKit-backed via `cryptography-provider-optimal`) for the actual field encryption, rather
 * than hand-written Kotlin/Native cinterop to CommonCrypto.
 *
 * Unverified by compilation or a real run - no Xcode on the machine this was written on.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
class AppleFieldEncryptor(
    private val keyAccount: String,
) : FieldEncryptor {
    private val aesGcm = CryptographyProvider.Default.get(AES.GCM)

    private suspend fun secretKey(): AES.GCM.Key {
        readKeyFromKeychain()?.let { raw ->
            return aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, raw)
        }
        val generated = aesGcm.keyGenerator().generateKey()
        writeKeyToKeychain(generated.encodeToByteArray(AES.Key.Format.RAW))
        return generated
    }

    override suspend fun encrypt(plainText: String): String {
        val ciphertext = secretKey().cipher().encrypt(plainText.encodeToByteArray())
        return Base64.encode(ciphertext)
    }

    override suspend fun decrypt(value: String): String {
        val plaintext = secretKey().cipher().decrypt(Base64.decode(value))
        return plaintext.decodeToString()
    }

    private fun query(): NSMutableDictionary =
        NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass)
            setObject("ke.don.intellisoft_patient_registration.fieldkey", forKey = kSecAttrService)
            setObject(keyAccount, forKey = kSecAttrAccount)
        }

    private fun readKeyFromKeychain(): ByteArray? {
        val searchQuery =
            query().apply {
                setObject(true, forKey = kSecReturnData)
                setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)
            }

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(searchQuery, result.ptr)
            if (status != errSecSuccess) return@memScoped null

            (result.value as? NSData)?.toByteArray()
        }
    }

    private fun writeKeyToKeychain(raw: ByteArray) {
        SecItemDelete(query())
        val addQuery = query().apply { setObject(raw.toNSData(), forKey = kSecValueData) }
        SecItemAdd(addQuery, null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    memScoped {
        NSData.create(bytes = allocArrayOf(this@toNSData), length = size.toULong())
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) {
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length)
        }
    }
    return result
}
