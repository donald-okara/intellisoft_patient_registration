package ke.don.intellisoft_patient_registration.datasource.local

/**
 * Secure storage for the API auth token. Implementations are platform-specific, using whatever
 * that platform's idiomatic secure-storage primitive is - there's no `expect`/`actual` here
 * because nothing in common code constructs an implementation by name; each platform's DI wiring
 * provides its own.
 */
interface TokenStorage {
    suspend fun saveToken(token: String)

    suspend fun getToken(): String?

    suspend fun clearToken()
}
