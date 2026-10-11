package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.GeneralAssessmentId
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.VitalsId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GeneralAssessmentMapperTest {
    private val mapper = GeneralAssessmentMapper(FakeFieldEncryptor())

    private val assessment =
        GeneralAssessment(
            id = GeneralAssessmentId(7),
            patientId = PatientId("P1"),
            vitalsId = VitalsId(5),
            visitDate = LocalDate(2025, 10, 31),
            generalHealth = HealthStatus.GOOD,
            everDietedToLoseWeight = true,
            comments = Comments("Patient reports regular exercise."),
        )

    @Test
    fun entityRoundTripsBackToTheSameAssessment() =
        runTest {
            val entity = mapper.toEntity(assessment)
            val roundTripped = mapper.toDomain(entity)

            assertEquals(assessment, roundTripped)
        }

    @Test
    fun entityStoresCiphertextNotPlaintextComments() =
        runTest {
            val entity = mapper.toEntity(assessment)

            assertFalse(entity.comments.contains("exercise"))
        }

    @Test
    fun everDietedFalseRoundTrips() =
        runTest {
            val entity = mapper.toEntity(assessment.copy(everDietedToLoseWeight = false))

            assertEquals(false, mapper.toDomain(entity).everDietedToLoseWeight)
        }

    @Test
    fun toAddVisitRequestDtoSetsOnDietNotOnDrugs() =
        runTest {
            val dto = assessment.toAddVisitRequestDto()

            assertEquals("Good", dto.generalHealth)
            assertEquals("Yes", dto.onDiet)
            assertNull(dto.onDrugs)
            assertEquals("Patient reports regular exercise.", dto.comments)
            assertEquals("5", dto.vitalId)
        }
}
