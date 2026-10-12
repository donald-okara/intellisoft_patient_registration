package ke.don.intellisoft_patient_registration.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import ke.don.intellisoft_patient_registration.datasource.remote.provideHttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Drives a real MockEngine request through [provideHttpClient] (same pattern as
 * KtorPatientApiTest) so a non-2xx status produces a genuine ClientRequestException - matching
 * what DefaultAuthRepository actually catches in production, rather than a hand-built fake.
 */
private suspend fun throwUnauthorized(): Nothing = throwHttpError(HttpStatusCode.Unauthorized)

private suspend fun throwUnprocessable(): Nothing = throwHttpError(HttpStatusCode.UnprocessableEntity)

private suspend fun throwHttpError(status: HttpStatusCode): Nothing {
    val client = provideHttpClient(MockEngine { respond(content = "{}", status = status) })
    client.get("mock")
    error("MockEngine should have thrown for status $status")
}

class DefaultAuthRepositoryTest {
    @Test
    fun signInWithValidCredentialsSavesTheTokenAndReturnsAuthenticated() =
        runTest {
            val tokenStorage = FakeTokenStorage()
            val authApi = FakeAuthApi()
            val repository = DefaultAuthRepository(authApi, CachedAuthTokenProvider(tokenStorage))

            val result = repository.signIn("jane@example.com", "password123")

            assertEquals(SignInResult.Authenticated, result)
            assertEquals("token-123", tokenStorage.getToken())
        }

    @Test
    fun signInWithWrongCredentialsReturnsInvalidCredentials() =
        runTest {
            val authApi = FakeAuthApi(signinResponse = { throwUnauthorized() })
            val repository = DefaultAuthRepository(authApi, CachedAuthTokenProvider(FakeTokenStorage()))

            assertEquals(SignInResult.InvalidCredentials, repository.signIn("jane@example.com", "wrong"))
        }

    @Test
    fun signUpWithAnAlreadyRegisteredEmailReturnsEmailAlreadyRegistered() =
        runTest {
            val authApi = FakeAuthApi(signupResponse = { throwUnprocessable() })
            val repository = DefaultAuthRepository(authApi, CachedAuthTokenProvider(FakeTokenStorage()))

            val result = repository.signUp("jane@example.com", "Jane", "Mdoe", "password123")

            assertEquals(SignUpResult.EmailAlreadyRegistered, result)
        }

    @Test
    fun signUpOnSuccessReturnsAccountCreatedAndDoesNotTouchTheToken() =
        runTest {
            val tokenStorage = FakeTokenStorage()
            val repository = DefaultAuthRepository(FakeAuthApi(), CachedAuthTokenProvider(tokenStorage))

            val result = repository.signUp("jane@example.com", "Jane", "Mdoe", "password123")

            assertEquals(SignUpResult.AccountCreated, result)
            assertNull(tokenStorage.getToken())
        }
}
