package ke.don.intellisoft_patient_registration.feature.vitals

import ke.don.intellisoft_patient_registration.data.FakeVitalsRepository
import ke.don.intellisoft_patient_registration.data.SubmitVitalsResult
import ke.don.intellisoft_patient_registration.domain.PatientId
import ke.don.intellisoft_patient_registration.domain.Vitals
import ke.don.intellisoft_patient_registration.domain.VitalsId
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class VitalsViewModelTest {
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
    fun onSubmitWithBmiUnder25NavigatesToGeneralAssessment() =
        runTest(testDispatcher) {
            val repository = FakeVitalsRepository()
            val navigator = Navigator(Route.Vitals(patientId = "P1"))
            val today = LocalDate(2026, 10, 12)
            val viewModel =
                VitalsViewModel(
                    repository,
                    navigator,
                    fixedClock(today),
                    patientId = "P1",
                )
            repository.stubVitalsForPatient(
                listOf(
                    Vitals(
                        id = VitalsId(7L),
                        patientId = PatientId("P1"),
                        visitDate = today,
                        heightCm = 180.0,
                        weightKg = 70.0,
                        bmi = 21.6,
                    ),
                ),
            )

            viewModel.onIntent(VitalsIntent.HeightChanged("180"))
            viewModel.onIntent(VitalsIntent.WeightChanged("70"))
            viewModel.onIntent(VitalsIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(Route.Vitals("P1"), Route.GeneralAssessment(patientId = "P1", vitalsId = 7L)),
                navigator.backStack.toList(),
            )
        }

    @Test
    fun onSubmitWithBmiExactly25NavigatesToOverweightAssessment() =
        runTest(testDispatcher) {
            val repository = FakeVitalsRepository()
            val navigator = Navigator(Route.Vitals(patientId = "P1"))
            val today = LocalDate(2026, 10, 12)
            val viewModel =
                VitalsViewModel(
                    repository,
                    navigator,
                    fixedClock(today),
                    patientId = "P1",
                )
            repository.stubVitalsForPatient(
                listOf(
                    Vitals(
                        id = VitalsId(8L),
                        patientId = PatientId("P1"),
                        visitDate = today,
                        heightCm = 180.0,
                        weightKg = 81.0,
                        bmi = 25.0,
                    ),
                ),
            )

            viewModel.onIntent(VitalsIntent.HeightChanged("180"))
            viewModel.onIntent(VitalsIntent.WeightChanged("81.0"))
            viewModel.onIntent(VitalsIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertEquals(
                listOf(Route.Vitals("P1"), Route.OverweightAssessment(patientId = "P1", vitalsId = 8L)),
                navigator.backStack.toList(),
            )
        }

    @Test
    fun nonNumericHeightFailsValidationAndNeverCallsTheRepository() =
        runTest(testDispatcher) {
            val repository = FakeVitalsRepository()
            val navigator = Navigator(Route.Vitals(patientId = "P1"))
            val viewModel =
                VitalsViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                    patientId = "P1",
                )

            viewModel.onIntent(VitalsIntent.HeightChanged("abc"))
            viewModel.onIntent(VitalsIntent.WeightChanged("70"))
            viewModel.onIntent(VitalsIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertEquals(null, repository.lastSubmitted)
            assertEquals("Enter a valid height in cm", viewModel.uiState.value.heightCm.errorMessage)
        }

    @Test
    fun duplicateVisitDateBlocksNavigation() =
        runTest(testDispatcher) {
            val repository = FakeVitalsRepository(resultToReturn = SubmitVitalsResult.DuplicateVisitDate)
            val navigator = Navigator(Route.Vitals(patientId = "P1"))
            val viewModel =
                VitalsViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                    patientId = "P1",
                )

            viewModel.onIntent(VitalsIntent.HeightChanged("180"))
            viewModel.onIntent(VitalsIntent.WeightChanged("70"))
            viewModel.onIntent(VitalsIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertIs<SubmissionOutcome.Duplicate>(viewModel.uiState.value.submissionOutcome)
            assertEquals(listOf(Route.Vitals("P1")), navigator.backStack.toList())
        }

    private fun fixedClock(date: LocalDate): Clock {
        val epochMs = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs)
        return object : Clock {
            override fun now(): kotlinx.datetime.Instant = instant
        }
    }
}
