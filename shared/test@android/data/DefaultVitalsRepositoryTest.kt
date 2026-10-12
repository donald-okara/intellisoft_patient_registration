package ke.don.intellisoft_patient_registration.data

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import ke.don.intellisoft_patient_registration.datasource.local.VitalsDao
import ke.don.intellisoft_patient_registration.datasource.local.VitalsEntity
import ke.don.intellisoft_patient_registration.datasource.remote.KtorVitalsApi
import ke.don.intellisoft_patient_registration.datasource.remote.VitalsApi
import ke.don.intellisoft_patient_registration.datasource.remote.provideHttpClient
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * This is the Vitals equivalent of DefaultPatientRepositoryTest's required "repository test using
 * a faked API, not a live network call" - FakeVitalsApi stands in for the network, a real
 * in-memory Room database (Robolectric, same pattern as the DAO tests) stands in for local
 * storage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DefaultVitalsRepositoryTest {
    /** Only used to force OfflineWriteResult.Error - a real Room DAO has no such failure mode to fake otherwise. */
    private class ThrowingVitalsDao(
        private val delegate: VitalsDao,
    ) : VitalsDao by delegate {
        override suspend fun insert(vitals: VitalsEntity): Long = throw RuntimeException("disk full")
    }

    private fun newRepository(
        api: VitalsApi = FakeVitalsApi(),
        dao: (VitalsDao) -> VitalsDao = { it },
    ): Pair<DefaultVitalsRepository, AppDatabase> {
        val db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .build()
        return DefaultVitalsRepository(dao(db.vitalsDao()), api) to db
    }

    private fun apiRespondingWith(status: HttpStatusCode): VitalsApi =
        KtorVitalsApi(provideHttpClient(MockEngine { respond(content = "{}", status = status) }))

    private fun vitals(
        patientId: String = "P1",
        visitDate: LocalDate = LocalDate(2025, 10, 31),
    ) = Vitals(
        id = null,
        patientId = PatientId(patientId),
        visitDate = visitDate,
        heightCm = 170.0,
        weightKg = 65.0,
        bmi = 22.5,
    )

    private suspend fun storedVitals(db: AppDatabase): List<VitalsEntity> = db.vitalsDao().findByPatientId("P1").first()

    @Test
    fun submitSucceedsAndMarksSyncedWhenTheApiSucceeds() =
        runTest {
            val (repository, db) = newRepository()

            val result = repository.submitVitals(vitals())
            val stored = storedVitals(db)
            db.close()

            assertEquals(SubmitVitalsResult.Success(synced = true), result)
            assertTrue(stored.single().synced)
        }

    @Test
    fun submitSucceedsLocallyButStaysUnsyncedWhenTheApiFails() =
        runTest {
            val failingApi = FakeVitalsApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)

            val result = repository.submitVitals(vitals())
            val stored = storedVitals(db)
            db.close()

            assertEquals(SubmitVitalsResult.Success(synced = false), result)
            assertFalse(stored.single().synced)
        }

    @Test
    fun submitReturnsErrorWhenTheLocalInsertFailsUnexpectedly() =
        runTest {
            val (repository, db) = newRepository(dao = { ThrowingVitalsDao(it) })

            val result = repository.submitVitals(vitals())
            db.close()

            assertIs<SubmitVitalsResult.Error>(result)
        }

    @Test
    fun submittingTwiceOnTheSameDateForSamePatientReturnsDuplicateAndNeverCallsTheApiTheSecondTime() =
        runTest {
            val api = FakeVitalsApi()
            val (repository, db) = newRepository(api)
            repository.submitVitals(vitals())

            val result = repository.submitVitals(vitals())
            db.close()

            assertEquals(SubmitVitalsResult.DuplicateVisitDate, result)
            assertEquals(1, api.addCallCount)
        }

    @Test
    fun sameDateIsAllowedForADifferentPatient() =
        runTest {
            val (repository, db) = newRepository()
            repository.submitVitals(vitals(patientId = "P1"))

            val result = repository.submitVitals(vitals(patientId = "P2"))
            db.close()

            assertEquals(SubmitVitalsResult.Success(synced = true), result)
        }

    @Test
    fun submitReturnsNeedsAttentionAndPurgesTheRowWhenUnauthorized() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.Unauthorized))

            val result = repository.submitVitals(vitals())
            val stored = storedVitals(db)
            db.close()

            assertEquals(SubmitVitalsResult.NeedsAttention(NeedsAttentionReason.Unauthorized), result)
            assertTrue(stored.isEmpty())
        }

    @Test
    fun submitReturnsNeedsAttentionAndPurgesTheRowWhenValidationFails() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.UnprocessableEntity))

            val result = repository.submitVitals(vitals())
            val stored = storedVitals(db)
            db.close()

            val reason = assertIs<SubmitVitalsResult.NeedsAttention>(result).reason
            val validationFailure = assertIs<NeedsAttentionReason.ValidationFailed>(reason)
            assertTrue(validationFailure.message.isNotBlank())
            assertTrue(stored.isEmpty())
        }

    @Test
    fun resubmittingAfterNeedsAttentionSucceedsAsAFreshSubmission() =
        runTest {
            val db =
                Room
                    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                    .setDriver(AndroidSQLiteDriver())
                    .build()
            val rejecting = DefaultVitalsRepository(db.vitalsDao(), apiRespondingWith(HttpStatusCode.Unauthorized))
            rejecting.submitVitals(vitals())

            val retrying = DefaultVitalsRepository(db.vitalsDao(), FakeVitalsApi())
            val result = retrying.submitVitals(vitals())
            db.close()

            assertEquals(SubmitVitalsResult.Success(synced = true), result)
        }

    @Test
    fun retryUnsyncedPushesMarksSyncedOnSuccess() =
        runTest {
            val failingApi = FakeVitalsApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitVitals(vitals())

            val succeedingRepository = DefaultVitalsRepository(db.vitalsDao(), FakeVitalsApi())
            succeedingRepository.retryUnsyncedPushes()
            val stored = storedVitals(db)
            db.close()

            assertTrue(stored.single().synced)
        }

    @Test
    fun retryUnsyncedPushesLeavesStillRetriableRowsAlone() =
        runTest {
            val failingApi = FakeVitalsApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitVitals(vitals())

            repository.retryUnsyncedPushes()
            val stored = storedVitals(db)
            db.close()

            assertFalse(stored.single().synced)
        }

    @Test
    fun retryUnsyncedPushesPurgesRowsThatNowNeedAttention() =
        runTest {
            val failingApi = FakeVitalsApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitVitals(vitals())

            val rejectingRepository =
                DefaultVitalsRepository(db.vitalsDao(), apiRespondingWith(HttpStatusCode.Unauthorized))
            rejectingRepository.retryUnsyncedPushes()
            val stored = storedVitals(db)
            db.close()

            assertTrue(stored.isEmpty())
        }

    @Test
    fun getVitalsForPatientReturnsLocallyStoredVitalsAsDomainObjects() =
        runTest {
            val (repository, db) = newRepository()
            repository.submitVitals(vitals())

            val found = repository.getVitalsForPatient(PatientId("P1")).first()
            db.close()

            // id is Room-assigned on insert, so the expected value borrows it rather than guessing it.
            assertEquals(listOf(vitals().copy(id = found.single().id)), found)
        }

    @Test
    fun getVitalsForPatientReturnsEmptyWhenNoVitalsExistForThatPatient() =
        runTest {
            val (repository, db) = newRepository()

            val found = repository.getVitalsForPatient(PatientId("missing")).first()
            db.close()

            assertTrue(found.isEmpty())
        }
}
