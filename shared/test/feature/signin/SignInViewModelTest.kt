package ke.don.intellisoft_patient_registration.feature.signin

import ke.don.intellisoft_patient_registration.data.FakeAuthRepository
import ke.don.intellisoft_patient_registration.data.SignInResult
import ke.don.intellisoft_patient_registration.feature.common.SubmissionOutcome
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
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
    fun onSubmitWithValidCredentialsNavigatesToPatientListing() =
        runTest(testDispatcher) {
            val repository = FakeAuthRepository()
            val navigator = Navigator(Route.SignIn)
            val viewModel = SignInViewModel(repository, navigator)

            viewModel.onIntent(SignInIntent.EmailChanged("jane@example.com"))
            viewModel.onIntent(SignInIntent.PasswordChanged("password123"))
            viewModel.onIntent(SignInIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertEquals("jane@example.com", repository.lastSignIn?.email)
            assertEquals(listOf(Route.PatientListing()), navigator.backStack.toList())
        }

    @Test
    fun onSubmitWithAMissingFieldDoesNotCallTheRepositoryAndTouchesEveryField() =
        runTest(testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = SignInViewModel(repository, Navigator(Route.SignIn))

            viewModel.onIntent(SignInIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertNull(repository.lastSignIn)
            assertEquals("Required", viewModel.uiState.value.email.errorMessage)
        }

    @Test
    fun invalidCredentialsSurfacesAsADuplicateSubmissionOutcomeAndDoesNotNavigate() =
        runTest(testDispatcher) {
            val repository = FakeAuthRepository(signInResult = SignInResult.InvalidCredentials)
            val navigator = Navigator(Route.SignIn)
            val viewModel = SignInViewModel(repository, navigator)

            viewModel.onIntent(SignInIntent.EmailChanged("jane@example.com"))
            viewModel.onIntent(SignInIntent.PasswordChanged("wrong"))
            viewModel.onIntent(SignInIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertIs<SubmissionOutcome.Duplicate>(viewModel.uiState.value.submissionOutcome)
            assertEquals(listOf(Route.SignIn), navigator.backStack.toList())
        }
}
