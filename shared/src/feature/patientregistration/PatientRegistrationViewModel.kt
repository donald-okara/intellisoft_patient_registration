package ke.don.intellisoft_patient_registration.feature.patientregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.domain.DateOfBirth
import ke.don.intellisoft_patient_registration.domain.FirstName
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.domain.LastName
import ke.don.intellisoft_patient_registration.domain.Patient
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.SelectionState
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome
import ke.don.intellisoft_patient_registration.feature.common.Validators
import ke.don.intellisoft_patient_registration.feature.common.toSubmissionOutcome
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant

data class PatientRegistrationUiState(
    val patientId: FieldState = FieldState(validators = listOf(Validators.patientIdFormat())),
    val firstName: FieldState = FieldState(validators = listOf(Validators.required("First name is required"))),
    val lastName: FieldState = FieldState(validators = listOf(Validators.required("Last name is required"))),
    val dateOfBirth: SelectionState<LocalDate> = SelectionState(requiredMessage = "Date of birth is required"),
    val gender: SelectionState<Gender> = SelectionState(requiredMessage = "Gender is required"),
    val submissionOutcome: SubmissionOutcome? = null,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = patientId.isValid && firstName.isValid && lastName.isValid && dateOfBirth.isValid && gender.isValid
}

sealed interface PatientRegistrationIntent {
    data class PatientIdChanged(
        val value: String,
    ) : PatientRegistrationIntent

    data class FirstNameChanged(
        val value: String,
    ) : PatientRegistrationIntent

    data class LastNameChanged(
        val value: String,
    ) : PatientRegistrationIntent

    data class DateOfBirthSelected(
        val value: LocalDate,
    ) : PatientRegistrationIntent

    data class GenderSelected(
        val value: Gender,
    ) : PatientRegistrationIntent

    data object Submit : PatientRegistrationIntent
}

class PatientRegistrationViewModel(
    private val patientRepository: PatientRepository,
    private val navigator: Navigator,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PatientRegistrationUiState())
    val uiState: StateFlow<PatientRegistrationUiState> = _uiState.asStateFlow()

    fun onIntent(intent: PatientRegistrationIntent) {
        when (intent) {
            is PatientRegistrationIntent.PatientIdChanged -> {
                reduce {
                    it.copy(patientId = it.patientId.apply { onValueChanged(intent.value) })
                }
            }

            is PatientRegistrationIntent.FirstNameChanged -> {
                reduce {
                    it.copy(firstName = it.firstName.apply { onValueChanged(intent.value) })
                }
            }

            is PatientRegistrationIntent.LastNameChanged -> {
                reduce {
                    it.copy(lastName = it.lastName.apply { onValueChanged(intent.value) })
                }
            }

            is PatientRegistrationIntent.DateOfBirthSelected -> {
                reduce {
                    it.copy(dateOfBirth = it.dateOfBirth.apply { select(intent.value) })
                }
            }

            is PatientRegistrationIntent.GenderSelected -> {
                reduce {
                    it.copy(gender = it.gender.apply { select(intent.value) })
                }
            }

            is PatientRegistrationIntent.Submit -> {
                handleSubmit()
            }
        }
    }

    private fun reduce(reducer: (PatientRegistrationUiState) -> PatientRegistrationUiState) {
        _uiState.update(reducer)
    }

    private fun handleSubmit() {
        val currentState = _uiState.value
        currentState.patientId.touch()
        currentState.firstName.touch()
        currentState.lastName.touch()
        currentState.dateOfBirth.touch()
        currentState.gender.touch()

        reduce { it.copy() }

        if (!currentState.canSubmit) return

        viewModelScope.launch {
            reduce { it.copy(isSubmitting = true) }
            val today = clock.now().toStdlibInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val patient =
                Patient(
                    patientId = PatientId(currentState.patientId.value.trim()),
                    registrationDate = today,
                    firstName = FirstName(currentState.firstName.value.trim()),
                    lastName = LastName(currentState.lastName.value.trim()),
                    dateOfBirth = DateOfBirth(currentState.dateOfBirth.selected!!),
                    gender = currentState.gender.selected!!,
                )
            val result = patientRepository.registerPatient(patient)
            val outcome = result.toSubmissionOutcome()
            reduce { it.copy(submissionOutcome = outcome, isSubmitting = false) }
            if (outcome is SubmissionOutcome.Submitted) {
                navigator.goTo(Route.Vitals(patientId = patient.patientId.value))
            }
        }
    }
}
