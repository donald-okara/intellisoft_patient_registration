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
class OverweightAssessmentDaoTest {
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
    ) = OverweightAssessmentEntity(
        id = 0,
        patientId = patientId,
        vitalsId = 1,
        visitDate = visitDate,
        generalHealth = "POOR",
        currentlyUsingDrugs = false,
        comments = "encrypted-comment-placeholder",
        serverId = null,
        synced = false,
    )

    @Test
    fun insertedAssessmentIsReturnedForPatient() =
        runTest {
            val db = newDatabase()

            db.overweightAssessmentDao().insert(assessment())
            val found = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(1, found.size)
            assertEquals("POOR", found.first().generalHealth)
        }

    @Test
    fun secondSubmissionOnSameDateForSamePatientFails() =
        runTest {
            val db = newDatabase()
            db.overweightAssessmentDao().insert(assessment())

            assertFailsWith<SQLiteConstraintException> {
                db.overweightAssessmentDao().insert(assessment())
            }
            db.close()
        }
}
