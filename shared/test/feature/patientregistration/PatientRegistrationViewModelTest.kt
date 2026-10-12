package ke.don.intellisoft_patient_registration.feature.patientregistration

import ke.don.intellisoft_patient_registration.data.FakePatientRepository
import ke.don.intellisoft_patient_registration.data.RegisterPatientResult
import ke.don.intellisoft_patient_registration.domain.Gender
import ke.don.intellisoft_patient_registration.domain.PatientId
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
class PatientRegistrationViewModelTest {
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
    fun onSubmitWithAllFieldsValidRegistersPatientAndNavigatesToVitals() =
        runTest(testDispatcher) {
            val repository = FakePatientRepository()
            val navigator = Navigator(Route.PatientRegistration)
            val viewModel =
                PatientRegistrationViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                )
            viewModel.patientId.onValueChanged("P1")
            viewModel.firstName.onValueChanged("Jane")
            viewModel.lastName.onValueChanged("Mdoe")
            viewModel.dateOfBirth.select(LocalDate(2000, 1, 1))
            viewModel.gender.select(Gender.FEMALE)

            viewModel.onSubmit()
            testScheduler.advanceUntilIdle()

            assertEquals(PatientId("P1"), repository.lastRegistered?.patientId)
            assertEquals(
                listOf(Route.PatientRegistration, Route.Vitals(patientId = "P1")),
                navigator.backStack.toList(),
            )
        }

    @Test
    fun onSubmitWithAMissingFieldDoesNotCallTheRepositoryAndTouchesEveryField() =
        runTest(testDispatcher) {
            val repository = FakePatientRepository()
            val navigator = Navigator(Route.PatientRegistration)
            val viewModel =
                PatientRegistrationViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                )

            viewModel.onSubmit()
            testScheduler.advanceUntilIdle()

            assertEquals(null, repository.lastRegistered)
            assertEquals("First name is required", viewModel.firstName.errorMessage)
            assertEquals("Last name is required", viewModel.lastName.errorMessage)
            assertEquals("Date of birth is required", viewModel.dateOfBirth.errorMessage)
            assertEquals("Gender is required", viewModel.gender.errorMessage)
        }

    @Test
    fun duplicatePatientIdSurfacesAsADuplicateSubmissionOutcomeAndDoesNotNavigate() =
        runTest(testDispatcher) {
            val repository = FakePatientRepository(resultToReturn = RegisterPatientResult.DuplicatePatientId)
            val navigator = Navigator(Route.PatientRegistration)
            val viewModel =
                PatientRegistrationViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                )
            viewModel.patientId.onValueChanged("P1")
            viewModel.firstName.onValueChanged("Jane")
            viewModel.lastName.onValueChanged("Mdoe")
            viewModel.dateOfBirth.select(LocalDate(2000, 1, 1))
            viewModel.gender.select(Gender.FEMALE)

            viewModel.onSubmit()
            testScheduler.advanceUntilIdle()

            assertIs<SubmissionOutcome.Duplicate>(viewModel.submissionOutcome.value)
            assertEquals(listOf(Route.PatientRegistration), navigator.backStack.toList())
        }

    private fun fixedClock(date: LocalDate): Clock {
        val epochMs = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs)
        return object : Clock {
            override fun now(): kotlinx.datetime.Instant = instant
        }
    }
}
