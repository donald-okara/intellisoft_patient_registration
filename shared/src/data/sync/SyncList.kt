package ke.don.intellisoft_patient_registration.data.sync

/**
 * Fetches a remote list and reconciles it into local storage: upserts everything present
 * remotely, deletes local records whose key is no longer present remotely - unless the remote
 * result is empty and [keepLocalWhenRemoteIsEmpty] is set, which guards against wiping local
 * data on a transient empty response (e.g. a flaky connection returning `[]` from
 * `patients/view` shouldn't delete every locally-registered patient).
 *
 * Modeled on the Synchronizer pattern in now-in-android/droidconKeKotlin, simplified for this
 * project's API, which has no change-list/version endpoint - this always does a full reconcile
 * rather than an incremental one.
 */
suspend fun <Remote, Key> syncList(
    remoteFetcher: suspend () -> List<Remote>,
    localKeysFetcher: suspend () -> Set<Key>,
    remoteKeySelector: (Remote) -> Key,
    localUpsert: suspend (List<Remote>) -> Unit,
    localDelete: suspend (Set<Key>) -> Unit,
    keepLocalWhenRemoteIsEmpty: Boolean = false,
): Boolean =
    suspendRunCatching {
        val remoteItems = remoteFetcher()
        if (remoteItems.isEmpty() && keepLocalWhenRemoteIsEmpty) {
            return@suspendRunCatching
        }

        if (remoteItems.isNotEmpty()) {
            localUpsert(remoteItems)
        }

        val remoteKeys = remoteItems.map(remoteKeySelector).toSet()
        val staleKeys = localKeysFetcher() - remoteKeys
        if (staleKeys.isNotEmpty()) {
            localDelete(staleKeys)
        }
    }.isSuccess
