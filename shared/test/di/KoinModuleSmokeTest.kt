package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import org.koin.test.verify.verifyAll
import kotlin.test.Test

/**
 * Not a real feature test. Proves the Koin graph for httpClientModule/apiModule/authModule
 * actually resolves, without starting a real app-wide Koin instance.
 */
class KoinModuleSmokeTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun httpClientModuleResolves() {
        httpClientModule.verify()
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun apiModuleResolvesAgainstHttpClientModule() {
        listOf(httpClientModule, apiModule).verifyAll()
    }

    /** TokenStorage is a Koin runtime parameter here, not a graph dependency - see authModule. */
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun authModuleResolves() {
        authModule.verify(
            injections = injectedParameters(definition<CachedAuthTokenProvider>(TokenStorage::class)),
        )
    }
}
