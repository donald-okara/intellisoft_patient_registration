package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.FakeGeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.FakeOverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.FakeVitalsRepository
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.VitalsRepository
import ke.don.intellisoft_patient_registration.feature.generalassessment.GeneralAssessmentViewModel
import ke.don.intellisoft_patient_registration.feature.overweightassessment.OverweightAssessmentViewModel
import ke.don.intellisoft_patient_registration.feature.vitals.VitalsViewModel
import ke.don.intellisoft_patient_registration.navigation.Navigator
import ke.don.intellisoft_patient_registration.navigation.Route
import kotlinx.datetime.Clock
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * viewModelModule's Vitals/GeneralAssessment/OverweightAssessment definitions read Koin runtime
 * parameters by raw index - Module.verify() can't catch a wrong index because it only checks
 * declared constructor *types*, not what a lambda body actually does with ParametersHolder. These
 * tests drive the module with the exact `parametersOf(navigator, patientId, vitalsId)` shape
 * AppNavHost passes, and assert on the resolved ViewModel's own fields - a wrong index surfaces
 * as a ClassCastException or a swapped value, not a missing-definition failure.
 */
class ViewModelModuleTest {
    private val testModules =
        module {
            single<GeneralAssessmentRepository> { FakeGeneralAssessmentRepository() }
            single<OverweightAssessmentRepository> { FakeOverweightAssessmentRepository() }
            single<VitalsRepository> { FakeVitalsRepository() }
            single { Navigator(startDestination = Route.Splash) }
            single<Clock> { Clock.System }
        }

    @Test
    fun vitalsViewModelResolvesThePatientIdFromRuntimeParameters() {
        val koin = koinApplication { modules(testModules, viewModelModule) }.koin
        val navigator = koin.get<Navigator>()

        val viewModel =
            koin.get<VitalsViewModel> { parametersOf(navigator, "P-001") }

        assertEquals("P-001", viewModel.patientId)
    }

    @Test
    fun generalAssessmentViewModelResolvesPatientIdAndVitalsIdFromRuntimeParameters() {
        val koin = koinApplication { modules(testModules, viewModelModule) }.koin
        val navigator = koin.get<Navigator>()

        val viewModel =
            koin.get<GeneralAssessmentViewModel> { parametersOf(navigator, "P-002", 7L) }

        assertEquals("P-002", viewModel.patientId)
        assertEquals(7L, viewModel.vitalsId)
    }

    @Test
    fun overweightAssessmentViewModelResolvesPatientIdAndVitalsIdFromRuntimeParameters() {
        val koin = koinApplication { modules(testModules, viewModelModule) }.koin
        val navigator = koin.get<Navigator>()

        val viewModel =
            koin.get<OverweightAssessmentViewModel> { parametersOf(navigator, "P-003", 9L) }

        assertEquals("P-003", viewModel.patientId)
        assertEquals(9L, viewModel.vitalsId)
    }
}
