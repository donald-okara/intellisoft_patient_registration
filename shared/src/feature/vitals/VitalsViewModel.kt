package ke.don.intellisoft_patient_registration.feature.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.VitalsRepository
import ke.don.intellisoft_patient_registration.domain.BmiStatus
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import ke.don.intellisoft_patient_registration.domain.calculateBmi
import ke.don.intellisoft_patient_registration.domain.classifyBmi
import ke.don.intellisoft_patient_registration.feature.common.FieldState
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome
import ke.don.intellisoft_patient_registration.feature.common.Validators
import ke.don.intellisoft_patient_registration.feature.common.toSubmissionOutcome
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant

data class VitalsUiState(
    val heightCm: FieldState =
        FieldState(
            validators =
                listOf(
                    Validators.required("Height is required"),
                    Validators.positiveDecimal("Enter a valid height in cm"),
                ),
        ),
    val weightKg: FieldState =
        FieldState(
            validators =
                listOf(
                    Validators.required("Weight is required"),
                    Validators.positiveDecimal("Enter a valid weight in kg"),
                ),
        ),
    val submissionOutcome: SubmissionOutcome? = null,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = heightCm.isValid && weightKg.isValid
}

sealed interface VitalsIntent {
    data class HeightChanged(
        val value: String,
    ) : VitalsIntent

    data class WeightChanged(
        val value: String,
    ) : VitalsIntent

    data object Submit : VitalsIntent
}

class VitalsViewModel(
    private val vitalsRepository: VitalsRepository,
    private val navigator: Navigator,
    private val clock: Clock,
    val patientId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    fun onIntent(intent: VitalsIntent) {
        when (intent) {
            is VitalsIntent.HeightChanged -> {
                reduce {
                    it.copy(heightCm = it.heightCm.onValueChanged(intent.value))
                }
            }

            is VitalsIntent.WeightChanged -> {
                reduce {
                    it.copy(weightKg = it.weightKg.onValueChanged(intent.value))
                }
            }

            is VitalsIntent.Submit -> {
                handleSubmit()
            }
        }
    }

    private fun reduce(reducer: (VitalsUiState) -> VitalsUiState) {
        _uiState.update(reducer)
    }

    private fun handleSubmit() {
        val touchedState =
            _uiState.value.let {
                it.copy(heightCm = it.heightCm.touch(), weightKg = it.weightKg.touch())
            }
        reduce { touchedState }

        if (!touchedState.canSubmit) return

        val height = touchedState.heightCm.value.toDoubleOrNull()
        val weight = touchedState.weightKg.value.toDoubleOrNull()
        if (height == null || weight == null || height <= 0 || weight <= 0) return

        viewModelScope.launch {
            reduce { it.copy(isSubmitting = true) }
            val today =
                clock
                    .now()
                    .toStdlibInstant()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            val bmi = calculateBmi(heightCm = height, weightKg = weight)
            val vitals =
                Vitals(
                    id = null,
                    patientId = PatientId(patientId),
                    visitDate = today,
                    heightCm = height,
                    weightKg = weight,
                    bmi = bmi,
                )

            val result = vitalsRepository.submitVitals(vitals)
            val outcome = result.toSubmissionOutcome()
            reduce { it.copy(submissionOutcome = outcome, isSubmitting = false) }

            if (outcome is SubmissionOutcome.Submitted) {
                val vitalsRows = vitalsRepository.getVitalsForPatient(PatientId(patientId)).first()
                val todayRow = vitalsRows.find { it.visitDate == today }
                val vitalsId = todayRow?.id?.value ?: 0L

                when (classifyBmi(bmi)) {
                    BmiStatus.OVERWEIGHT -> {
                        navigator.goTo(
                            Route.OverweightAssessment(
                                patientId = patientId,
                                vitalsId = vitalsId,
                            ),
                        )
                    }

                    else -> {
                        navigator.goTo(
                            Route.GeneralAssessment(
                                patientId = patientId,
                                vitalsId = vitalsId,
                            ),
                        )
                    }
                }
            }
        }
    }
}
