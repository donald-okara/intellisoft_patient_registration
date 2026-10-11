package ke.don.intellisoft_patient_registration.data.sync

/**
 * Fetches a single remote value and writes it to local storage - no reconciliation needed (e.g.
 * caching a single `patients/show/{id}` record). For list results that need
 * upsert/delete-stale-records handling, use [syncList] instead.
 */
suspend fun <T> syncOne(
    remoteFetcher: suspend () -> T,
    localWrite: suspend (T) -> Unit,
): Boolean =
    suspendRunCatching {
        localWrite(remoteFetcher())
    }.isSuccess
