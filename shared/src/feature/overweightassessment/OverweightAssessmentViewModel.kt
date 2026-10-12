package ke.don.intellisoft_patient_registration.feature.overweightassessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.domain.Comments
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.OverweightAssessment
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

data class OverweightAssessmentUiState(
    val generalHealth: SelectionState<HealthStatus> =
        SelectionState(requiredMessage = "General health status is required"),
    val currentlyUsingDrugs: SelectionState<Boolean> =
        SelectionState(requiredMessage = "Drug question is required"),
    val comments: FieldState =
        FieldState(validators = listOf(Validators.required("Comments are required"))),
    val submissionOutcome: SubmissionOutcome? = null,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = generalHealth.isValid && currentlyUsingDrugs.isValid && comments.isValid
}

sealed interface OverweightAssessmentIntent {
    data class HealthStatusSelected(
        val value: HealthStatus,
    ) : OverweightAssessmentIntent

    data class CurrentlyUsingDrugsSelected(
        val value: Boolean,
    ) : OverweightAssessmentIntent

    data class CommentsChanged(
        val value: String,
    ) : OverweightAssessmentIntent

    data object Submit : OverweightAssessmentIntent
}

class OverweightAssessmentViewModel(
    private val overweightAssessmentRepository: OverweightAssessmentRepository,
    private val navigator: Navigator,
    private val clock: Clock,
    val patientId: String,
    val vitalsId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OverweightAssessmentUiState())
    val uiState: StateFlow<OverweightAssessmentUiState> = _uiState.asStateFlow()

    fun onIntent(intent: OverweightAssessmentIntent) {
        when (intent) {
            is OverweightAssessmentIntent.HealthStatusSelected -> {
                reduce {
                    it.copy(generalHealth = it.generalHealth.select(intent.value))
                }
            }

            is OverweightAssessmentIntent.CurrentlyUsingDrugsSelected -> {
                reduce {
                    it.copy(currentlyUsingDrugs = it.currentlyUsingDrugs.select(intent.value))
                }
            }

            is OverweightAssessmentIntent.CommentsChanged -> {
                reduce {
                    it.copy(comments = it.comments.onValueChanged(intent.value))
                }
            }

            is OverweightAssessmentIntent.Submit -> {
                handleSubmit()
            }
        }
    }

    private fun reduce(reducer: (OverweightAssessmentUiState) -> OverweightAssessmentUiState) {
        _uiState.update(reducer)
    }

    private fun handleSubmit() {
        val touchedState =
            _uiState.value.let {
                it.copy(
                    generalHealth = it.generalHealth.touch(),
                    currentlyUsingDrugs = it.currentlyUsingDrugs.touch(),
                    comments = it.comments.touch(),
                )
            }
        reduce { touchedState }

        if (!touchedState.canSubmit) return

        viewModelScope.launch {
            reduce { it.copy(isSubmitting = true) }
            val today =
                clock
                    .now()
                    .toStdlibInstant()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date

            val assessment =
                OverweightAssessment(
                    id = null,
                    patientId = PatientId(patientId),
                    vitalsId = VitalsId(vitalsId),
                    visitDate = today,
                    generalHealth = touchedState.generalHealth.selected!!,
                    currentlyUsingDrugs = touchedState.currentlyUsingDrugs.selected!!,
                    comments = Comments(touchedState.comments.value.trim()),
                )

            val result = overweightAssessmentRepository.submitOverweightAssessment(assessment)
            val outcome = result.toSubmissionOutcome()
            reduce { it.copy(submissionOutcome = outcome, isSubmitting = false) }

            if (outcome is SubmissionOutcome.Submitted) {
                navigator.replaceAllWith(Route.PatientListing())
            }
        }
    }
}
