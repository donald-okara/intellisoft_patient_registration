package ke.don.intellisoft_patient_registration.infra

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * Not a real feature test. Proves the Koin graph for httpClientModule actually resolves (engine
 * -> HttpClient), without starting a real app-wide Koin instance.
 */
class KoinModuleSmokeTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun httpClientModuleResolves() {
        httpClientModule.verify()
    }
}
