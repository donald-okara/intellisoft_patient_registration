package ke.don.intellisoft_patient_registration.data.sync

import androidx.sqlite.SQLiteException
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
    fun succeedsAndSyncedWhenBothWritesSucceed() =
        runTest {
            var pushSucceededCalled = false

            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = { true },
                    onPushSucceeded = { pushSucceededCalled = true },
                )

            assertEquals(OfflineWriteResult.Success(synced = true), result)
            assertTrue(pushSucceededCalled)
        }

    @Test
    fun succeedsButUnsyncedWhenThePushReturnsFalse() =
        runTest {
            var pushSucceededCalled = false

            val result =
                offlineFirstWrite(
                    localInsert = {},
                    remotePush = { false },
                    onPushSucceeded = { pushSucceededCalled = true },
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
                )

            assertEquals(OfflineWriteResult.Success(synced = false), result)
        }

    @Test
    fun conflictWhenTheLocalInsertThrowsSqliteException() =
        runTest {
            val result =
                offlineFirstWrite(
                    localInsert = { throw SQLiteException("UNIQUE constraint failed") },
                    remotePush = { error("should not be called") },
                    onPushSucceeded = { error("should not be called") },
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
                )

            assertIs<OfflineWriteResult.Error>(result)
        }
}
