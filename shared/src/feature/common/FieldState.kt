package ke.don.intellisoft_patient_registration.feature.common

/**
 * Holds one form field's raw text plus its validation outcome, so a screen's ViewModel can
 * expose a `FieldState` per field instead of hand-rolling `value`/`touched`/`error` triples.
 *
 * Validation only surfaces once the field has been [touch]ed — either by the user typing
 * ([onValueChanged]) or by a Submit handler force-touching every field — so a freshly opened
 * form doesn't show every mandatory field as an error before the user has interacted with it.
 *
 * Immutable by design: [onValueChanged]/[touch] return a *new* FieldState rather than mutating
 * in place. A ViewModel reducer does `uiState.copy(email = uiState.email.onValueChanged(x))`,
 * never `.apply { onValueChanged(x) }` — mutating in place would make the old and new UiState
 * carry the same FieldState reference, which breaks two things at once: StateFlow conflates
 * structurally-equal consecutive values (so collectAsState() never emits, and the field looks
 * frozen), and even if equals() were forced to always report "changed", Compose would then tear
 * down and rebuild the TextField on every keystroke instead of updating it in place (losing
 * focus/the IME connection, so a typed character flashes and immediately reverts to empty).
 */
class FieldState(
    initialValue: String = "",
    initialTouched: Boolean = false,
    private val validators: List<FieldValidator> = emptyList(),
) {
    val value: String = initialValue

    val touched: Boolean = initialTouched

    /** First failing validator's [Decision], evaluated in order; [Decision.Valid] if none fail. */
    val decision: Decision
        get() = validators.firstNotNullOfOrNull { it.validate(value) as? Decision.Invalid } ?: Decision.Valid

    /** Non-null only once the field is [touched] and [decision] is [Decision.Invalid]. */
    val errorMessage: String?
        get() = (decision as? Decision.Invalid)?.reason.takeIf { touched }

    val isValid: Boolean
        get() = decision is Decision.Valid

    fun onValueChanged(newValue: String): FieldState = FieldState(newValue, initialTouched = true, validators)

    /** No-op (returns this unchanged) when already touched, so an already-revealed field doesn't force a new instance. */
    fun touch(): FieldState = if (touched) this else FieldState(value, initialTouched = true, validators)
}
