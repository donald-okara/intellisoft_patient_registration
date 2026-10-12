package ke.don.intellisoft_patient_registration.feature.common

/**
 * Factory functions for the common [FieldValidator] rules shared across the Vitals/General/
 * Overweight forms, so each screen's ViewModel wires up validation by calling these rather than
 * rewriting the same blank/numeric checks per field.
 */
object Validators {
    /** Rejects blank or whitespace-only input. */
    fun required(message: String = "Required"): FieldValidator =
        FieldValidator { value ->
            if (value.isBlank()) Decision.Invalid(message) else Decision.Valid
        }

    /**
     * Rejects anything that isn't a positive decimal number — used for vitals fields (height,
     * weight, etc.) where zero or negative values aren't physically valid.
     */
    fun positiveDecimal(message: String = "Enter a valid number"): FieldValidator =
        FieldValidator { value ->
            val parsed = value.toDoubleOrNull()
            if (parsed != null && parsed > 0) Decision.Valid else Decision.Invalid(message)
        }
}
