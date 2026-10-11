package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val API_BASE_URL = "https://patientvisitapis.intellisoftkenya.com/api/"

expect fun platformHttpClientEngine(): HttpClientEngine

/** Not wired to real auth storage yet - that lands with the repository layer next phase. */
fun interface AuthTokenProvider {
    fun currentToken(): String?
}

fun provideHttpClient(
    engine: HttpClientEngine,
    authTokenProvider: AuthTokenProvider = AuthTokenProvider { null },
): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            url(API_BASE_URL)
            accept(ContentType.Application.Json)
            authTokenProvider.currentToken()?.let { token ->
                header("Authorization", "Bearer $token")
            }
        }
    }
