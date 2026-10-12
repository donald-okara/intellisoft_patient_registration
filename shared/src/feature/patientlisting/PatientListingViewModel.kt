package ke.don.intellisoft_patient_registration.feature.patientlisting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.PatientListingRepository
import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant

data class PatientListingUiState(
    val selectedDate: LocalDate,
    val rows: List<PatientListingRow> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface PatientListingIntent {
    data class DateSelected(
        val date: LocalDate,
    ) : PatientListingIntent

    data object AddPatientClicked : PatientListingIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
class PatientListingViewModel(
    private val patientListingRepository: PatientListingRepository,
    private val navigator: Navigator,
    private val clock: Clock,
) : ViewModel() {
    private val today: LocalDate
        get() =
            clock
                .now()
                .toStdlibInstant()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

    private val selectedDateFlow = MutableStateFlow(today)

    val uiState: StateFlow<PatientListingUiState> =
        selectedDateFlow
            .flatMapLatest { date ->
                patientListingRepository.getListing(visitDate = date, today = today).map { rows ->
                    PatientListingUiState(
                        selectedDate = date,
                        rows = rows,
                        isLoading = false,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PatientListingUiState(selectedDate = today, isLoading = true),
            )

    fun onIntent(intent: PatientListingIntent) {
        when (intent) {
            is PatientListingIntent.DateSelected -> {
                selectedDateFlow.value = intent.date
            }

            is PatientListingIntent.AddPatientClicked -> {
                navigator.goTo(Route.PatientRegistration)
            }
        }
    }
}
