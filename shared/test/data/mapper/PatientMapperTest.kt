package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.datasource.remote.dto.PatientDto
import ke.don.intellisoft_patient_registration.datasource.remote.dto.RegisterPatientRequestDto
import ke.don.intellisoft_patient_registration.domain.DateOfBirth
import ke.don.intellisoft_patient_registration.domain.FirstName
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.domain.LastName
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PatientMapperTest {
    private val mapper = PatientMapper(FakeFieldEncryptor())

    private val patient =
        Patient(
            patientId = PatientId("P1"),
            registrationDate = LocalDate(2025, 10, 31),
            firstName = FirstName("Jane"),
            lastName = LastName("Doe"),
            dateOfBirth = DateOfBirth(LocalDate(2000, 1, 1)),
            gender = Gender.FEMALE,
        )

    @Test
    fun entityRoundTripsBackToTheSamePatient() =
        runTest {
            val entity = mapper.toEntity(patient)
            val roundTripped = mapper.toDomain(entity)

            assertEquals(patient, roundTripped)
        }

    @Test
    fun entityStoresCiphertextNotPlaintextPii() =
        runTest {
            val entity = mapper.toEntity(patient)

            assertFalse(entity.firstName.contains("Jane"))
            assertFalse(entity.lastName.contains("Doe"))
            assertFalse(entity.dateOfBirth.contains("2000"))
        }

    @Test
    fun entityIdAndGenderAreNotEncrypted() =
        runTest {
            val entity = mapper.toEntity(patient)

            assertEquals("P1", entity.patientId)
            assertEquals("FEMALE", entity.gender)
        }

    @Test
    fun toRegisterRequestDtoMatchesApiShape() {
        val dto = patient.toRegisterRequestDto()

        assertEquals(
            RegisterPatientRequestDto(
                firstname = "Jane",
                lastname = "Doe",
                unique = "P1",
                dob = "2000-01-01",
                gender = "Female",
                regDate = "2025-10-31",
            ),
            dto,
        )
    }

    @Test
    fun patientDtoMapsToDomain() {
        val dto =
            PatientDto(
                id = 3,
                unique = "P1",
                firstname = "Jane",
                lastname = "Doe",
                dob = "2000-01-01",
                gender = "Female",
                regDate = "2025-10-31",
                createdAt = "2025-10-30 16:35:41",
                updatedAt = "2025-10-30 16:35:41",
            )

        assertEquals(patient, dto.toDomain())
    }
}
