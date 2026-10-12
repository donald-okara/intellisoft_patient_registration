package ke.don.intellisoft_patient_registration.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
    @Test
    fun goToPushesOntoTheBackStack() {
        val navigator = Navigator(Route.PatientRegistration)
        navigator.goTo(Route.PatientListing())
        assertEquals(listOf(Route.PatientRegistration, Route.PatientListing()), navigator.backStack)
    }

    @Test
    fun goBackRemovesTheLastEntry() {
        val navigator = Navigator(Route.PatientRegistration)
        navigator.goTo(Route.PatientListing())
        navigator.goBack()
        assertEquals(listOf<Route>(Route.PatientRegistration), navigator.backStack)
    }

    @Test
    fun replaceAllWithClearsTheEntireStackBeforePushing() {
        val navigator = Navigator(Route.PatientRegistration)
        navigator.goTo(Route.Vitals(patientId = "P1"))
        navigator.replaceAllWith(Route.PatientListing())
        assertEquals(listOf<Route>(Route.PatientListing()), navigator.backStack)
    }
}
