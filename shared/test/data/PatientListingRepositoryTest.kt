package ke.don.intellisoft_patient_registration.data

import androidx.paging.testing.asSnapshot
import ke.don.intellisoft_patient_registration.datasource.local.VitalsEntity
import ke.don.intellisoft_patient_registration.domain.BmiStatus
import ke.don.intellisoft_patient_registration.domain.DateOfBirth
import ke.don.intellisoft_patient_registration.domain.FirstName
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.domain.LastName
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PatientListingRepositoryTest {
    @Test
    fun getListingJoinsVitalsAndPatientAndClassifiesBmiLocally() =
        runTest {
            val vitalsDao =
                FakeVitalsDao(
                    rowsByDate =
                        mapOf(
                            "2026-10-12" to
                                listOf(
                                    VitalsEntity(
                                        id = 1,
                                        patientId = "P1",
                                        visitDate = "2026-10-12",
                                        heightCm = 180.0,
                                        weightKg = 70.0,
                                        bmi = 21.6,
                                        serverId = null,
                                        synced = true,
                                    ),
                                ),
                        ),
                )
            val patientRepository =
                FakePatientRepository(
                    patients =
                        mapOf(
                            PatientId("P1") to
                                testPatient(firstName = "Jane", lastName = "Mdoe", dob = LocalDate(2000, 1, 1)),
                        ),
                )
            val repository = DefaultPatientListingRepository(vitalsDao, patientRepository)

            val rows = repository.getListing(LocalDate(2026, 10, 12), today = LocalDate(2026, 10, 12)).asSnapshot()

            assertEquals(
                listOf(PatientListingRow(name = "Jane Mdoe", age = 26, bmi = 21.6, status = BmiStatus.NORMAL)),
                rows,
            )
        }

    @Test
    fun getListingReturnsEmptyListWhenNoVisitsMatchTheDate() =
        runTest {
            val repository =
                DefaultPatientListingRepository(FakeVitalsDao(emptyMap()), FakePatientRepository(patients = emptyMap()))
            assertEquals(
                emptyList(),
                repository.getListing(LocalDate(2026, 10, 12), today = LocalDate(2026, 10, 12)).asSnapshot(),
            )
        }

    @Test
    fun getListingLoadsAdditionalPagesWhenScrolledPastTheFirstPage() =
        runTest {
            val visitDate = "2026-10-12"
            val vitalsRows =
                (1..25).map { index ->
                    VitalsEntity(
                        id = index.toLong(),
                        patientId = "P$index",
                        visitDate = visitDate,
                        heightCm = 180.0,
                        weightKg = 70.0,
                        bmi = 21.6,
                        serverId = null,
                        synced = true,
                    )
                }
            val patients =
                (1..25).associate { index ->
                    PatientId("P$index") to
                        testPatient(
                            patientId = "P$index",
                            firstName = "Patient",
                            lastName = "$index",
                            dob = LocalDate(2000, 1, 1),
                        )
                }
            val repository =
                DefaultPatientListingRepository(
                    FakeVitalsDao(mapOf(visitDate to vitalsRows)),
                    FakePatientRepository(patients = patients),
                )

            val rows =
                repository
                    .getListing(LocalDate(2026, 10, 12), today = LocalDate(2026, 10, 12))
                    .asSnapshot { scrollTo(24) }

            assertEquals(25, rows.size)
        }
}

private fun testPatient(
    patientId: String = "P1",
    firstName: String,
    lastName: String,
    dob: LocalDate,
    gender: Gender = Gender.FEMALE,
    registrationDate: LocalDate = LocalDate(2026, 1, 1),
) = Patient(
    patientId = PatientId(patientId),
    registrationDate = registrationDate,
    firstName = FirstName(firstName),
    lastName = LastName(lastName),
    dateOfBirth = DateOfBirth(dob),
    gender = gender,
)
