package ke.don.intellisoft_patient_registration.feature.patientregistration

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import ke.don.intellisoft_patient_registration.feature.common.SelectionState
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Route composable: collects state from [viewModel] and delegates rendering to [PatientRegistrationComponent].
 */
@Composable
fun PatientRegistrationScreen(viewModel: PatientRegistrationViewModel) {
    val submissionOutcome by viewModel.submissionOutcome.collectAsState()

    PatientRegistrationComponent(
        patientId = viewModel.patientId,
        firstName = viewModel.firstName,
        lastName = viewModel.lastName,
        dateOfBirth = viewModel.dateOfBirth,
        gender = viewModel.gender,
        submissionOutcome = submissionOutcome,
        onSubmit = viewModel::onSubmit,
    )
}

/**
 * Component composable: stateless UI layout that receives state and callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRegistrationComponent(
    patientId: FieldState,
    firstName: FieldState,
    lastName: FieldState,
    dateOfBirth: SelectionState<LocalDate>,
    gender: SelectionState<Gender>,
    submissionOutcome: SubmissionOutcome?,
    onSubmit: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Registration") },
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
            submissionOutcome?.let { outcome ->
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

            OutlinedTextField(
                value = patientId.value,
                onValueChange = patientId::onValueChanged,
                label = { Text("Patient ID") },
                isError = patientId.errorMessage != null,
                supportingText = patientId.errorMessage?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = firstName.value,
                onValueChange = firstName::onValueChanged,
                label = { Text("First Name") },
                isError = firstName.errorMessage != null,
                supportingText = firstName.errorMessage?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = lastName.value,
                onValueChange = lastName::onValueChanged,
                label = { Text("Last Name") },
                isError = lastName.errorMessage != null,
                supportingText = lastName.errorMessage?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

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
                    value = dateOfBirth.selected?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Date of Birth") },
                    isError = dateOfBirth.errorMessage != null,
                    supportingText = dateOfBirth.errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = interactionSource,
                )
            }

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
                            selected = gender.selected == option,
                            onClick = { gender.select(option) },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                gender.errorMessage?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text("Register Patient")
            }
        }
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
                            dateOfBirth.select(localDate)
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

@Composable
@Preview
private fun PatientRegistrationScreenPreview() {
    MaterialTheme {
        PatientRegistrationComponent(
            patientId = FieldState(),
            firstName = FieldState(),
            lastName = FieldState(),
            dateOfBirth = SelectionState(),
            gender = SelectionState(),
            submissionOutcome = null,
            onSubmit = {},
        )
    }
}
