package ke.don.intellisoft_patient_registration.feature.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Shared by SignIn/SignUp (both had their own copy before this) - visibility is local, one-shot
 * UI presentation state, not form data, so it lives in `remember` rather than the screen's
 * UiState: toggling it has no validation/business-logic meaning and resets harmlessly on
 * recomposition loss (e.g. process death), same as any other transient UI affordance.
 */
@Composable
fun PasswordSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
    label: String = "Password",
) {
    var isVisible by remember { mutableStateOf(false) }

    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { isVisible = !isVisible }) {
                Text(if (isVisible) "Hide" else "Show")
            }
        },
    )
}
