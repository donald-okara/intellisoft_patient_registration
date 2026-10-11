package ke.don.intellisoft_patient_registration.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.io.File
import javax.crypto.KeyGenerator
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Proves the save/get/clear contract and - the part that matters here - that the value sitting
 * in the underlying DataStore file is ciphertext, not the plaintext token.
 */
class EncryptedTokenStorageTest {
    private val dataStoreFile = File.createTempFile("tokens-test", ".preferences_pb")
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(produceFile = { dataStoreFile.absolutePath.toPath() })
    private val inMemoryKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val storage =
        EncryptedTokenStorage(
            dataStore = dataStore,
            encryptor = TokenEncryptor { inMemoryKey },
        )

    @AfterTest
    fun tearDown() {
        dataStoreFile.delete()
    }

    @Test
    fun saveThenGetReturnsTheSameToken() =
        runTest {
            storage.saveToken("super-secret-token")

            assertEquals("super-secret-token", storage.getToken())
        }

    @Test
    fun getReturnsNullWhenNothingSaved() =
        runTest {
            assertNull(storage.getToken())
        }

    @Test
    fun clearRemovesTheToken() =
        runTest {
            storage.saveToken("super-secret-token")
            storage.clearToken()

            assertNull(storage.getToken())
        }

    @Test
    fun rawDataStoreValueIsNotPlaintext() =
        runTest {
            storage.saveToken("super-secret-token")

            val rawValue = dataStore.data.first()[stringPreferencesKey("auth_token")]

            assertFalse(rawValue.isNullOrEmpty())
            assertFalse(rawValue!!.contains("super-secret-token"))
        }
}
