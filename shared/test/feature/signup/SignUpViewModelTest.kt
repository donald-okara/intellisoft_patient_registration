package ke.don.intellisoft_patient_registration.feature.signup

import ke.don.intellisoft_patient_registration.data.FakeAuthRepository
import ke.don.intellisoft_patient_registration.data.SignUpResult
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
class SignUpViewModelTest {
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
    fun onSubmitWithAllFieldsValidNavigatesToSignIn() =
        runTest(testDispatcher) {
            val repository = FakeAuthRepository()
            val navigator = Navigator(Route.SignUp)
            val viewModel = SignUpViewModel(repository, navigator)

            viewModel.onIntent(SignUpIntent.EmailChanged("jane@example.com"))
            viewModel.onIntent(SignUpIntent.FirstnameChanged("Jane"))
            viewModel.onIntent(SignUpIntent.LastnameChanged("Mdoe"))
            viewModel.onIntent(SignUpIntent.PasswordChanged("password123"))
            viewModel.onIntent(SignUpIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertEquals("jane@example.com", repository.lastSignUp?.email)
            assertEquals(listOf(Route.SignIn), navigator.backStack.toList())
        }

    @Test
    fun onSubmitWithAMissingFieldDoesNotCallTheRepositoryAndTouchesEveryField() =
        runTest(testDispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = SignUpViewModel(repository, Navigator(Route.SignUp))

            viewModel.onIntent(SignUpIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertNull(repository.lastSignUp)
            assertEquals("Required", viewModel.uiState.value.email.errorMessage)
        }

    /** Same rationale as SignInViewModelTest's equivalent test - see that file for the full why. */
    @Test
    fun signInClickedReplacesTheBackStackSoReturningToSignUpLaterStartsFresh() =
        runTest(testDispatcher) {
            val navigator = Navigator(Route.SignUp)
            val viewModel = SignUpViewModel(FakeAuthRepository(), navigator)

            viewModel.onIntent(SignUpIntent.SignInClicked)

            assertEquals(listOf(Route.SignIn), navigator.backStack.toList())
        }

    @Test
    fun emailAlreadyRegisteredSurfacesAsADuplicateSubmissionOutcomeAndDoesNotNavigate() =
        runTest(testDispatcher) {
            val repository = FakeAuthRepository(signUpResult = SignUpResult.EmailAlreadyRegistered)
            val navigator = Navigator(Route.SignUp)
            val viewModel = SignUpViewModel(repository, navigator)

            viewModel.onIntent(SignUpIntent.EmailChanged("jane@example.com"))
            viewModel.onIntent(SignUpIntent.FirstnameChanged("Jane"))
            viewModel.onIntent(SignUpIntent.LastnameChanged("Mdoe"))
            viewModel.onIntent(SignUpIntent.PasswordChanged("password123"))
            viewModel.onIntent(SignUpIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertIs<SubmissionOutcome.Duplicate>(viewModel.uiState.value.submissionOutcome)
            assertEquals(listOf(Route.SignUp), navigator.backStack.toList())
        }
}
