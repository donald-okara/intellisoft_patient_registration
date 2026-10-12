package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val API_BASE_URL = "https://patientvisitapis.intellisoftkenya.com/api/"

expect fun platformHttpClientEngine(): HttpClientEngine

/**
 * Android's actual logs straight to Logcat (see HttpClientProvider.android.kt) - `Logger.DEFAULT`
 * alone resolves to an SLF4J logger, which is a silent no-op here since no SLF4J binding is on
 * the classpath, so every push would otherwise log and show nothing.
 */
expect fun platformPushLogger(): Logger

/** Backed by CachedAuthTokenProvider in production - see authModule/httpClientModule. */
fun interface AuthTokenProvider {
    fun currentToken(): String?
}

fun provideHttpClient(
    engine: HttpClientEngine,
    authTokenProvider: AuthTokenProvider = AuthTokenProvider { null },
): HttpClient =
    HttpClient(engine) {
        // Makes a non-2xx response throw ClientRequestException/ServerResponseException - see data.sync.classifyPush.
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        install(Logging) {
            logger = platformPushLogger()
            level = LogLevel.INFO
            // "All API pushes" means writes, not every read (patients/view, visits/view) -
            // LogLevel.INFO only logs method/url/status anyway, never headers or the request/
            // response body, so no PII (name, DOB, etc.) ever reaches the log.
            filter { request -> request.method != HttpMethod.Get }
        }
        defaultRequest {
            url(API_BASE_URL)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            authTokenProvider.currentToken()?.let { token ->
                header("Authorization", "Bearer $token")
            }
        }
    }
