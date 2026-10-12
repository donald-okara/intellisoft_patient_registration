package ke.don.intellisoft_patient_registration.feature.generalassessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.GeneralAssessment
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.VitalsId
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant

data class GeneralAssessmentUiState(
    val generalHealth: SelectionState<HealthStatus> =
        SelectionState(requiredMessage = "General health status is required"),
    val everDietedToLoseWeight: SelectionState<Boolean> =
        SelectionState(requiredMessage = "Diet question is required"),
    val comments: FieldState =
        FieldState(validators = listOf(Validators.required("Comments are required"))),
    val submissionOutcome: SubmissionOutcome? = null,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = generalHealth.isValid && everDietedToLoseWeight.isValid && comments.isValid
}

sealed interface GeneralAssessmentIntent {
    data class HealthStatusSelected(
        val value: HealthStatus,
    ) : GeneralAssessmentIntent

    data class EverDietedSelected(
        val value: Boolean,
    ) : GeneralAssessmentIntent

    data class CommentsChanged(
        val value: String,
    ) : GeneralAssessmentIntent

    data object Submit : GeneralAssessmentIntent
}

class GeneralAssessmentViewModel(
    private val generalAssessmentRepository: GeneralAssessmentRepository,
    private val navigator: Navigator,
    private val clock: Clock,
    val patientId: String,
    val vitalsId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GeneralAssessmentUiState())
    val uiState: StateFlow<GeneralAssessmentUiState> = _uiState.asStateFlow()

    fun onIntent(intent: GeneralAssessmentIntent) {
        when (intent) {
            is GeneralAssessmentIntent.HealthStatusSelected -> {
                reduce {
                    it.copy(generalHealth = it.generalHealth.apply { select(intent.value) })
                }
            }

            is GeneralAssessmentIntent.EverDietedSelected -> {
                reduce {
                    it.copy(
                        everDietedToLoseWeight =
                            it.everDietedToLoseWeight.apply { select(intent.value) },
                    )
                }
            }

            is GeneralAssessmentIntent.CommentsChanged -> {
                reduce {
                    it.copy(comments = it.comments.apply { onValueChanged(intent.value) })
                }
            }

            is GeneralAssessmentIntent.Submit -> {
                handleSubmit()
            }
        }
    }

    private fun reduce(reducer: (GeneralAssessmentUiState) -> GeneralAssessmentUiState) {
        _uiState.update(reducer)
    }

    private fun handleSubmit() {
        val currentState = _uiState.value
        currentState.generalHealth.touch()
        currentState.everDietedToLoseWeight.touch()
        currentState.comments.touch()

        reduce { it.copy() }

        if (!currentState.canSubmit) return

        viewModelScope.launch {
            reduce { it.copy(isSubmitting = true) }
            val today =
                clock
                    .now()
                    .toStdlibInstant()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date

            val assessment =
                GeneralAssessment(
                    id = null,
                    patientId = PatientId(patientId),
                    vitalsId = VitalsId(vitalsId),
                    visitDate = today,
                    generalHealth = currentState.generalHealth.selected!!,
                    everDietedToLoseWeight = currentState.everDietedToLoseWeight.selected!!,
                    comments = Comments(currentState.comments.value.trim()),
                )

            val result = generalAssessmentRepository.submitGeneralAssessment(assessment)
            val outcome = result.toSubmissionOutcome()
            reduce { it.copy(submissionOutcome = outcome, isSubmitting = false) }

            if (outcome is SubmissionOutcome.Submitted) {
                navigator.replaceAllWith(Route.PatientListing())
            }
        }
    }
}
