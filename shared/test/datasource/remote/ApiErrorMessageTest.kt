package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Drives a real MockEngine request through provideHttpClient (same pattern as
 * DefaultAuthRepositoryTest) so these exercise a genuine Ktor response exception - the exact
 * `message` format apiErrorMessage() parses, rather than a hand-built string. Catches the common
 * ResponseException parent since a 5xx throws ServerResponseException, not ClientRequestException.
 */
private suspend fun responseExceptionFor(
    status: HttpStatusCode,
    body: String,
): Throwable {
    val client = provideHttpClient(MockEngine { respond(content = body, status = status) })
    return try {
        client.get("mock")
        error("MockEngine should have thrown for status $status")
    } catch (e: ResponseException) {
        e
    }
}

class ApiErrorMessageTest {
    @Test
    fun recoversTheServersOwnMessageFromAJsonErrorBody() =
        runTest {
            val exception =
                responseExceptionFor(
                    HttpStatusCode.Unauthorized,
                    """{"message":"Invalid email or password","success":false,"code":401}""",
                )

            assertEquals("Invalid email or password", exception.apiErrorMessage())
        }

    @Test
    fun isNullWhenTheBodyHasNoMessageField() =
        runTest {
            val exception = responseExceptionFor(HttpStatusCode.UnprocessableEntity, "{}")

            assertNull(exception.apiErrorMessage())
        }

    @Test
    fun isNullWhenTheBodyIsNotJson() =
        runTest {
            val exception = responseExceptionFor(HttpStatusCode.InternalServerError, "<html>502 Bad Gateway</html>")

            assertNull(exception.apiErrorMessage())
        }

    @Test
    fun isNullForAThrowableThatIsNotAResponseException() {
        assertNull(RuntimeException("boom").apiErrorMessage())
    }
}
