package ke.don.intellisoft_patient_registration.data

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import ke.don.intellisoft_patient_registration.data.mapper.FakeFieldEncryptor
import ke.don.intellisoft_patient_registration.data.mapper.GeneralAssessmentMapper
import ke.don.intellisoft_patient_registration.data.mapper.ThrowingFieldEncryptor
import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import ke.don.intellisoft_patient_registration.datasource.remote.KtorVisitApi
import ke.don.intellisoft_patient_registration.datasource.remote.VisitApi
import ke.don.intellisoft_patient_registration.datasource.remote.provideHttpClient
import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.HealthStatus
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
 * This is the General Assessment equivalent of DefaultVitalsRepositoryTest's required "repository
 * test using a faked API, not a live network call" - FakeVisitApi stands in for the network, a
 * real in-memory Room database (Robolectric, same pattern as the DAO tests) stands in for local
 * storage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DefaultGeneralAssessmentRepositoryTest {
    private fun newRepository(
        api: VisitApi = FakeVisitApi(),
        mapper: GeneralAssessmentMapper = GeneralAssessmentMapper(FakeFieldEncryptor()),
    ): Pair<DefaultGeneralAssessmentRepository, AppDatabase> {
        val db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .build()
        return DefaultGeneralAssessmentRepository(db.generalAssessmentDao(), api, mapper) to db
    }

    private fun apiRespondingWith(status: HttpStatusCode): VisitApi =
        KtorVisitApi(provideHttpClient(MockEngine { respond(content = "{}", status = status) }))

    private fun assessment(
        patientId: String = "P1",
        visitDate: LocalDate = LocalDate(2025, 10, 31),
    ) = GeneralAssessment(
        id = null,
        patientId = PatientId(patientId),
        vitalsId = VitalsId(1),
        visitDate = visitDate,
        generalHealth = HealthStatus.GOOD,
        everDietedToLoseWeight = true,
        comments = Comments("feeling fine"),
    )

    @Test
    fun submitSucceedsAndMarksSyncedWhenTheApiSucceeds() =
        runTest {
            val (repository, db) = newRepository()

            val result = repository.submitGeneralAssessment(assessment())
            val stored = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(SubmitGeneralAssessmentResult.Success(synced = true), result)
            assertTrue(stored.single().synced)
        }

    @Test
    fun submitSucceedsLocallyButStaysUnsyncedWhenTheApiFails() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)

            val result = repository.submitGeneralAssessment(assessment())
            val stored = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(SubmitGeneralAssessmentResult.Success(synced = false), result)
            assertFalse(stored.single().synced)
        }

    @Test
    fun submitReturnsErrorWhenMappingToAnEntityFailsUnexpectedly() =
        runTest {
            val (repository, db) = newRepository(mapper = GeneralAssessmentMapper(ThrowingFieldEncryptor()))

            val result = repository.submitGeneralAssessment(assessment())
            db.close()

            assertIs<SubmitGeneralAssessmentResult.Error>(result)
        }

    @Test
    fun submittingTwiceOnTheSameDateForSamePatientReturnsDuplicateAndNeverCallsTheApiTheSecondTime() =
        runTest {
            val api = FakeVisitApi()
            val (repository, db) = newRepository(api)
            repository.submitGeneralAssessment(assessment())

            val result = repository.submitGeneralAssessment(assessment())
            db.close()

            assertEquals(SubmitGeneralAssessmentResult.DuplicateVisitDate, result)
            assertEquals(1, api.addCallCount)
        }

    @Test
    fun submitReturnsNeedsAttentionAndPurgesTheRowWhenUnauthorized() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.Unauthorized))

            val result = repository.submitGeneralAssessment(assessment())
            val stored = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertEquals(SubmitGeneralAssessmentResult.NeedsAttention(NeedsAttentionReason.Unauthorized), result)
            assertTrue(stored.isEmpty())
        }

    @Test
    fun submitReturnsNeedsAttentionAndPurgesTheRowWhenValidationFails() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.UnprocessableEntity))

            val result = repository.submitGeneralAssessment(assessment())
            val stored = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            val reason = assertIs<SubmitGeneralAssessmentResult.NeedsAttention>(result).reason
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
            val mapper = GeneralAssessmentMapper(FakeFieldEncryptor())
            val rejecting =
                DefaultGeneralAssessmentRepository(
                    db.generalAssessmentDao(),
                    apiRespondingWith(HttpStatusCode.Unauthorized),
                    mapper,
                )
            rejecting.submitGeneralAssessment(assessment())

            val retrying = DefaultGeneralAssessmentRepository(db.generalAssessmentDao(), FakeVisitApi(), mapper)
            val result = retrying.submitGeneralAssessment(assessment())
            db.close()

            assertEquals(SubmitGeneralAssessmentResult.Success(synced = true), result)
        }

    @Test
    fun retryUnsyncedPushesMarksSyncedOnSuccess() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitGeneralAssessment(assessment())

            val succeedingRepository =
                DefaultGeneralAssessmentRepository(
                    db.generalAssessmentDao(),
                    FakeVisitApi(),
                    GeneralAssessmentMapper(FakeFieldEncryptor()),
                )
            succeedingRepository.retryUnsyncedPushes()
            val stored = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertTrue(stored.single().synced)
        }

    @Test
    fun retryUnsyncedPushesLeavesStillRetriableRowsAlone() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitGeneralAssessment(assessment())

            repository.retryUnsyncedPushes()
            val stored = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertFalse(stored.single().synced)
        }

    @Test
    fun retryUnsyncedPushesPurgesRowsThatNowNeedAttention() =
        runTest {
            val failingApi = FakeVisitApi(addResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.submitGeneralAssessment(assessment())

            val rejectingRepository =
                DefaultGeneralAssessmentRepository(
                    db.generalAssessmentDao(),
                    apiRespondingWith(HttpStatusCode.Unauthorized),
                    GeneralAssessmentMapper(FakeFieldEncryptor()),
                )
            rejectingRepository.retryUnsyncedPushes()
            val stored = db.generalAssessmentDao().findByPatientId("P1").first()
            db.close()

            assertTrue(stored.isEmpty())
        }

    @Test
    fun getGeneralAssessmentsForPatientReturnsLocallyStoredAssessmentsAsDomainObjects() =
        runTest {
            val (repository, db) = newRepository()
            repository.submitGeneralAssessment(assessment())

            val found = repository.getGeneralAssessmentsForPatient(PatientId("P1")).first()
            db.close()

            assertEquals(listOf(assessment().copy(id = found.single().id)), found)
        }

    @Test
    fun getGeneralAssessmentsForPatientReturnsEmptyWhenNoneExistForThatPatient() =
        runTest {
            val (repository, db) = newRepository()

            val found = repository.getGeneralAssessmentsForPatient(PatientId("missing")).first()
            db.close()

            assertTrue(found.isEmpty())
        }
}
