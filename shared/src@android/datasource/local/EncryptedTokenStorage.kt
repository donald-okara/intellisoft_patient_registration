package ke.don.intellisoft_patient_registration.datasource.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/** Encrypts the token with [encryptor] before it ever reaches [dataStore], and decrypts on read. */
class EncryptedTokenStorage(
    private val dataStore: DataStore<Preferences>,
    private val encryptor: TokenEncryptor,
) : TokenStorage {
    private val tokenKey = stringPreferencesKey("auth_token")

    override suspend fun saveToken(token: String) {
        val encrypted = encryptor.encrypt(token)
        dataStore.edit { it[tokenKey] = encrypted }
    }

    override suspend fun getToken(): String? {
        val encrypted = dataStore.data.first()[tokenKey] ?: return null
        return encryptor.decrypt(encrypted)
    }

    override suspend fun clearToken() {
        dataStore.edit { it.remove(tokenKey) }
    }
}
