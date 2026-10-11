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
            val found = db.vitalsDao().findByPatientId("P1")
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
            val countForP1 = db.vitalsDao().findByPatientId("P1").size
            db.close()

            assertEquals(1, countForP1)
        }
}
