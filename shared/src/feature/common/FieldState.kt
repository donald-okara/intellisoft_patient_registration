package ke.don.intellisoft_patient_registration.feature.common

/**
 * Holds one form field's raw text plus its validation outcome, so a screen's ViewModel can
 * expose a `FieldState` per field instead of hand-rolling `value`/`touched`/`error` triples.
 *
 * Validation only surfaces once the field has been [touch]ed — either by the user typing
 * ([onValueChanged]) or by a Submit handler force-touching every field — so a freshly opened
 * form doesn't show every mandatory field as an error before the user has interacted with it.
 *
 * Backed by plain `var` properties rather than Compose snapshot state: this class has no
 * Compose dependency and is fully testable as plain Kotlin. A screen that reads `field.value`
 * directly inside a composable would need `mutableStateOf` to recompose automatically on
 * change — that's a concern for the ViewModel/screen wiring that consumes this, not for this
 * class's own correctness.
 */
class FieldState(
    initialValue: String = "",
    private val validators: List<FieldValidator> = emptyList(),
) {
    var value: String = initialValue
        private set

    var touched: Boolean = false
        private set

    /** First failing validator's [Decision], evaluated in order; [Decision.Valid] if none fail. */
    val decision: Decision
        get() = validators.firstNotNullOfOrNull { it.validate(value) as? Decision.Invalid } ?: Decision.Valid

    /** Non-null only once the field is [touched] and [decision] is [Decision.Invalid]. */
    val errorMessage: String?
        get() = (decision as? Decision.Invalid)?.reason.takeIf { touched }

    val isValid: Boolean
        get() = decision is Decision.Valid

    fun onValueChanged(newValue: String) {
        value = newValue
        touched = true
    }

    fun touch() {
        touched = true
    }
}
