package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Not a real feature test. Proves the Ktor client + kotlinx.serialization wiring round-trips
 * JSON and carries the auth header, without a live network call.
 */
class HttpClientProviderSmokeTest {
    @Serializable
    data class Echo(
        val message: String,
    )

    @Test
    fun roundTripsJsonAndSendsBearerToken() =
        runTest {
            var sentAuthHeader: String? = null
            val engine =
                MockEngine { request ->
                    sentAuthHeader = request.headers[HttpHeaders.Authorization]
                    respond(
                        content = """{"message":"hello"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val client = provideHttpClient(engine, AuthTokenProvider { "test-token" })
            val echo: Echo = client.get("patients/view").body()

            assertEquals("hello", echo.message)
            assertEquals("Bearer test-token", sentAuthHeader)
        }
}
