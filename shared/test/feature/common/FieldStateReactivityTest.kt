package ke.don.intellisoft_patient_registration.feature.common

import kotlin.test.Test
import kotlin.test.assertNotEquals

data class ScratchUiState(
    val email: FieldState = FieldState(),
)

/**
 * Regression test for a real bug: mutating FieldState in place and reusing the same reference
 * in `copy()` made the "old" and "new" wrapping UiState structurally equal, which made
 * StateFlow conflate the update away - Compose's collectAsState() never saw a new emission, so
 * text fields appeared frozen even though the value had changed.
 */
class FieldStateReactivityTest {
    @Test
    fun copyWithAMutatedFieldStateIsNeverEqualToTheOriginal() {
        val original = ScratchUiState()

        val mutatedCopy = original.copy(email = original.email.apply { onValueChanged("a") })

        assertNotEquals(original, mutatedCopy)
    }
}
