package ke.don.intellisoft_patient_registration.feature.generalassessment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.SelectionState
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome

/**
 * Route composable: collects state from [viewModel] and delegates rendering to [GeneralAssessmentComponent].
 */
@Composable
fun GeneralAssessmentScreen(viewModel: GeneralAssessmentViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    GeneralAssessmentComponent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Component composable: stateless UI layout that receives shared UiState and intent callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralAssessmentComponent(
    uiState: GeneralAssessmentUiState,
    onIntent: (GeneralAssessmentIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General Assessment") },
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

            HealthStatusSegment(
                state = uiState.generalHealth,
                onSelected = { onIntent(GeneralAssessmentIntent.HealthStatusSelected(it)) },
            )

            DietQuestionSegment(
                state = uiState.everDietedToLoseWeight,
                onSelected = { onIntent(GeneralAssessmentIntent.EverDietedSelected(it)) },
            )

            CommentsSegment(
                state = uiState.comments,
                onValueChange = { onIntent(GeneralAssessmentIntent.CommentsChanged(it)) },
            )

            Button(
                onClick = { onIntent(GeneralAssessmentIntent.Submit) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(if (uiState.isSubmitting) "Submitting..." else "Submit Assessment")
            }
        }
    }
}

/** Stateless segment for General Health status selection */
@Composable
fun HealthStatusSegment(
    state: SelectionState<HealthStatus>,
    onSelected: (HealthStatus) -> Unit,
) {
    Column {
        Text(
            text = "General Health",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HealthStatus.entries.forEach { option ->
                FilterChip(
                    selected = state.selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        state.errorMessage?.let { errorMsg ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Stateless segment for "Have you ever dieted to lose weight?" question */
@Composable
fun DietQuestionSegment(
    state: SelectionState<Boolean>,
    onSelected: (Boolean) -> Unit,
) {
    Column {
        Text(
            text = "Have you ever dieted to lose weight?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.selected == true,
                onClick = { onSelected(true) },
                label = { Text("Yes") },
            )
            FilterChip(
                selected = state.selected == false,
                onClick = { onSelected(false) },
                label = { Text("No") },
            )
        }
        state.errorMessage?.let { errorMsg ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Stateless segment for Comments field */
@Composable
fun CommentsSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = onValueChange,
        label = { Text("Comments") },
        isError = state.errorMessage != null,
        supportingText = state.errorMessage?.let { { Text(it) } },
        minLines = 3,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
@Preview
private fun GeneralAssessmentScreenPreview() {
    MaterialTheme {
        GeneralAssessmentComponent(
            uiState = GeneralAssessmentUiState(),
            onIntent = {},
        )
    }
}
