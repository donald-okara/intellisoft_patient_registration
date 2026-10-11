package ke.don.intellisoft_patient_registration.data.sync

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException

/**
 * Local outcome of an offline-first write - the push outcome is folded into [Success.synced]
 * rather than split out, because nothing yet distinguishes *why* a push failed (network, 401,
 * 422, 5xx all land the same way) or retries it - there's no outbox. That's a deliberate,
 * tracked gap (see DESIGN.md): a real error taxonomy only pays for itself once something acts
 * differently per category, and today nothing does.
 */
sealed interface OfflineWriteResult {
    data class Success(
        val synced: Boolean,
    ) : OfflineWriteResult

    /** The local write was rejected by a UNIQUE constraint - "already registered"/"already submitted today". */
    data object Conflict : OfflineWriteResult

    /** Anything unexpected (e.g. a PII-encryption failure) - never thrown out raw. */
    data class Error(
        val cause: Throwable,
    ) : OfflineWriteResult
}

/**
 * Shared shape for every registration/submission repository: write to Room first (whose UNIQUE
 * constraint is what actually enforces the business rule), then attempt one best-effort remote
 * push - matches "write to Room first, don't block the local save on network success".
 */
suspend fun offlineFirstWrite(
    localInsert: suspend () -> Unit,
    remotePush: suspend () -> Boolean,
    onPushSucceeded: suspend () -> Unit,
): OfflineWriteResult =
    try {
        localInsert()

        val synced = suspendRunCatching(remotePush).getOrDefault(false)
        if (synced) {
            onPushSucceeded()
        }

        OfflineWriteResult.Success(synced)
    } catch (e: SQLiteException) {
        OfflineWriteResult.Conflict
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        OfflineWriteResult.Error(e)
    }
