package ke.don.intellisoft_patient_registration.data.sync

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Local outcome of an offline-first write. [Success.synced] covers both a confirmed push and a
 * [PushOutcome.Retriable] failure (left as unsynced for a later retry - no persisted state
 * needed beyond that, since the local row already says so). [NeedsAttention] never gets to that
 * point: see [offlineFirstWrite]'s [NeedsAttentionReason] handling.
 */
sealed interface OfflineWriteResult {
    data class Success(
        val synced: Boolean,
    ) : OfflineWriteResult

    /** The local write was rejected by a UNIQUE constraint - "already registered"/"already submitted today". */
    data object Conflict : OfflineWriteResult

    /** The push will never succeed by retrying (401/422) - the local row has already been purged. */
    data class NeedsAttention(
        val reason: NeedsAttentionReason,
    ) : OfflineWriteResult

    /** Anything unexpected (e.g. a PII-encryption failure) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : OfflineWriteResult
}

/**
 * Shared shape for every registration/submission repository: write to Room first (whose UNIQUE
 * constraint is what actually enforces the business rule), then attempt a best-effort remote
 * push with exponential backoff on a transient failure - matches "write to Room first, don't
 * block the local save on network success" (a push still unsynced after [retryPolicy] is
 * exhausted falls back to [OfflineWriteResult.Success] with `synced = false`, same as before
 * this retried at all - the periodic retry worker picks it up later).
 *
 * A [PushOutcome.NeedsAttention] purges the local row via [onNeedsAttention] rather than leaving
 * it stuck - a 401/422 will never succeed by retrying (so it's never retried here either), and
 * leaving the row behind would permanently collide with the same uniqueness constraint that
 * detects duplicates, blocking the only real recovery path (the user resubmits from scratch).
 */
suspend fun offlineFirstWrite(
    localInsert: suspend () -> Unit,
    remotePush: suspend () -> PushOutcome,
    onPushSucceeded: suspend () -> Unit,
    onNeedsAttention: suspend () -> Unit,
    retryPolicy: RetryPolicy = RetryPolicy(),
): OfflineWriteResult =
    try {
        localInsert()

        when (val outcome = pushWithBackoff(remotePush, retryPolicy)) {
            PushOutcome.Synced -> {
                onPushSucceeded()
                OfflineWriteResult.Success(synced = true)
            }

            PushOutcome.Retriable -> {
                OfflineWriteResult.Success(synced = false)
            }

            is PushOutcome.NeedsAttention -> {
                onNeedsAttention()
                OfflineWriteResult.NeedsAttention(outcome.reason)
            }
        }
    } catch (e: SQLiteException) {
        OfflineWriteResult.Conflict
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        OfflineWriteResult.Error(e)
    }

private suspend fun pushWithBackoff(
    remotePush: suspend () -> PushOutcome,
    retryPolicy: RetryPolicy,
): PushOutcome {
    repeat(retryPolicy.maxAttempts) { attempt ->
        val outcome = suspendRunCatching(remotePush).getOrDefault(PushOutcome.Retriable)
        if (outcome != PushOutcome.Retriable) return outcome
        if (attempt < retryPolicy.maxAttempts - 1) {
            delay(retryPolicy.delayForAttempt(attempt).milliseconds)
        }
    }
    return PushOutcome.Retriable
}
