package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage

class FakeTokenStorage(
    private var token: String? = null,
) : TokenStorage {
    override suspend fun saveToken(token: String) {
        this.token = token
    }

    override suspend fun getToken(): String? = token

    override suspend fun clearToken() {
        token = null
    }
}
