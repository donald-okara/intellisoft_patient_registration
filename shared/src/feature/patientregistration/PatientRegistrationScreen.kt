package ke.don.intellisoft_patient_registration.feature.patientregistration

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.FieldStateOutlinedTextField
import ke.don.intellisoft_patient_registration.feature.common.FormScreenColumn
import ke.don.intellisoft_patient_registration.feature.common.SelectionState
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Route composable: collects state from [viewModel] and delegates rendering to [PatientRegistrationComponent].
 */
@Composable
fun PatientRegistrationScreen(viewModel: PatientRegistrationViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    PatientRegistrationComponent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Component composable: stateless UI layout that receives shared UiState and intent callbacks.
 */
@Composable
fun PatientRegistrationComponent(
    uiState: PatientRegistrationUiState,
    onIntent: (PatientRegistrationIntent) -> Unit,
) {
    FormScreenColumn(
        title = "Patient Registration",
        outcome = uiState.submissionOutcome,
    ) {
        PatientIdSegment(
            state = uiState.patientId,
            onValueChange = { onIntent(PatientRegistrationIntent.PatientIdChanged(it)) },
        )

        FirstNameSegment(
            state = uiState.firstName,
            onValueChange = { onIntent(PatientRegistrationIntent.FirstNameChanged(it)) },
        )

        LastNameSegment(
            state = uiState.lastName,
            onValueChange = { onIntent(PatientRegistrationIntent.LastNameChanged(it)) },
        )

        DateOfBirthSegment(
            state = uiState.dateOfBirth,
            onDateSelected = { onIntent(PatientRegistrationIntent.DateOfBirthSelected(it)) },
        )

        GenderSegment(
            state = uiState.gender,
            onGenderSelected = { onIntent(PatientRegistrationIntent.GenderSelected(it)) },
        )

        Button(
            onClick = { onIntent(PatientRegistrationIntent.Submit) },
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(if (uiState.isSubmitting) "Registering..." else "Register Patient")
        }
    }
}

/** Stateless segment for Patient ID field */
@Composable
fun PatientIdSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = "Patient ID",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

/** Stateless segment for First Name field */
@Composable
fun FirstNameSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = "First Name",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

/** Stateless segment for Last Name field */
@Composable
fun LastNameSegment(
    state: FieldState,
    onValueChange: (String) -> Unit,
) {
    FieldStateOutlinedTextField(
        state = state,
        onValueChange = onValueChange,
        label = "Last Name",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}

/** Stateless segment for Date of Birth field */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOfBirthSegment(
    state: SelectionState<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Date of Birth",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        val interactionSource = remember { MutableInteractionSource() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDatePicker = true
                }
            }
        }
        OutlinedTextField(
            value = state.selected?.toString() ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Date of Birth") },
            isError = state.errorMessage != null,
            supportingText = state.errorMessage?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate =
                                Instant
                                    .fromEpochMilliseconds(millis)
                                    .toLocalDateTime(TimeZone.UTC)
                                    .date
                            onDateSelected(localDate)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Stateless segment for Gender selection */
@Composable
fun GenderSegment(
    state: SelectionState<Gender>,
    onGenderSelected: (Gender) -> Unit,
) {
    Column {
        Text(
            text = "Gender",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Gender.entries.forEach { option ->
                FilterChip(
                    selected = state.selected == option,
                    onClick = { onGenderSelected(option) },
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

@Composable
@Preview
private fun PatientRegistrationScreenPreview() {
    MaterialTheme {
        PatientRegistrationComponent(
            uiState = PatientRegistrationUiState(),
            onIntent = {},
        )
    }
}
