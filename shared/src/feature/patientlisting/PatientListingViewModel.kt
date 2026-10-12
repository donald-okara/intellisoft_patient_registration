package ke.don.intellisoft_patient_registration.feature.patientlisting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.data.PatientListingRepository
import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant

data class PatientListingUiState(
    val selectedDate: LocalDate,
)

sealed interface PatientListingIntent {
    data class DateSelected(
        val date: LocalDate,
    ) : PatientListingIntent

    data object AddPatientClicked : PatientListingIntent

    data object SignOutClicked : PatientListingIntent
}

/**
 * [pagedRows] is deliberately separate from [uiState] rather than folded into it - PagingData is
 * designed to flow straight to Compose via collectAsLazyPagingItems() and can't be meaningfully
 * held inside a plain data class the way the rest of this project's MVI UiStates are (see
 * CLAUDE.md's MVI rule and the pagination design discussion for why this one screen differs).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatientListingViewModel(
    private val patientListingRepository: PatientListingRepository,
    private val navigator: Navigator,
    private val clock: Clock,
    private val cachedAuthTokenProvider: CachedAuthTokenProvider,
) : ViewModel() {
    private val today: LocalDate
        get() =
            clock
                .now()
                .toStdlibInstant()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

    private val _uiState = MutableStateFlow(PatientListingUiState(selectedDate = today))
    val uiState: StateFlow<PatientListingUiState> = _uiState

    val pagedRows: Flow<PagingData<PatientListingRow>> =
        _uiState
            .map { it.selectedDate }
            .distinctUntilChanged()
            .flatMapLatest { date -> patientListingRepository.getListing(visitDate = date, today = today) }
            .cachedIn(viewModelScope)

    fun onIntent(intent: PatientListingIntent) {
        when (intent) {
            is PatientListingIntent.DateSelected -> {
                _uiState.value = PatientListingUiState(selectedDate = intent.date)
            }

            is PatientListingIntent.AddPatientClicked -> {
                navigator.goTo(Route.PatientRegistration)
            }

            is PatientListingIntent.SignOutClicked -> {
                viewModelScope.launch {
                    cachedAuthTokenProvider.clearToken()
                    navigator.replaceAllWith(Route.SignIn)
                }
            }
        }
    }
}
