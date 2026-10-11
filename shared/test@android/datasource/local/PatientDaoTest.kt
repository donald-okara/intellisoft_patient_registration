package ke.don.intellisoft_patient_registration.datasource.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PatientDaoTest {
    private fun newDatabase(): AppDatabase =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).setDriver(AndroidSQLiteDriver())
            .build()

    private fun patient(
        patientId: String = "P1",
        registrationDate: String = "2025-10-31",
    ) = PatientEntity(
        patientId = patientId,
        serverId = null,
        registrationDate = registrationDate,
        firstName = "Jane",
        lastName = "Doe",
        dateOfBirth = "2000-01-01",
        gender = "FEMALE",
        synced = false,
    )

    @Test
    fun insertedPatientIsReturnedById() =
        runTest {
            val db = newDatabase()

            db.patientDao().insert(patient())
            val found = db.patientDao().findByPatientId("P1")
            db.close()

            assertEquals("Jane", found?.firstName)
        }

    @Test
    fun missingPatientIdReturnsNull() =
        runTest {
            val db = newDatabase()

            val found = db.patientDao().findByPatientId("missing")
            db.close()

            assertNull(found)
        }

    @Test
    fun duplicatePatientIdInsertFails() =
        runTest {
            val db = newDatabase()
            db.patientDao().insert(patient())

            assertFailsWith<SQLiteConstraintException> {
                db.patientDao().insert(patient())
            }
            db.close()
        }

    @Test
    fun upsertReplacesAnExistingRowInsteadOfFailing() =
        runTest {
            val db = newDatabase()
            db.patientDao().insert(patient())

            db.patientDao().upsert(listOf(patient().copy(firstName = "Updated")))
            val found = db.patientDao().findByPatientId("P1")
            db.close()

            assertEquals("Updated", found?.firstName)
        }

    @Test
    fun markSyncedUpdatesOnlyTheSyncedFlag() =
        runTest {
            val db = newDatabase()
            db.patientDao().insert(patient())

            db.patientDao().markSynced("P1", synced = true)
            val found = db.patientDao().findByPatientId("P1")
            db.close()

            assertEquals(true, found?.synced)
            assertEquals("Jane", found?.firstName)
        }

    @Test
    fun deleteByPatientIdsRemovesOnlyTheGivenIds() =
        runTest {
            val db = newDatabase()
            db.patientDao().insert(patient("P1"))
            db.patientDao().insert(patient("P2"))

            db.patientDao().deleteByPatientIds(setOf("P1"))
            val remaining = db.patientDao().findAll()
            db.close()

            assertEquals(listOf("P2"), remaining.map { it.patientId })
        }

    @Test
    fun findPageOrdersMostRecentlyRegisteredFirst() =
        runTest {
            val db = newDatabase()
            db.patientDao().insert(patient("P1", registrationDate = "2025-10-01"))
            db.patientDao().insert(patient("P2", registrationDate = "2025-10-31"))
            db.patientDao().insert(patient("P3", registrationDate = "2025-10-15"))

            val page = db.patientDao().findPage(limit = 10, offset = 0)
            db.close()

            assertEquals(listOf("P2", "P3", "P1"), page.map { it.patientId })
        }

    @Test
    fun findPageRespectsLimitAndOffset() =
        runTest {
            val db = newDatabase()
            db.patientDao().insert(patient("P1", registrationDate = "2025-10-01"))
            db.patientDao().insert(patient("P2", registrationDate = "2025-10-31"))
            db.patientDao().insert(patient("P3", registrationDate = "2025-10-15"))

            val secondPage = db.patientDao().findPage(limit = 2, offset = 2)
            db.close()

            assertEquals(listOf("P1"), secondPage.map { it.patientId })
        }

    @Test
    fun findUnsyncedReturnsOnlyUnsyncedRows() =
        runTest {
            val db = newDatabase()
            db.patientDao().insert(patient("P1"))
            db.patientDao().insert(patient("P2"))
            db.patientDao().markSynced("P2", synced = true)

            val unsynced = db.patientDao().findUnsynced()
            db.close()

            assertEquals(listOf("P1"), unsynced.map { it.patientId })
        }
}
