package ke.don.intellisoft_patient_registration.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ke.don.intellisoft_patient_registration.feature.patientregistration.PatientRegistrationScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The app's single NavDisplay, driven by the Koin-provided [Navigator]'s back stack.
 */
@Composable
fun AppNavHost(navigator: Navigator = koinInject()) {
    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.goBack() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.PatientRegistration> {
                    PatientRegistrationScreen(
                        viewModel = koinViewModel { parametersOf(navigator) },
                    )
                }
            },
    )
}
