package ke.don.intellisoft_patient_registration.data

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import ke.don.intellisoft_patient_registration.data.mapper.FakeFieldEncryptor
import ke.don.intellisoft_patient_registration.data.mapper.OverweightAssessmentMapper
import ke.don.intellisoft_patient_registration.data.mapper.ThrowingFieldEncryptor
import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import ke.don.intellisoft_patient_registration.datasource.remote.KtorVisitApi
import ke.don.intellisoft_patient_registration.datasource.remote.VisitApi
import ke.don.intellisoft_patient_registration.datasource.remote.provideHttpClient
import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.VitalsId
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
import kotlin.test.assertTrue

/**
 * This is the Overweight Assessment equivalent of DefaultGeneralAssessmentRepositoryTest's
 * required "repository test using a faked API, not a live network call" - FakeVisitApi stands in
 * for the network, a real in-memory Room database (Robolectric, same pattern as the DAO tests)
 * stands in for local storage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DefaultOverweightAssessmentRepositoryTest {
    private fun newRepository(
        api: VisitApi = FakeVisitApi(),
        mapper: OverweightAssessmentMapper = OverweightAssessmentMapper(FakeFieldEncryptor()),
    ): Pair<DefaultOverweightAssessmentRepository, AppDatabase> {
        val db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .build()
        return DefaultOverweightAssessmentRepository(db.overweightAssessmentDao(), api, mapper) to db
    }

    private fun apiRespondingWith(status: HttpStatusCode): VisitApi =
        KtorVisitApi(provideHttpClient(MockEngine { respond(content = "{}", status = status) }))

    private fun assessment(
        patientId: String = "P1",
        visitDate: LocalDate = LocalDate(2025, 10, 31),
    ) = OverweightAssessment(
        id = null,
        patientId = PatientId(patientId),
        vitalsId = VitalsId(1),
        visitDate = visitDate,
        generalHealth = HealthStatus.POOR,
        currentlyUsingDrugs = false,
        comments = Comments("overweight, no drug use reported"),
    )

    @Test
    fun submitSucceedsAndMarksSyncedWhenTheApiSucceeds() =
        runTest {
            val (repository, db) = newRepository()

            val result = repository.submitOverweightAssessment(assessment())
            val stored = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(SubmitOverweightAssessmentResult.Success(synced = true), result)
            assertTrue(stored.single().synced)
        }

    @Test
    fun submitSucceedsLocallyButStaysUnsyncedWhenTheApiFails() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)

            val result = repository.submitOverweightAssessment(assessment())
            val stored = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(SubmitOverweightAssessmentResult.Success(synced = false), result)
            assertFalse(stored.single().synced)
        }

    @Test
    fun submitReturnsErrorWhenMappingToAnEntityFailsUnexpectedly() =
        runTest {
            val (repository, db) = newRepository(mapper = OverweightAssessmentMapper(ThrowingFieldEncryptor()))

            val result = repository.submitOverweightAssessment(assessment())
            db.close()

            assertIs<SubmitOverweightAssessmentResult.Error>(result)
        }

    @Test
    fun submittingTwiceOnTheSameDateForSamePatientReturnsDuplicateAndNeverCallsTheApiTheSecondTime() =
        runTest {
            val api = FakeVisitApi()
            val (repository, db) = newRepository(api)
            repository.submitOverweightAssessment(assessment())

            val result = repository.submitOverweightAssessment(assessment())
            db.close()

            assertEquals(SubmitOverweightAssessmentResult.DuplicateVisitDate, result)
            assertEquals(1, api.addCallCount)
        }

    @Test
    fun submitReturnsNeedsAttentionAndPurgesTheRowWhenUnauthorized() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.Unauthorized))

            val result = repository.submitOverweightAssessment(assessment())
            val stored = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(SubmitOverweightAssessmentResult.NeedsAttention(NeedsAttentionReason.Unauthorized), result)
            assertTrue(stored.isEmpty())
        }

    @Test
    fun submitReturnsNeedsAttentionAndPurgesTheRowWhenValidationFails() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.UnprocessableEntity))

            val result = repository.submitOverweightAssessment(assessment())
            val stored = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            val reason = assertIs<SubmitOverweightAssessmentResult.NeedsAttention>(result).reason
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
            val mapper = OverweightAssessmentMapper(FakeFieldEncryptor())
            val rejecting =
                DefaultOverweightAssessmentRepository(
                    db.overweightAssessmentDao(),
                    apiRespondingWith(HttpStatusCode.Unauthorized),
                    mapper,
                )
            rejecting.submitOverweightAssessment(assessment())

            val retrying = DefaultOverweightAssessmentRepository(db.overweightAssessmentDao(), FakeVisitApi(), mapper)
            val result = retrying.submitOverweightAssessment(assessment())
            db.close()

            assertEquals(SubmitOverweightAssessmentResult.Success(synced = true), result)
        }

    @Test
    fun retryUnsyncedPushesMarksSyncedOnSuccess() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitOverweightAssessment(assessment())

            val succeedingRepository =
                DefaultOverweightAssessmentRepository(
                    db.overweightAssessmentDao(),
                    FakeVisitApi(),
                    OverweightAssessmentMapper(FakeFieldEncryptor()),
                )
            succeedingRepository.retryUnsyncedPushes()
            val stored = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertTrue(stored.single().synced)
        }

    @Test
    fun retryUnsyncedPushesLeavesStillRetriableRowsAlone() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitOverweightAssessment(assessment())

            repository.retryUnsyncedPushes()
            val stored = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertFalse(stored.single().synced)
        }

    @Test
    fun retryUnsyncedPushesPurgesRowsThatNowNeedAttention() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitOverweightAssessment(assessment())

            val rejectingRepository =
                DefaultOverweightAssessmentRepository(
                    db.overweightAssessmentDao(),
                    apiRespondingWith(HttpStatusCode.Unauthorized),
                    OverweightAssessmentMapper(FakeFieldEncryptor()),
                )
            rejectingRepository.retryUnsyncedPushes()
            val stored = db.overweightAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertTrue(stored.isEmpty())
        }

    @Test
    fun getOverweightAssessmentsForPatientReturnsLocallyStoredAssessmentsAsDomainObjects() =
        runTest {
            val (repository, db) = newRepository()
            repository.submitOverweightAssessment(assessment())

            val found = repository.getOverweightAssessmentsForPatient(PatientId("P1")).first()
            db.close()

            assertEquals(listOf(assessment().copy(id = found.single().id)), found)
        }

    @Test
    fun getOverweightAssessmentsForPatientReturnsEmptyWhenNoneExistForThatPatient() =
        runTest {
            val (repository, db) = newRepository()

            val found = repository.getOverweightAssessmentsForPatient(PatientId("missing")).first()
            db.close()

            assertTrue(found.isEmpty())
        }
}
