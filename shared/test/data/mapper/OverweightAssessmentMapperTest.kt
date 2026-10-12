package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
import ke.don.intellisoft_patient_registration.domain.OverweightAssessmentId
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.VitalsId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OverweightAssessmentMapperTest {
    private val mapper = OverweightAssessmentMapper(FakeFieldEncryptor())

    private val assessment =
        OverweightAssessment(
            id = OverweightAssessmentId(9),
            patientId = PatientId("P1"),
            vitalsId = VitalsId(5),
            visitDate = LocalDate(2025, 10, 31),
            generalHealth = HealthStatus.POOR,
            currentlyUsingDrugs = false,
            comments = Comments("Patient reports no current medication."),
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

            assertFalse(entity.comments.contains("medication"))
        }

    /**
     * onDiet sends "No", not null - visits/add rejects the request without it, confirmed live
     * against the real API (see docs/endpoints.md's confirmed flaws section and
     * OverweightAssessmentMapper's own doc comment).
     */
    @Test
    fun toAddVisitRequestDtoSetsOnDrugsAndAnInertOnDietPlaceholder() =
        runTest {
            val dto = assessment.toAddVisitRequestDto(serverPatientId = 13L)

            assertEquals("Poor", dto.generalHealth)
            assertEquals("No", dto.onDrugs)
            assertEquals("No", dto.onDiet)
            assertEquals("Patient reports no current medication.", dto.comments)
            assertEquals("13", dto.patientId)
            assertEquals("5", dto.vitalId)
        }
}
