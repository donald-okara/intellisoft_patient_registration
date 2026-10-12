package ke.don.intellisoft_patient_registration.feature.overweightassessment

import ke.don.intellisoft_patient_registration.data.FakeOverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.SubmitOverweightAssessmentResult
import ke.don.intellisoft_patient_registration.domain.HealthStatus
import ke.don.intellisoft_patient_registration.domain.PatientId
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
class OverweightAssessmentViewModelTest {
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
    fun onSubmitWithAllFieldsValidSubmitsAndReplacesBackStackWithPatientListing() =
        runTest(testDispatcher) {
            val repository = FakeOverweightAssessmentRepository()
            val navigator = Navigator(Route.OverweightAssessment(patientId = "P1", vitalsId = 8L))
            val viewModel =
                OverweightAssessmentViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                    patientId = "P1",
                    vitalsId = 8L,
                )

            viewModel.onIntent(OverweightAssessmentIntent.HealthStatusSelected(HealthStatus.GOOD))
            viewModel.onIntent(OverweightAssessmentIntent.CurrentlyUsingDrugsSelected(false))
            viewModel.onIntent(OverweightAssessmentIntent.CommentsChanged("Feeling healthy"))
            viewModel.onIntent(OverweightAssessmentIntent.Submit)
            testScheduler.advanceUntilIdle()

            val lastSubmitted = repository.lastSubmitted
            assertEquals(PatientId("P1"), lastSubmitted?.patientId)
            assertEquals(VitalsId(8L), lastSubmitted?.vitalsId)
            assertEquals(HealthStatus.GOOD, lastSubmitted?.generalHealth)
            assertEquals(false, lastSubmitted?.currentlyUsingDrugs)
            assertEquals("Feeling healthy", lastSubmitted?.comments?.value)
            assertEquals(listOf(Route.PatientListing()), navigator.backStack.toList())
        }

    @Test
    fun onSubmitWithNoCommentsFailsValidationAndTouchesEveryField() =
        runTest(testDispatcher) {
            val repository = FakeOverweightAssessmentRepository()
            val navigator = Navigator(Route.OverweightAssessment(patientId = "P1", vitalsId = 8L))
            val viewModel =
                OverweightAssessmentViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                    patientId = "P1",
                    vitalsId = 8L,
                )

            viewModel.onIntent(OverweightAssessmentIntent.Submit)
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(null, repository.lastSubmitted)
            assertEquals("General health status is required", state.generalHealth.errorMessage)
            assertEquals("Drug question is required", state.currentlyUsingDrugs.errorMessage)
            assertEquals("Comments are required", state.comments.errorMessage)
        }

    @Test
    fun duplicateVisitDateBlocksNavigation() =
        runTest(testDispatcher) {
            val repository =
                FakeOverweightAssessmentRepository(
                    resultToReturn = SubmitOverweightAssessmentResult.DuplicateVisitDate,
                )
            val navigator = Navigator(Route.OverweightAssessment(patientId = "P1", vitalsId = 8L))
            val viewModel =
                OverweightAssessmentViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                    patientId = "P1",
                    vitalsId = 8L,
                )

            viewModel.onIntent(OverweightAssessmentIntent.HealthStatusSelected(HealthStatus.GOOD))
            viewModel.onIntent(OverweightAssessmentIntent.CurrentlyUsingDrugsSelected(true))
            viewModel.onIntent(OverweightAssessmentIntent.CommentsChanged("No comments"))
            viewModel.onIntent(OverweightAssessmentIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertIs<SubmissionOutcome.Duplicate>(viewModel.uiState.value.submissionOutcome)
            assertEquals(
                listOf(Route.OverweightAssessment(patientId = "P1", vitalsId = 8L)),
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
