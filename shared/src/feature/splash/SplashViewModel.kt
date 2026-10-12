package ke.don.intellisoft_patient_registration.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.coroutines.launch

/**
 * hydrate() must complete before currentToken() is read - the in-memory cache starts empty every
 * launch, so reading it first would always route to Sign In regardless of a saved token.
 */
class SplashViewModel(
    private val cachedAuthTokenProvider: CachedAuthTokenProvider,
    private val navigator: Navigator,
) : ViewModel() {
    init {
        viewModelScope.launch {
            cachedAuthTokenProvider.hydrate()
            navigator.replaceAllWith(
                if (cachedAuthTokenProvider.currentToken() != null) Route.PatientListing() else Route.SignIn,
            )
        }
    }
}
