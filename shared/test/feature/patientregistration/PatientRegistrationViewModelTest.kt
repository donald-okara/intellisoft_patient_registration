package ke.don.intellisoft_patient_registration.feature.patientregistration

import ke.don.intellisoft_patient_registration.data.FakePatientRepository
import ke.don.intellisoft_patient_registration.data.RegisterPatientResult
import ke.don.intellisoft_patient_registration.domain.Gender
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
import kotlin.test.assertNotNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    /**
     * There's no field value to assert against anymore - patientId is generated, not typed - so
     * this instead proves it's a real UUID and that the exact same id both reaches the repository
     * and drives the Vitals route, rather than two independently-generated values.
     */
    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun onSubmitWithAllFieldsValidRegistersPatientWithAGeneratedIdAndNavigatesToVitals() =
        runTest(testDispatcher) {
            val repository = FakePatientRepository()
            val navigator = Navigator(Route.PatientRegistration)
            val viewModel =
                PatientRegistrationViewModel(
                    repository,
                    navigator,
                    fixedClock(LocalDate(2026, 10, 12)),
                )
            viewModel.onIntent(PatientRegistrationIntent.FirstNameChanged("Jane"))
            viewModel.onIntent(PatientRegistrationIntent.LastNameChanged("Mdoe"))
            viewModel.onIntent(PatientRegistrationIntent.DateOfBirthSelected(LocalDate(2000, 1, 1)))
            viewModel.onIntent(PatientRegistrationIntent.GenderSelected(Gender.FEMALE))

            viewModel.onIntent(PatientRegistrationIntent.Submit)
            testScheduler.advanceUntilIdle()

            val registeredId = repository.lastRegistered?.patientId
            assertNotNull(registeredId)
            Uuid.parse(registeredId.value)
            assertEquals(
                listOf(Route.PatientRegistration, Route.Vitals(patientId = registeredId.value)),
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

            viewModel.onIntent(PatientRegistrationIntent.Submit)
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(null, repository.lastRegistered)
            assertEquals("First name is required", state.firstName.errorMessage)
            assertEquals("Last name is required", state.lastName.errorMessage)
            assertEquals("Date of birth is required", state.dateOfBirth.errorMessage)
            assertEquals("Gender is required", state.gender.errorMessage)
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
            viewModel.onIntent(PatientRegistrationIntent.FirstNameChanged("Jane"))
            viewModel.onIntent(PatientRegistrationIntent.LastNameChanged("Mdoe"))
            viewModel.onIntent(PatientRegistrationIntent.DateOfBirthSelected(LocalDate(2000, 1, 1)))
            viewModel.onIntent(PatientRegistrationIntent.GenderSelected(Gender.FEMALE))

            viewModel.onIntent(PatientRegistrationIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertIs<SubmissionOutcome.Duplicate>(viewModel.uiState.value.submissionOutcome)
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
