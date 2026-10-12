package ke.don.intellisoft_patient_registration.datasource.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VitalsDaoTest {
    private fun newDatabase(): AppDatabase =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).setDriver(AndroidSQLiteDriver())
            .build()

    private fun vitals(
        patientId: String = "P1",
        visitDate: String = "2025-10-31",
    ) = VitalsEntity(
        id = 0,
        patientId = patientId,
        visitDate = visitDate,
        heightCm = 170.0,
        weightKg = 65.0,
        bmi = 22.5,
        serverId = null,
        synced = false,
    )

    @Test
    fun insertedVitalsIsReturnedForPatient() =
        runTest {
            val db = newDatabase()

            db.vitalsDao().insert(vitals())
            val found = db.vitalsDao().findByPatientId("P1").first()
            db.close()

            assertEquals(1, found.size)
            assertEquals(22.5, found.first().bmi)
        }

    @Test
    fun secondVitalsSubmissionOnSameDateForSamePatientFails() =
        runTest {
            val db = newDatabase()
            db.vitalsDao().insert(vitals())

            assertFailsWith<SQLiteConstraintException> {
                db.vitalsDao().insert(vitals())
            }
            db.close()
        }

    @Test
    fun sameDateIsAllowedForDifferentPatients() =
        runTest {
            val db = newDatabase()

            db.vitalsDao().insert(vitals(patientId = "P1"))
            db.vitalsDao().insert(vitals(patientId = "P2"))
            val forP1 = db.vitalsDao().findByPatientId("P1").first()
            db.close()

            assertEquals(1, forP1.size)
        }

    @Test
    fun markSyncedUpdatesOnlyTheSyncedFlagForThatRow() =
        runTest {
            val db = newDatabase()
            val id = db.vitalsDao().insert(vitals())

            db.vitalsDao().markSynced(id, synced = true)
            val found =
                db
                    .vitalsDao()
                    .findByPatientId("P1")
                    .first()
                    .first()
            db.close()

            assertEquals(true, found.synced)
            assertEquals(22.5, found.bmi)
        }

    @Test
    fun deleteByIdRemovesOnlyThatRow() =
        runTest {
            val db = newDatabase()
            val keptId = db.vitalsDao().insert(vitals(visitDate = "2025-10-30"))
            val removedId = db.vitalsDao().insert(vitals(visitDate = "2025-10-31"))

            db.vitalsDao().deleteById(removedId)
            val remaining = db.vitalsDao().findByPatientId("P1").first()
            db.close()

            assertEquals(listOf(keptId), remaining.map { it.id })
        }

    @Test
    fun findUnsyncedReturnsOnlyUnsyncedRows() =
        runTest {
            val db = newDatabase()
            val syncedId = db.vitalsDao().insert(vitals(visitDate = "2025-10-30"))
            db.vitalsDao().insert(vitals(visitDate = "2025-10-31"))
            db.vitalsDao().markSynced(syncedId, synced = true)

            val unsynced = db.vitalsDao().findUnsynced()
            db.close()

            assertEquals(listOf("2025-10-31"), unsynced.map { it.visitDate })
        }

    @Test
    fun findByVisitDateReturnsOnlyRowsMatchingTheDate() =
        runTest {
            val db = newDatabase()
            db.vitalsDao().insert(vitals(patientId = "P1", visitDate = "2026-10-12"))
            db.vitalsDao().insert(vitals(patientId = "P2", visitDate = "2026-10-12"))
            db.vitalsDao().insert(vitals(patientId = "P1", visitDate = "2026-10-11"))

            val onDate = db.vitalsDao().findByVisitDate("2026-10-12").first()
            db.close()

            assertEquals(2, onDate.size)
            assertEquals(listOf("P1", "P2"), onDate.map { it.patientId }.sorted())
            assertEquals(setOf("2026-10-12"), onDate.map { it.visitDate }.toSet())
        }
}
