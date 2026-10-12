package ke.don.intellisoft_patient_registration.data

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import ke.don.intellisoft_patient_registration.data.mapper.FakeFieldEncryptor
import ke.don.intellisoft_patient_registration.data.mapper.PatientMapper
import ke.don.intellisoft_patient_registration.data.mapper.ThrowingFieldEncryptor
import ke.don.intellisoft_patient_registration.data.sync.NeedsAttentionReason
import ke.don.intellisoft_patient_registration.datasource.local.AppDatabase
import ke.don.intellisoft_patient_registration.datasource.remote.KtorPatientApi
import ke.don.intellisoft_patient_registration.datasource.remote.PatientApi
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ApiResponse
import ke.don.intellisoft_patient_registration.datasource.remote.dto.PatientDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.ProceedMessageDataDto
import ke.don.intellisoft_patient_registration.datasource.remote.provideHttpClient
import ke.don.intellisoft_patient_registration.domain.DateOfBirth
import ke.don.intellisoft_patient_registration.domain.FirstName
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.domain.LastName
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * This is the assignment's required "one repository test using a faked or mocked API, not a
 * live network call" - FakePatientApi stands in for the network, a real in-memory Room database
 * (Robolectric, same pattern as the DAO tests) stands in for local storage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DefaultPatientRepositoryTest {
    private fun newRepository(
        api: PatientApi = FakePatientApi(),
        mapper: PatientMapper = PatientMapper(FakeFieldEncryptor()),
    ): Pair<DefaultPatientRepository, AppDatabase> {
        val db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .setDriver(AndroidSQLiteDriver())
                .build()
        return DefaultPatientRepository(db.patientDao(), api, mapper) to db
    }

    private fun apiRespondingWith(status: HttpStatusCode): PatientApi =
        KtorPatientApi(provideHttpClient(MockEngine { respond(content = "{}", status = status) }))

    private fun patient(patientId: String = "P1") =
        Patient(
            patientId = PatientId(patientId),
            registrationDate = LocalDate(2025, 10, 31),
            firstName = FirstName("Jane"),
            lastName = LastName("Doe"),
            dateOfBirth = DateOfBirth(LocalDate(2000, 1, 1)),
            gender = Gender.FEMALE,
        )

    @Test
    fun registerSucceedsAndMarksSyncedWhenTheApiSucceeds() =
        runTest {
            val (repository, db) = newRepository()

            val result = repository.registerPatient(patient())
            val stored = db.patientDao().findByPatientId("P1")
            db.close()

            assertEquals(RegisterPatientResult.Success(synced = true), result)
            assertTrue(stored?.synced == true)
        }

    @Test
    fun registerSucceedsLocallyButStaysUnsyncedWhenTheApiFails() =
        runTest {
            val failingApi = FakePatientApi(registerResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)

            val result = repository.registerPatient(patient())
            val stored = db.patientDao().findByPatientId("P1")
            db.close()

            assertEquals(RegisterPatientResult.Success(synced = false), result)
            assertFalse(stored?.synced == true)
        }

    @Test
    fun registerReturnsErrorWhenMappingToAnEntityFailsUnexpectedly() =
        runTest {
            val (repository, db) = newRepository(mapper = PatientMapper(ThrowingFieldEncryptor()))

            val result = repository.registerPatient(patient())
            db.close()

            assertIs<RegisterPatientResult.Error>(result)
        }

    @Test
    fun registeringTheSamePatientIdTwiceReturnsDuplicateAndNeverCallsTheApiTheSecondTime() =
        runTest {
            val api = FakePatientApi()
            val (repository, db) = newRepository(api)
            repository.registerPatient(patient())

            val result = repository.registerPatient(patient())
            db.close()

            assertEquals(RegisterPatientResult.DuplicatePatientId, result)
            assertEquals(1, api.registerCallCount)
        }

    @Test
    fun registerReturnsNeedsAttentionAndPurgesTheRowWhenUnauthorized() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.Unauthorized))

            val result = repository.registerPatient(patient())
            val stored = db.patientDao().findByPatientId("P1")
            db.close()

            assertEquals(RegisterPatientResult.NeedsAttention(NeedsAttentionReason.Unauthorized), result)
            assertNull(stored)
        }

    @Test
    fun registerReturnsNeedsAttentionAndPurgesTheRowWhenValidationFails() =
        runTest {
            val (repository, db) = newRepository(apiRespondingWith(HttpStatusCode.UnprocessableEntity))

            val result = repository.registerPatient(patient())
            val stored = db.patientDao().findByPatientId("P1")
            db.close()

            val reason = assertIs<RegisterPatientResult.NeedsAttention>(result).reason
            val validationFailure = assertIs<NeedsAttentionReason.ValidationFailed>(reason)
            assertTrue(validationFailure.message.isNotBlank())
            assertNull(stored)
        }

    @Test
    fun resubmittingAfterNeedsAttentionSucceedsAsAFreshRegistration() =
        runTest {
            val db =
                Room
                    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                    .setDriver(AndroidSQLiteDriver())
                    .build()
            val mapper = PatientMapper(FakeFieldEncryptor())
            val rejecting =
                DefaultPatientRepository(db.patientDao(), apiRespondingWith(HttpStatusCode.Unauthorized), mapper)
            rejecting.registerPatient(patient())

            val retrying = DefaultPatientRepository(db.patientDao(), FakePatientApi(), mapper)
            val result = retrying.registerPatient(patient())
            db.close()

            assertEquals(RegisterPatientResult.Success(synced = true), result)
        }

    @Test
    fun retryUnsyncedPushesMarksSyncedOnSuccess() =
        runTest {
            val failingApi = FakePatientApi(registerResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.registerPatient(patient())

            val succeedingRepository =
                DefaultPatientRepository(db.patientDao(), FakePatientApi(), PatientMapper(FakeFieldEncryptor()))
            succeedingRepository.retryUnsyncedPushes()
            val stored = db.patientDao().findByPatientId("P1")
            db.close()

            assertTrue(stored?.synced == true)
        }

    @Test
    fun retryUnsyncedPushesLeavesStillRetriableRowsAlone() =
        runTest {
            val failingApi = FakePatientApi(registerResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.registerPatient(patient())

            repository.retryUnsyncedPushes()
            val stored = db.patientDao().findByPatientId("P1")
            db.close()

            assertFalse(stored?.synced == true)
        }

    @Test
    fun retryUnsyncedPushesPurgesRowsThatNowNeedAttention() =
        runTest {
            val failingApi = FakePatientApi(registerResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(failingApi)
            repository.registerPatient(patient())

            val rejectingRepository =
                DefaultPatientRepository(
                    db.patientDao(),
                    apiRespondingWith(HttpStatusCode.Unauthorized),
                    PatientMapper(FakeFieldEncryptor()),
                )
            rejectingRepository.retryUnsyncedPushes()
            val stored = db.patientDao().findByPatientId("P1")
            db.close()

            assertNull(stored)
        }

    @Test
    fun syncPullsTheApisPatientListIntoLocalStorage() =
        runTest {
            val api =
                FakePatientApi(
                    listResponse = {
                        ApiResponse(
                            "success",
                            true,
                            200,
                            listOf(
                                PatientDto(
                                    id = 9,
                                    unique = "P2",
                                    firstname = "John",
                                    lastname = "Smith",
                                    dob = "1999-05-05",
                                    gender = "Male",
                                    regDate = "2025-10-31",
                                    createdAt = "2025-10-31T00:00:00Z",
                                    updatedAt = "2025-10-31T00:00:00Z",
                                ),
                            ),
                        )
                    },
                )
            val (repository, db) = newRepository(api)

            val result = repository.sync()
            val stored = db.patientDao().findByPatientId("P2")
            db.close()

            assertTrue(result)
            assertEquals(9L, stored?.serverId)
            assertTrue(stored?.synced == true)
        }

    @Test
    fun syncReturnsFalseAndLeavesLocalDataUntouchedWhenTheApiThrows() =
        runTest {
            val api = FakePatientApi(listResponse = { throw RuntimeException("network down") })
            val (repository, db) = newRepository(api)
            db.patientDao().insert(
                PatientMapper(FakeFieldEncryptor()).toEntity(patient("existing")),
            )

            val result = repository.sync()
            val stillThere = db.patientDao().findByPatientId("existing")
            db.close()

            assertFalse(result)
            assertEquals("existing", stillThere?.patientId)
        }

    @Test
    fun getPatientsReturnsLocallyStoredPatientsAsDomainObjects() =
        runTest {
            val (repository, db) = newRepository()
            repository.registerPatient(patient())

            val all = repository.getPatients(page = 0, pageSize = 10)
            db.close()

            assertEquals(listOf(patient()), all)
        }

    @Test
    fun getPatientsRespectsPageAndPageSize() =
        runTest {
            val (repository, db) = newRepository()
            repository.registerPatient(patient("P1"))
            repository.registerPatient(patient("P2"))
            repository.registerPatient(patient("P3"))

            val firstPage = repository.getPatients(page = 0, pageSize = 2)
            val secondPage = repository.getPatients(page = 1, pageSize = 2)
            db.close()

            assertEquals(2, firstPage.size)
            assertEquals(1, secondPage.size)
        }

    @Test
    fun getPatientsRejectsAnInvalidPageOrPageSize() =
        runTest {
            val (repository, db) = newRepository()

            assertFailsWith<IllegalArgumentException> { repository.getPatients(page = -1, pageSize = 10) }
            assertFailsWith<IllegalArgumentException> { repository.getPatients(page = 0, pageSize = 0) }
            db.close()
        }
}
