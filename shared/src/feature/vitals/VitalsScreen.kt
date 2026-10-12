package ke.don.intellisoft_patient_registration.feature.vitals

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.FieldStateOutlinedTextField
import ke.don.intellisoft_patient_registration.feature.common.FormScreenColumn

/**
 * Route composable: collects state from [viewModel] and delegates rendering to [VitalsComponent].
 */
@Composable
fun VitalsScreen(viewModel: VitalsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    VitalsComponent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Component composable: stateless UI layout that receives shared UiState and intent callbacks.
 */
@Composable
fun VitalsComponent(
    uiState: VitalsUiState,
    onIntent: (VitalsIntent) -> Unit,
) {
    FormScreenColumn(
        title = "Capture Vitals",
        outcome = uiState.submissionOutcome,
    ) {
        HeightSegment(
            state = uiState.heightCm,
            onValueChange = { onIntent(VitalsIntent.HeightChanged(it)) },
        )

        WeightSegment(
            state = uiState.weightKg,
            onValueChange = { onIntent(VitalsIntent.WeightChanged(it)) },
        )

        Button(
            onClick = { onIntent(VitalsIntent.Submit) },
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (uiState.isSubmitting) "Submitting..." else "Submit Vitals")
        }
    }
}

/** Stateless segment for Height field */
@Composable
fun HeightSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = "Height (cm)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}

/** Stateless segment for Weight field */
@Composable
fun WeightSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = "Weight (kg)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    )
}

@Composable
@Preview
private fun VitalsScreenPreview() {
    MaterialTheme {
        VitalsComponent(
            uiState = VitalsUiState(),
            onIntent = {},
        )
    }
}

@Composable
@Preview
private fun VitalsScreenPreview_WithDuplicateError() {
    MaterialTheme {
        VitalsComponent(
            uiState =
                VitalsUiState(
                    submissionOutcome =
                        ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome.Duplicate(
                            "Vitals have already been submitted for this patient today.",
                        ),
                ),
            onIntent = {},
        )
    }
}
