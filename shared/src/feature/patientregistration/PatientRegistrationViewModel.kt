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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PatientRegistrationViewModel(
    private val patientRepository: PatientRepository,
    private val navigator: Navigator,
    private val clock: Clock,
) : ViewModel() {
    val patientId = FieldState(validators = listOf(Validators.patientIdFormat()))
    val firstName = FieldState(validators = listOf(Validators.required("First name is required")))
    val lastName = FieldState(validators = listOf(Validators.required("Last name is required")))
    val dateOfBirth = SelectionState<LocalDate>(requiredMessage = "Date of birth is required")
    val gender = SelectionState<Gender>(requiredMessage = "Gender is required")

    private val _submissionOutcome = MutableStateFlow<SubmissionOutcome?>(null)
    val submissionOutcome: StateFlow<SubmissionOutcome?> = _submissionOutcome.asStateFlow()

    val canSubmit: Boolean
        get() = patientId.isValid && firstName.isValid && lastName.isValid && dateOfBirth.isValid && gender.isValid

    fun onSubmit() {
        patientId.touch()
        firstName.touch()
        lastName.touch()
        dateOfBirth.touch()
        gender.touch()

        if (!canSubmit) return

        viewModelScope.launch {
            val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val patient =
                Patient(
                    patientId = PatientId(patientId.value.trim()),
                    registrationDate = today,
                    firstName = FirstName(firstName.value.trim()),
                    lastName = LastName(lastName.value.trim()),
                    dateOfBirth = DateOfBirth(dateOfBirth.selected!!),
                    gender = gender.selected!!,
                )
            val result = patientRepository.registerPatient(patient)
            val outcome = result.toSubmissionOutcome()
            _submissionOutcome.value = outcome
            if (outcome is SubmissionOutcome.Submitted) {
                navigator.goTo(Route.Vitals(patientId = patient.patientId.value))
            }
        }
    }
}
