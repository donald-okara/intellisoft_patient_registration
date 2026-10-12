package ke.don.intellisoft_patient_registration.feature.common

import kotlin.test.Test
import kotlin.test.assertEquals

class FieldStateTest {
    @Test
    fun untouchedInvalidFieldHasNoErrorMessage() {
        val field = FieldState(validators = listOf(Validators.required()))
        assertEquals(null, field.errorMessage)
        assertEquals(false, field.touched)
    }

    @Test
    fun touchedInvalidFieldExposesTheFirstFailingReason() {
        val field = FieldState(validators = listOf(Validators.required("Required"))).onValueChanged("")
        assertEquals("Required", field.errorMessage)
        assertEquals(false, field.isValid)
    }

    @Test
    fun touchedValidFieldHasNoErrorMessage() {
        val field = FieldState(validators = listOf(Validators.required())).onValueChanged("Jane")
        assertEquals(null, field.errorMessage)
        assertEquals(true, field.isValid)
    }

    @Test
    fun touchWithoutChangingValueStillRevealsExistingErrors() {
        val field = FieldState(validators = listOf(Validators.required("Required"))).touch()
        assertEquals("Required", field.errorMessage)
    }
}
