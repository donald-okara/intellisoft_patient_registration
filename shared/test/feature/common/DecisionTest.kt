package ke.don.intellisoft_patient_registration.feature.common

import kotlin.test.Test
import kotlin.test.assertEquals

class DecisionTest {
    @Test
    fun validatorReturnsInvalidWithReasonForBlankValue() {
        val validator = FieldValidator { value -> if (value.isBlank()) Decision.Invalid("Required") else Decision.Valid }
        assertEquals(Decision.Invalid("Required"), validator.validate(""))
    }

    @Test
    fun validatorReturnsValidForNonBlankValue() {
        val validator = FieldValidator { value -> if (value.isBlank()) Decision.Invalid("Required") else Decision.Valid }
        assertEquals(Decision.Valid, validator.validate("x"))
    }
}
