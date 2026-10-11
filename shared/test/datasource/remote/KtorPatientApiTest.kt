package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import ke.don.intellisoft_patient_registration.datasource.remote.dto.RegisterPatientRequestDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Not exhaustive per-endpoint coverage - this is the representative proof that the API client
 * pattern (base URL + relative path + typed request/response) works against a real MockEngine
 * request/response cycle. AuthApi/VitalsApi/VisitApi follow the same mechanical shape and get
 * proven indirectly once their repositories exist, same as PatientApi is proven here ahead of
 * PatientRepository.
 */
class KtorPatientApiTest {
    @Test
    fun registerPostsToPatientsRegisterWithTheRequestBody() =
        runTest {
            var requestedUrl: String? = null
            var requestedMethod: HttpMethod? = null

            val engine =
                MockEngine { request ->
                    requestedUrl = request.url.encodedPath
                    requestedMethod = request.method
                    respond(
                        content =
                            """{"message":"success","success":true,"code":200,"data":{"proceed":0,"message":"ok"}}""",
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                io.ktor.http.HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                }

            val api = KtorPatientApi(provideHttpClient(engine))
            val response =
                api.register(
                    RegisterPatientRequestDto(
                        firstname = "Jane",
                        lastname = "Doe",
                        unique = "P1",
                        dob = "2000-01-01",
                        gender = "Female",
                        regDate = "2025-10-31",
                    ),
                )

            assertEquals("/api/patients/register", requestedUrl)
            assertEquals(HttpMethod.Post, requestedMethod)
            assertTrue(response.success)
            assertEquals("ok", response.data.message)
        }

    @Test
    fun listGetsPatientsView() =
        runTest {
            var requestedUrl: String? = null
            var requestedMethod: HttpMethod? = null

            val engine =
                MockEngine { request ->
                    requestedUrl = request.url.encodedPath
                    requestedMethod = request.method
                    respond(
                        content = """{"message":"success","success":true,"code":200,"data":[]}""",
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                io.ktor.http.HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                }

            val api = KtorPatientApi(provideHttpClient(engine))
            api.list()

            assertEquals("/api/patients/view", requestedUrl)
            assertEquals(HttpMethod.Get, requestedMethod)
        }

    @Test
    fun showGetsPatientsShowWithId() =
        runTest {
            var requestedUrl: String? = null

            val engine =
                MockEngine { request ->
                    requestedUrl = request.url.encodedPath
                    respond(
                        content = """{"message":"success","success":true,"code":200,"data":[]}""",
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                io.ktor.http.HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                }

            val api = KtorPatientApi(provideHttpClient(engine))
            api.show(7)

            assertEquals("/api/patients/show/7", requestedUrl)
        }
}
