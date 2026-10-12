package ke.don.intellisoft_patient_registration.data.sync

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * offlineFirstWrite is common code, but constructing a real SQLiteException outside Robolectric
 * risks the usual "not mocked" android.jar stub behaviour - same reasoning as PatientDaoTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OfflineWriteResultTest {
    @Test
    fun succeedsAndSyncedWhenThePushIsSynced() =
        runTest {
            var pushSucceededCalled = false

            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = { PushOutcome.Synced },
                    onPushSucceeded = { pushSucceededCalled = true },
                    onNeedsAttention = { error("should not be called") },
                )

            assertEquals(OfflineWriteResult.Success(synced = true), result)
            assertTrue(pushSucceededCalled)
        }

    @Test
    fun succeedsButUnsyncedWhenThePushIsRetriable() =
        runTest {
            var pushSucceededCalled = false

            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = { PushOutcome.Retriable },
                    onPushSucceeded = { pushSucceededCalled = true },
                    onNeedsAttention = { error("should not be called") },
                )

            assertEquals(OfflineWriteResult.Success(synced = false), result)
            assertFalse(pushSucceededCalled)
        }

    @Test
    fun succeedsButUnsyncedWhenThePushThrows() =
        runTest {
            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = { throw RuntimeException("network down") },
                    onPushSucceeded = { error("should not be called") },
                    onNeedsAttention = { error("should not be called") },
                )

            assertEquals(OfflineWriteResult.Success(synced = false), result)
        }

    @Test
    fun needsAttentionPurgesTheLocalRowAndDoesNotMarkSynced() =
        runTest {
            var pushSucceededCalled = false
            var needsAttentionCalled = false

            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = { PushOutcome.NeedsAttention(NeedsAttentionReason.Unauthorized) },
                    onPushSucceeded = { pushSucceededCalled = true },
                    onNeedsAttention = { needsAttentionCalled = true },
                )

            assertEquals(OfflineWriteResult.NeedsAttention(NeedsAttentionReason.Unauthorized), result)
            assertTrue(needsAttentionCalled)
            assertFalse(pushSucceededCalled)
        }

    @Test
    fun conflictWhenTheLocalInsertThrowsSqliteException() =
        runTest {
            val result =
                offlineFirstWrite(
                    localInsert = { throw SQLiteException("UNIQUE constraint failed") },
                    remotePush = { error("should not be called") },
                    onPushSucceeded = { error("should not be called") },
                    onNeedsAttention = { error("should not be called") },
                )

            assertEquals(OfflineWriteResult.Conflict, result)
        }

    @Test
    fun errorWhenTheLocalInsertThrowsAnythingElse() =
        runTest {
            val result =
                offlineFirstWrite(
                    localInsert = { throw RuntimeException("encryption failed") },
                    remotePush = { error("should not be called") },
                    onPushSucceeded = { error("should not be called") },
                    onNeedsAttention = { error("should not be called") },
                )

            assertIs<OfflineWriteResult.Error>(result)
        }

    @Test
    fun succeedsAfterATransientFailureWithinMaxAttempts() =
        runTest {
            var attempts = 0

            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = {
                        attempts++
                        if (attempts < 2) PushOutcome.Retriable else PushOutcome.Synced
                    },
                    onPushSucceeded = {},
                    onNeedsAttention = { error("should not be called") },
                )

            assertEquals(OfflineWriteResult.Success(synced = true), result)
            assertEquals(2, attempts)
        }

    @Test
    fun exhaustsMaxAttemptsThenFallsBackToUnsyncedSuccess() =
        runTest {
            var attempts = 0

            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = {
                        attempts++
                        PushOutcome.Retriable
                    },
                    onPushSucceeded = { error("should not be called") },
                    onNeedsAttention = { error("should not be called") },
                    retryPolicy = RetryPolicy(maxAttempts = 3, initialDelayMs = 1_000, factor = 2.0),
                )

            assertEquals(OfflineWriteResult.Success(synced = false), result)
            assertEquals(3, attempts)
        }

    @Test
    fun waitsWithExponentialBackoffBetweenRetries() =
        runTest {
            offlineFirstWrite(
                localInsert = {},
                remotePush = { PushOutcome.Retriable },
                onPushSucceeded = { error("should not be called") },
                onNeedsAttention = { error("should not be called") },
                retryPolicy = RetryPolicy(maxAttempts = 3, initialDelayMs = 1_000, factor = 2.0, maxDelayMs = 10_000),
            )

            // 1s before the 2nd attempt, 2s before the 3rd - no delay after the last attempt.
            assertEquals(3_000, currentTime)
        }

    @Test
    fun backoffDelayIsCappedAtMaxDelayMs() =
        runTest {
            offlineFirstWrite(
                localInsert = {},
                remotePush = { PushOutcome.Retriable },
                onPushSucceeded = { error("should not be called") },
                onNeedsAttention = { error("should not be called") },
                retryPolicy = RetryPolicy(maxAttempts = 4, initialDelayMs = 1_000, factor = 10.0, maxDelayMs = 5_000),
            )

            // Uncapped would be 1s + 10s + 100s; capped it's 1s + 5s + 5s.
            assertEquals(11_000, currentTime)
        }

    @Test
    fun stopsRetryingImmediatelyOnceNeedsAttention() =
        runTest {
            var attempts = 0

            offlineFirstWrite(
                localInsert = {},
                remotePush = {
                    attempts++
                    PushOutcome.NeedsAttention(NeedsAttentionReason.Unauthorized)
                },
                onPushSucceeded = { error("should not be called") },
                onNeedsAttention = {},
                retryPolicy = RetryPolicy(maxAttempts = 3),
            )

            assertEquals(1, attempts)
        }
}
