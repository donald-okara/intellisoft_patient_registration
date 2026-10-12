package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Mirrors the `message` field of the `ApiResponse` envelope (see
 * datasource.remote.dto.ApiResponse) - kept as its own type because an error body never carries
 * the `data` field `ApiResponse` requires, so it can't be reused directly for errors.
 */
@Serializable
private data class ApiErrorBodyDto(
    val message: String,
)

private val lenientJson = Json { ignoreUnknownKeys = true }

private const val RESPONSE_TEXT_MARKER = "Text: \""

/**
 * The server's own error message, if [this] is a Ktor response exception whose body was valid
 * JSON with a `message` field - null for anything else (a plain IOException/timeout, a non-JSON
 * error page, a body missing that field), so callers fall back to a generic string instead of
 * showing nothing or a confusing raw exception.
 *
 * Reads the exception's own formatted `message` rather than re-reading `response.bodyAsText()` -
 * HttpClientProvider's `expectSuccess = true` means Ktor's default validator already consumed the
 * response body once to build that message, and re-reading the channel a second time isn't safe
 * on every engine (see classifyPush). Ktor embeds the exact, unescaped response text verbatim
 * after `Text: "` up to the final quote (ResponseException/ClientRequestException/
 * ServerResponseException in ktor-client-core's DefaultResponseValidation.kt), so extracting that
 * substring recovers the original JSON body without a second read.
 */
fun Throwable.apiErrorMessage(): String? {
    if (this !is ResponseException) return null
    val text = message ?: return null
    val markerIndex = text.indexOf(RESPONSE_TEXT_MARKER)
    if (markerIndex == -1) return null
    val bodyStart = markerIndex + RESPONSE_TEXT_MARKER.length
    if (bodyStart >= text.length) return null
    val body = text.substring(bodyStart, text.length - 1)
    return runCatching { lenientJson.decodeFromString<ApiErrorBodyDto>(body).message }.getOrNull()
}
