package ke.don.intellisoft_patient_registration.feature.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun FieldStateOutlinedTextField(
    state: FieldState,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    readOnly: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    if (interactionSource != null) {
        OutlinedTextField(
            value = state.value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = state.errorMessage != null,
            supportingText = state.errorMessage?.let { { Text(it) } },
            singleLine = singleLine,
            minLines = minLines,
            readOnly = readOnly,
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = modifier,
        )
    } else {
        OutlinedTextField(
            value = state.value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = state.errorMessage != null,
            supportingText = state.errorMessage?.let { { Text(it) } },
            singleLine = singleLine,
            minLines = minLines,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = modifier,
        )
    }
}
