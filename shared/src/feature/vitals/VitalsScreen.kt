package ke.don.intellisoft_patient_registration.feature.vitals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsComponent(
    uiState: VitalsUiState,
    onIntent: (VitalsIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Vitals") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.submissionOutcome?.let { outcome ->
                when (outcome) {
                    is SubmissionOutcome.Duplicate -> {
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = outcome.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    is SubmissionOutcome.Failed -> {
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = outcome.cause.message ?: "An unexpected error occurred.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    else -> {}
                }
            }

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
}

/** Stateless segment for Height field */
@Composable
fun HeightSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = onValueChange,
        label = { Text("Height (cm)") },
        isError = state.errorMessage != null,
        supportingText = state.errorMessage?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Stateless segment for Weight field */
@Composable
fun WeightSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = onValueChange,
        label = { Text("Weight (kg)") },
        isError = state.errorMessage != null,
        supportingText = state.errorMessage?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
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
