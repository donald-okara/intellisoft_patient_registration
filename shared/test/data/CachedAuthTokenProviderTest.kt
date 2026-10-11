package ke.don.intellisoft_patient_registration.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CachedAuthTokenProviderTest {
    @Test
    fun setTokenMakesCurrentTokenReturnItImmediately() =
        runTest {
            val provider = CachedAuthTokenProvider(FakeTokenStorage())

            provider.setToken("abc123")

            assertEquals("abc123", provider.currentToken())
        }

    @Test
    fun setTokenPersistsToTokenStorage() =
        runTest {
            val storage = FakeTokenStorage()
            val provider = CachedAuthTokenProvider(storage)

            provider.setToken("abc123")

            assertEquals("abc123", storage.getToken())
        }

    @Test
    fun clearTokenClearsBothCacheAndStorage() =
        runTest {
            val storage = FakeTokenStorage()
            val provider = CachedAuthTokenProvider(storage)
            provider.setToken("abc123")

            provider.clearToken()

            assertNull(provider.currentToken())
            assertNull(storage.getToken())
        }

    @Test
    fun hydrateLoadsAnExistingTokenFromStorageIntoTheCache() =
        runTest {
            val storage = FakeTokenStorage(token = "already-saved")
            val provider = CachedAuthTokenProvider(storage)

            provider.hydrate()

            assertEquals("already-saved", provider.currentToken())
        }

    @Test
    fun currentTokenIsNullBeforeAnythingIsSetOrHydrated() {
        val provider = CachedAuthTokenProvider(FakeTokenStorage())

        assertNull(provider.currentToken())
    }
}
