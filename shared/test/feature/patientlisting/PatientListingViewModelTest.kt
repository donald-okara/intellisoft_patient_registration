package ke.don.intellisoft_patient_registration.feature.patientlisting

import ke.don.intellisoft_patient_registration.data.FakePatientListingRepository
import ke.don.intellisoft_patient_registration.domain.BmiStatus
import ke.don.intellisoft_patient_registration.domain.PatientListingRow
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PatientListingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultsToTodayAndLoadsTodaysRows() =
        runTest(testDispatcher) {
            val repository = FakePatientListingRepository()
            val today = LocalDate(2026, 10, 12)
            val row1 = PatientListingRow(name = "Jane Mdoe", age = 26, bmi = 21.6, status = BmiStatus.NORMAL)
            repository.stubRowsForDate(today, listOf(row1))

            val navigator = Navigator(Route.PatientListing())
            val viewModel =
                PatientListingViewModel(
                    repository,
                    navigator,
                    fixedClock(today),
                )

            val state = viewModel.uiState.first { !it.isLoading }
            assertEquals(today, state.selectedDate)
            assertEquals(listOf(row1), state.rows)
        }

    @Test
    fun changingTheDateReloadsAndReplacesTheRowsEntirely() =
        runTest(testDispatcher) {
            val repository = FakePatientListingRepository()
            val today = LocalDate(2026, 10, 12)
            val yesterday = LocalDate(2026, 10, 11)
            val rowToday = PatientListingRow(name = "Jane Mdoe", age = 26, bmi = 21.6, status = BmiStatus.NORMAL)
            val rowYesterday = PatientListingRow(name = "John Doe", age = 30, bmi = 27.5, status = BmiStatus.OVERWEIGHT)

            repository.stubRowsForDate(today, listOf(rowToday))
            repository.stubRowsForDate(yesterday, listOf(rowYesterday))

            val navigator = Navigator(Route.PatientListing())
            val viewModel =
                PatientListingViewModel(
                    repository,
                    navigator,
                    fixedClock(today),
                )

            backgroundScope.launch { viewModel.uiState.collect {} }
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(PatientListingIntent.DateSelected(yesterday))
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(yesterday, state.selectedDate)
            assertEquals(listOf(rowYesterday), state.rows)
        }

    @Test
    fun onAddPatientClickedNavigatesToPatientRegistration() =
        runTest(testDispatcher) {
            val repository = FakePatientListingRepository()
            val navigator = Navigator(Route.PatientListing())
            val viewModel =
                PatientListingViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                )

            viewModel.onIntent(PatientListingIntent.AddPatientClicked)
            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(Route.PatientListing(), Route.PatientRegistration),
                navigator.backStack.toList(),
            )
        }

    private fun fixedClock(date: LocalDate): Clock {
        val epochMs = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs)
        return object : Clock {
            override fun now(): kotlinx.datetime.Instant = instant
        }
    }
}
