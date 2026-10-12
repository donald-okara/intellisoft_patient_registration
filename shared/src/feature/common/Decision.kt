package ke.don.intellisoft_patient_registration.feature.common

/**
 * Outcome of validating a single form field.
 *
 * A sealed type rather than a `Boolean` so the failure carries the message to show
 * the user, and so every consumer is forced to handle both cases with `when`.
 */
sealed interface Decision {
    data object Valid : Decision

    data class Invalid(val reason: String) : Decision
}

/**
 * Validates one field's raw text. A `fun interface` so a rule can be written as a
 * lambda at the call site, e.g. `FieldValidator { if (it.isBlank()) Decision.Invalid("Required") else Decision.Valid }`.
 */
fun interface FieldValidator {
    fun validate(value: String): Decision
}
