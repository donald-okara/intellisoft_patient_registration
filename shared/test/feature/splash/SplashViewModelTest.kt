package ke.don.intellisoft_patient_registration.feature.splash

import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.data.FakeTokenStorage
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Uses UnconfinedTestDispatcher for Main, same reason PatientListingViewModelTest does - the
 * ViewModel's init{} kicks off a viewModelScope.launch, and the test body never calls
 * advanceUntilIdle() from inside that launch itself, so Unconfined is what actually lets it run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aSavedTokenNavigatesToPatientListing() =
        runTest(testDispatcher) {
            val tokenStorage = FakeTokenStorage().apply { saveToken("token-123") }
            val navigator = Navigator(Route.Splash)

            SplashViewModel(CachedAuthTokenProvider(tokenStorage), navigator)
            advanceUntilIdle()

            assertEquals(listOf(Route.PatientListing()), navigator.backStack.toList())
        }

    @Test
    fun noSavedTokenNavigatesToSignIn() =
        runTest(testDispatcher) {
            val navigator = Navigator(Route.Splash)

            SplashViewModel(CachedAuthTokenProvider(FakeTokenStorage()), navigator)
            advanceUntilIdle()

            assertEquals(listOf(Route.SignIn), navigator.backStack.toList())
        }
}
