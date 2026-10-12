package ke.don.intellisoft_patient_registration.data.sync

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import ke.don.intellisoft_patient_registration.datasource.remote.provideHttpClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * classifyPush only interprets the two Ktor exceptions expectSuccess=true throws for non-2xx
 * responses - a plain IOException/timeout is deliberately left uncaught here and classified as
 * Retriable one layer up, by offlineFirstWrite's suspendRunCatching (see OfflineWriteResultTest).
 */
class PushOutcomeTest {
    private fun clientRespondingWith(status: HttpStatusCode) =
        provideHttpClient(MockEngine { respond(content = "{}", status = status) })

    @Test
    fun aSuccessfulResponseAndTrueResultIsSynced() =
        runTest {
            val client = clientRespondingWith(HttpStatusCode.OK)

            val outcome = classifyPush { client.get("x").status == HttpStatusCode.OK }

            assertEquals(PushOutcome.Synced, outcome)
        }

    @Test
    fun aSuccessfulResponseButFalseResultIsRetriable() =
        runTest {
            val outcome = classifyPush { false }

            assertEquals(PushOutcome.Retriable, outcome)
        }

    @Test
    fun aServerErrorResponseIsRetriable() =
        runTest {
            val client = clientRespondingWith(HttpStatusCode.InternalServerError)

            val outcome = classifyPush { client.get("x").status == HttpStatusCode.OK }

            assertEquals(PushOutcome.Retriable, outcome)
        }

    @Test
    fun anUnauthorizedResponseNeedsAttention() =
        runTest {
            val client = clientRespondingWith(HttpStatusCode.Unauthorized)

            val outcome = classifyPush { client.get("x").status == HttpStatusCode.OK }

            assertEquals(PushOutcome.NeedsAttention(NeedsAttentionReason.Unauthorized), outcome)
        }

    @Test
    fun anyOtherClientErrorResponseNeedsAttentionAsValidationFailed() =
        runTest {
            val client = clientRespondingWith(HttpStatusCode.UnprocessableEntity)

            val outcome = classifyPush { client.get("x").status == HttpStatusCode.OK }

            val reason = assertIs<PushOutcome.NeedsAttention>(outcome).reason
            val validationFailure = assertIs<NeedsAttentionReason.ValidationFailed>(reason)
            assertTrue(validationFailure.message.contains("422"))
        }
}
