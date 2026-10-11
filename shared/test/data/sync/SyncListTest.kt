package ke.don.intellisoft_patient_registration.data.sync

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private data class RemoteItem(
    val id: String,
    val value: String,
)

class SyncListTest {
    @Test
    fun itemsPresentOnlyRemotelyAreUpserted() =
        runTest {
            val upserted = mutableListOf<RemoteItem>()

            val result =
                syncList(
                    remoteFetcher = { listOf(RemoteItem("1", "a")) },
                    localKeysFetcher = { emptySet() },
                    remoteKeySelector = { it.id },
                    localUpsert = { upserted.addAll(it) },
                    localDelete = { error("should not delete") },
                )

            assertTrue(result)
            assertEquals(listOf(RemoteItem("1", "a")), upserted)
        }

    @Test
    fun itemsPresentOnlyLocallyAreDeleted() =
        runTest {
            val deleted = mutableSetOf<String>()

            val result =
                syncList<RemoteItem, String>(
                    remoteFetcher = { emptyList() },
                    localKeysFetcher = { setOf("stale-1", "stale-2") },
                    remoteKeySelector = { it.id },
                    localUpsert = { error("should not upsert") },
                    localDelete = { deleted.addAll(it) },
                )

            assertTrue(result)
            assertEquals(setOf("stale-1", "stale-2"), deleted)
        }

    @Test
    fun itemsPresentInBothAreUpsertedNotDeleted() =
        runTest {
            val upserted = mutableListOf<RemoteItem>()
            val deleted = mutableSetOf<String>()

            syncList(
                remoteFetcher = { listOf(RemoteItem("1", "updated")) },
                localKeysFetcher = { setOf("1") },
                remoteKeySelector = { it.id },
                localUpsert = { upserted.addAll(it) },
                localDelete = { deleted.addAll(it) },
            )

            assertEquals(listOf(RemoteItem("1", "updated")), upserted)
            assertTrue(deleted.isEmpty())
        }

    @Test
    fun emptyRemoteResultWithKeepLocalTriggersNeitherUpsertNorDelete() =
        runTest {
            var upsertCalled = false
            var deleteCalled = false

            val result =
                syncList<RemoteItem, String>(
                    remoteFetcher = { emptyList() },
                    localKeysFetcher = { setOf("1", "2") },
                    remoteKeySelector = { it.id },
                    localUpsert = { upsertCalled = true },
                    localDelete = { deleteCalled = true },
                    keepLocalWhenRemoteIsEmpty = true,
                )

            assertTrue(result)
            assertFalse(upsertCalled)
            assertFalse(deleteCalled)
        }

    @Test
    fun emptyRemoteResultWithoutKeepLocalDeletesEverything() =
        runTest {
            val deleted = mutableSetOf<String>()

            syncList<RemoteItem, String>(
                remoteFetcher = { emptyList() },
                localKeysFetcher = { setOf("1", "2") },
                remoteKeySelector = { it.id },
                localUpsert = { error("should not upsert") },
                localDelete = { deleted.addAll(it) },
                keepLocalWhenRemoteIsEmpty = false,
            )

            assertEquals(setOf("1", "2"), deleted)
        }

    @Test
    fun throwingRemoteFetcherReturnsFalseAndNeverTouchesLocalWrites() =
        runTest {
            var localTouched = false

            val result =
                syncList<RemoteItem, String>(
                    remoteFetcher = { throw RuntimeException("boom") },
                    localKeysFetcher = { emptySet() },
                    remoteKeySelector = { it.id },
                    localUpsert = { localTouched = true },
                    localDelete = { localTouched = true },
                )

            assertFalse(result)
            assertFalse(localTouched)
        }
}
