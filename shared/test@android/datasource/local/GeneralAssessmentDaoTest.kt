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
class GeneralAssessmentDaoTest {
    private fun newDatabase(): AppDatabase =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).setDriver(AndroidSQLiteDriver())
            .build()

    private fun assessment(
        patientId: String = "P1",
        visitDate: String = "2025-10-31",
    ) = GeneralAssessmentEntity(
        id = 0,
        patientId = patientId,
        vitalsId = 1,
        visitDate = visitDate,
        generalHealth = "GOOD",
        everDietedToLoseWeight = true,
        comments = "encrypted-comment-placeholder",
        serverId = null,
        synced = false,
    )

    @Test
    fun insertedAssessmentIsReturnedForPatient() =
        runTest {
            val db = newDatabase()

            db.generalAssessmentDao().insert(assessment())
            val found = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(1, found.size)
            assertEquals("GOOD", found.first().generalHealth)
        }

    @Test
    fun secondSubmissionOnSameDateForSamePatientFails() =
        runTest {
            val db = newDatabase()
            db.generalAssessmentDao().insert(assessment())

            assertFailsWith<SQLiteConstraintException> {
                db.generalAssessmentDao().insert(assessment())
            }
            db.close()
        }

    @Test
    fun markSyncedUpdatesOnlyTheSyncedFlagForThatRow() =
        runTest {
            val db = newDatabase()
            val id = db.generalAssessmentDao().insert(assessment())

            db.generalAssessmentDao().markSynced(id, synced = true)
            val found =
                db
                    .generalAssessmentDao()
                    .findByPatientId("P1")
                    .first()
                    .first()
            db.close()

            assertEquals(true, found.synced)
            assertEquals("GOOD", found.generalHealth)
        }

    @Test
    fun deleteByIdRemovesOnlyThatRow() =
        runTest {
            val db = newDatabase()
            val keptId = db.generalAssessmentDao().insert(assessment(visitDate = "2025-10-30"))
            val removedId = db.generalAssessmentDao().insert(assessment(visitDate = "2025-10-31"))

            db.generalAssessmentDao().deleteById(removedId)
            val remaining = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(listOf(keptId), remaining.map { it.id })
        }

    @Test
    fun findUnsyncedReturnsOnlyUnsyncedRows() =
        runTest {
            val db = newDatabase()
            val syncedId = db.generalAssessmentDao().insert(assessment(visitDate = "2025-10-30"))
            db.generalAssessmentDao().insert(assessment(visitDate = "2025-10-31"))
            db.generalAssessmentDao().markSynced(syncedId, synced = true)

            val unsynced = db.generalAssessmentDao().findUnsynced()
            db.close()

            assertEquals(listOf("2025-10-31"), unsynced.map { it.visitDate })
        }
}
