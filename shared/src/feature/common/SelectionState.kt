package ke.don.intellisoft_patient_registration.feature.common

/**
 * Holds one form field's selected value plus its validation outcome, for non-text fields
 * (enum pickers, date pickers, yes/no toggles) — the generic analog of [FieldState].
 *
 * Unlike [FieldState]'s pluggable [FieldValidator] list, there's exactly one rule here: a
 * selection is required. That's the only validation a picker needs — there's no equivalent of
 * "format" or "range" checks on an already-constrained [T], so a validator list would be
 * unused flexibility.
 *
 * Validation only surfaces once the field has been [touch]ed — either by the user picking a
 * value ([select]) or by a Submit handler force-touching every field — matching [FieldState]'s
 * same touch-gated contract so a freshly opened form doesn't show every mandatory field as an
 * error before the user has interacted with it.
 */
class SelectionState<T>(
    initial: T? = null,
    private val requiredMessage: String = "Required",
) {
    var selected: T? = initial
        private set

    var touched: Boolean = false
        private set

    /** [Decision.Valid] once a value is [selected]; [Decision.Invalid] while it's still null. */
    val decision: Decision
        get() = if (selected != null) Decision.Valid else Decision.Invalid(requiredMessage)

    /** Non-null only once the field is [touched] and [decision] is [Decision.Invalid]. */
    val errorMessage: String?
        get() = (decision as? Decision.Invalid)?.reason.takeIf { touched }

    val isValid: Boolean
        get() = decision is Decision.Valid

    fun select(value: T) {
        selected = value
        touched = true
    }

    fun touch() {
        touched = true
    }
}
