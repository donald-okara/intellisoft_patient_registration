package ke.don.intellisoft_patient_registration.data.sync

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode

/**
 * What a remote push actually needs the caller to do next - not every failure is the same.
 * [Retriable] (timeout/IOException/5xx) is worth another attempt without user involvement.
 * [NeedsAttention] never is: a 401 means the token is dead, a 422 means the server rejected the
 * data - retrying either blindly would just repeat the same failure forever.
 */
sealed interface PushOutcome {
    data object Synced : PushOutcome

    data object Retriable : PushOutcome

    data class NeedsAttention(
        val reason: NeedsAttentionReason,
    ) : PushOutcome
}

/** [ValidationFailed.message] is the server's own rejection reason, not just "it was rejected". */
sealed interface NeedsAttentionReason {
    data object Unauthorized : NeedsAttentionReason

    data class ValidationFailed(
        val message: String,
    ) : NeedsAttentionReason
}

/**
 * Requires the HttpClient passed to [call] to have `expectSuccess = true` (see
 * HttpClientProvider) - that's what makes a non-2xx response throw ClientRequestException/
 * ServerResponseException instead of just deserializing into a success=false body.
 */
suspend fun classifyPush(call: suspend () -> Boolean): PushOutcome =
    try {
        if (call()) PushOutcome.Synced else PushOutcome.Retriable
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.Unauthorized) {
            PushOutcome.NeedsAttention(NeedsAttentionReason.Unauthorized)
        } else {
            // e.message already carries Ktor's cached response text (see ClientRequestException) -
            // re-reading e.response's body here would risk a double-receive on some engines.
            val message = e.message ?: "Validation failed with status ${e.response.status}"
            PushOutcome.NeedsAttention(NeedsAttentionReason.ValidationFailed(message))
        }
    } catch (e: ServerResponseException) {
        PushOutcome.Retriable
    }
