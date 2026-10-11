package ke.don.intellisoft_patient_registration.data.mapper

import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import ke.don.intellisoft_patient_registration.domain.VitalsId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VitalsMapperTest {
    private val vitals =
        Vitals(
            id = VitalsId(5),
            patientId = PatientId("P1"),
            visitDate = LocalDate(2025, 10, 31),
            heightCm = 170.0,
            weightKg = 65.0,
            bmi = 22.5,
        )

    @Test
    fun entityRoundTripsBackToTheSameVitals() {
        val entity = vitals.toEntity()
        val roundTripped = entity.toDomain()

        assertEquals(vitals, roundTripped)
    }

    @Test
    fun newVitalsWithNoIdMapsToEntityWithZeroId() {
        val newVitals = vitals.copy(id = null)

        val entity = newVitals.toEntity()

        assertEquals(0L, entity.id)
    }

    @Test
    fun entityWithZeroIdMapsBackToNullDomainId() {
        val entity = vitals.toEntity().copy(id = 0L)

        assertNull(entity.toDomain().id)
    }

    @Test
    fun toAddVitalsRequestDtoSendsStringTypedFields() {
        val dto = vitals.toAddVitalsRequestDto()

        assertEquals("2025-10-31", dto.visitDate)
        assertEquals("170.0", dto.height)
        assertEquals("65.0", dto.weight)
        assertEquals("22.5", dto.bmi)
        assertEquals("P1", dto.patientId)
    }
}
