package ke.don.intellisoft_patient_registration.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Owns the back stack that Navigation 3's `NavDisplay` renders.
 *
 * The list is a [SnapshotStateList] so Compose recomposes `NavDisplay` on every change. Nothing
 * outside this class should mutate [backStack] directly; screens go through these methods so the
 * stack rules (push, pop, replace-all) live in one place.
 */
class Navigator(
    startDestination: Route,
) {
    val backStack: SnapshotStateList<Route> = mutableStateListOf(startDestination)

    fun goTo(route: Route) {
        backStack.add(route)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }

    /**
     * Used after a successful assessment submit so the back button can never return to an
     * already-submitted form.
     */
    fun replaceAllWith(route: Route) {
        backStack.clear()
        backStack.add(route)
    }
}
