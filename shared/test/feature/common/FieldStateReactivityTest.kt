package ke.don.intellisoft_patient_registration.feature.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

data class ScratchUiState(
    val email: FieldState = FieldState(),
)

/**
 * Regression test: a form field's value/touched mutators must return a *new* FieldState rather
 * than mutating in place. Mutating in place previously made the "old" and "new" wrapping UiState
 * carry the same FieldState reference, which broke two things at once: StateFlow conflated the
 * structurally-equal consecutive states (so collectAsState() never emitted, and the field looked
 * frozen), and forcing FieldState to report itself as "always changed" made Compose tear down
 * and rebuild the TextField every keystroke instead (so a typed character flashed and
 * immediately reverted, because the IME connection/focus was lost on every recomposition).
 */
class FieldStateReactivityTest {
    @Test
    fun onValueChangedReturnsANewInstanceDistinctFromTheOriginal() {
        val original = ScratchUiState()

        val changed = original.copy(email = original.email.onValueChanged("a"))

        assertNotSame(original.email, changed.email)
        assertEquals("a", changed.email.value)
        assertEquals("", original.email.value)
    }

    @Test
    fun touchOnAnAlreadyTouchedFieldReturnsTheSameInstance() {
        val touchedOnce = FieldState().touch()

        val touchedTwice = touchedOnce.touch()

        assertSame(touchedOnce, touchedTwice)
    }
}
