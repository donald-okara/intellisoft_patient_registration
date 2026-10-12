package ke.don.intellisoft_patient_registration.feature.common

import ke.don.intellisoft_patient_registration.domain.Gender
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectionStateTest {
    @Test
    fun untouchedUnselectedFieldHasNoErrorMessage() {
        val field = SelectionState<Gender>()
        assertEquals(null, field.errorMessage)
        assertEquals(false, field.touched)
    }

    @Test
    fun touchedUnselectedFieldExposesTheRequiredReason() {
        val field = SelectionState<Gender>(requiredMessage = "Required").touch()
        assertEquals("Required", field.errorMessage)
        assertEquals(false, field.isValid)
    }

    @Test
    fun touchedSelectedFieldHasNoErrorMessage() {
        val field = SelectionState<Gender>().select(Gender.FEMALE)
        assertEquals(null, field.errorMessage)
        assertEquals(true, field.isValid)
    }
}
