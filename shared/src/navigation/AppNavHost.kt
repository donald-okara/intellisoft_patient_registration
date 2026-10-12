package ke.don.intellisoft_patient_registration.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.koinInject

/**
 * The app's single NavDisplay, driven by the Koin-provided [Navigator]'s back stack.
 *
 * `entryProvider` is empty for now - no screen exists yet to map a [Route] to (the first one,
 * PatientRegistrationScreen, lands in Task 2.6). Each later phase adds one `entry<Route.X>` line
 * here as its screen is built.
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
        entryProvider = entryProvider { },
    )
}
