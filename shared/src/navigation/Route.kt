package ke.don.intellisoft_patient_registration.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every screen in the app, as a serializable back-stack entry.
 *
 * Route fields are primitives (`String`/`Long`), not domain value classes: the domain layer
 * stays framework-free (see CLAUDE.md), and each screen's ViewModel wraps the primitive back
 * into its domain type at its own boundary.
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Splash : Route

    @Serializable
    data object SignIn : Route

    @Serializable
    data object SignUp : Route

    @Serializable
    data object PatientRegistration : Route

    @Serializable
    data class Vitals(
        val patientId: String,
    ) : Route

    @Serializable
    data class GeneralAssessment(
        val patientId: String,
        val vitalsId: Long,
    ) : Route

    @Serializable
    data class OverweightAssessment(
        val patientId: String,
        val vitalsId: Long,
    ) : Route

    @Serializable
    data class PatientListing(
        val visitDate: String? = null,
    ) : Route
}
