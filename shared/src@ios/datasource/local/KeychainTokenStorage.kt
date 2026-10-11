package ke.don.intellisoft_patient_registration.datasource.local

import kotlinx.cinterop.CFTypeRefVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
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

/**
 * iOS has no AndroidKeyStore equivalent, and Keychain already encrypts its contents at rest -
 * so unlike Android (DataStore + manual AES/GCM via TokenEncryptor), this stores the token
 * directly in Keychain rather than combining it with DataStore. No real device/Xcode is
 * available where this was written, so this is unverified by compilation or a real run - it
 * follows the standard SecItemAdd/SecItemCopyMatching/SecItemDelete pattern for a generic
 * password keychain item.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainTokenStorage(
    private val service: String = "ke.don.intellisoft_patient_registration.auth",
    private val account: String = "access_token",
) : TokenStorage {
    private fun query(): NSMutableDictionary =
        NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass)
            setObject(service, forKey = kSecAttrService)
            setObject(account, forKey = kSecAttrAccount)
        }

    override suspend fun saveToken(token: String) {
        val data =
            (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                ?: error("Failed to encode token for Keychain storage")

        SecItemDelete(query())

        val addQuery = query().apply { setObject(data, forKey = kSecValueData) }
        val status = SecItemAdd(addQuery, null)
        check(status == errSecSuccess) { "Keychain save failed with status $status" }
    }

    override suspend fun getToken(): String? {
        val searchQuery =
            query().apply {
                setObject(true, forKey = kSecReturnData)
                setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)
            }

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(searchQuery, result.ptr)
            if (status != errSecSuccess) return@memScoped null

            val data = result.value as? NSData ?: return@memScoped null
            NSString.create(data, NSUTF8StringEncoding) as String?
        }
    }

    override suspend fun clearToken() {
        SecItemDelete(query())
    }
}
