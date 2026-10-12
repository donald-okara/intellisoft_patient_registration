package ke.don.intellisoft_patient_registration.domain

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PatientIdTest {
    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun generateProducesAParseableUuid() {
        val id = PatientId.generate()

        Uuid.parse(id.value)
    }

    @Test
    fun generateProducesADifferentIdEachCall() {
        assertNotEquals(PatientId.generate(), PatientId.generate())
    }
}
