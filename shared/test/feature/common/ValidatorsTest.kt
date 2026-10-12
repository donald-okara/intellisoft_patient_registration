package ke.don.intellisoft_patient_registration.feature.common

import kotlin.test.Test
import kotlin.test.assertEquals

class ValidatorsTest {
    @Test
    fun requiredRejectsBlankAndWhitespace() {
        val validator = Validators.required()

        assertEquals(Decision.Invalid("Required"), validator.validate(""))
        assertEquals(Decision.Invalid("Required"), validator.validate("  "))
        assertEquals(Decision.Valid, validator.validate("x"))
    }

    @Test
    fun positiveDecimalRejectsNonNumericAndNonPositive() {
        val validator = Validators.positiveDecimal()

        assertEquals(Decision.Valid, validator.validate("70.5"))
        assertEquals(Decision.Invalid("Enter a valid number"), validator.validate("-1"))
        assertEquals(Decision.Invalid("Enter a valid number"), validator.validate("abc"))
        assertEquals(Decision.Invalid("Enter a valid number"), validator.validate("0"))
    }
}
