package ke.don.intellisoft_patient_registration.data.sync

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException

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
 * constraint is what actually enforces the business rule), then attempt one best-effort remote
 * push - matches "write to Room first, don't block the local save on network success".
 *
 * A [PushOutcome.NeedsAttention] purges the local row via [onNeedsAttention] rather than leaving
 * it stuck - a 401/422 will never succeed by retrying, and leaving the row behind would
 * permanently collide with the same uniqueness constraint that detects duplicates, blocking the
 * only real recovery path (the user resubmits from scratch).
 */
suspend fun offlineFirstWrite(
    localInsert: suspend () -> Unit,
    remotePush: suspend () -> PushOutcome,
    onPushSucceeded: suspend () -> Unit,
    onNeedsAttention: suspend () -> Unit,
): OfflineWriteResult =
    try {
        localInsert()

        when (val outcome = suspendRunCatching(remotePush).getOrDefault(PushOutcome.Retriable)) {
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
