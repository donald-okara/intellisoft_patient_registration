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
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.data.RegisterPatientResult
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRegistrationScreen(viewModel: PatientRegistrationViewModel) {
    val submissionOutcome by viewModel.submissionOutcome.collectAsState()
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
                value = viewModel.patientId.value,
                onValueChange = viewModel.patientId::onValueChanged,
                label = { Text("Patient ID") },
                isError = viewModel.patientId.errorMessage != null,
                supportingText = viewModel.patientId.errorMessage?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = viewModel.firstName.value,
                onValueChange = viewModel.firstName::onValueChanged,
                label = { Text("First Name") },
                isError = viewModel.firstName.errorMessage != null,
                supportingText = viewModel.firstName.errorMessage?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = viewModel.lastName.value,
                onValueChange = viewModel.lastName::onValueChanged,
                label = { Text("Last Name") },
                isError = viewModel.lastName.errorMessage != null,
                supportingText = viewModel.lastName.errorMessage?.let { { Text(it) } },
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
                    value = viewModel.dateOfBirth.selected?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Date of Birth") },
                    isError = viewModel.dateOfBirth.errorMessage != null,
                    supportingText = viewModel.dateOfBirth.errorMessage?.let { { Text(it) } },
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
                            selected = viewModel.gender.selected == option,
                            onClick = { viewModel.gender.select(option) },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                viewModel.gender.errorMessage?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = viewModel::onSubmit,
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
                            viewModel.dateOfBirth.select(localDate)
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
    val dummyRepository =
        remember {
            object : PatientRepository {
                override suspend fun registerPatient(patient: Patient): RegisterPatientResult =
                    RegisterPatientResult.Success(synced = true)

                override fun getPatient(id: PatientId): Flow<Patient?> = flowOf(null)

                override fun getPatients(
                    page: Int,
                    pageSize: Int,
                ): Flow<List<Patient>> = flowOf(emptyList())

                override suspend fun retryUnsyncedPushes() {}

                override suspend fun sync(): Boolean = true
            }
        }
    val dummyNavigator = remember { Navigator(Route.PatientRegistration) }
    val viewModel =
        remember {
            PatientRegistrationViewModel(
                patientRepository = dummyRepository,
                navigator = dummyNavigator,
                clock = Clock.System,
            )
        }

    MaterialTheme {
        PatientRegistrationScreen(viewModel = viewModel)
    }
}
