package ke.don.intellisoft_patient_registration.data.sync

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncOneTest {
    @Test
    fun fetchedValueIsPassedToLocalWrite() =
        runTest {
            var written: String? = null

            val result =
                syncOne(
                    remoteFetcher = { "fetched-value" },
                    localWrite = { written = it },
                )

            assertTrue(result)
            assertEquals("fetched-value", written)
        }

    @Test
    fun throwingRemoteFetcherReturnsFalseAndNeverCallsLocalWrite() =
        runTest {
            var called = false

            val result =
                syncOne(
                    remoteFetcher = { throw RuntimeException("boom") },
                    localWrite = { called = true },
                )

            assertFalse(result)
            assertFalse(called)
        }
}
