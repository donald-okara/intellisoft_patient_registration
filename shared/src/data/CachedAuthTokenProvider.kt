package ke.don.intellisoft_patient_registration.data

import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import ke.don.intellisoft_patient_registration.datasource.remote.AuthTokenProvider
import kotlin.concurrent.Volatile

/**
 * Bridges TokenStorage (suspend) to AuthTokenProvider (sync - called inside Ktor's non-suspend
 * `defaultRequest {}`) with an in-memory cache. `setToken`/`clearToken` are the only places that
 * touch TokenStorage's suspend API; `currentToken()` just reads the cache.
 */
class CachedAuthTokenProvider(
    private val tokenStorage: TokenStorage,
) : AuthTokenProvider {
    @Volatile
    private var cachedToken: String? = null

    override fun currentToken(): String? = cachedToken

    suspend fun setToken(token: String) {
        tokenStorage.saveToken(token)
        cachedToken = token
    }

    suspend fun clearToken() {
        tokenStorage.clearToken()
        cachedToken = null
    }

    /** Call once at startup to restore a previously-saved token into the cache. */
    suspend fun hydrate() {
        cachedToken = tokenStorage.getToken()
    }
}
