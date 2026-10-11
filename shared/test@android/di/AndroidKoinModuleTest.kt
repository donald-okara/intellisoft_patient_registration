package ke.don.intellisoft_patient_registration.di

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * Not a real feature test. Proves cryptoModule/databaseModule actually resolve, without starting
 * a real app-wide Koin instance - same pattern as KoinModuleSmokeTest.
 */
class AndroidKoinModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun cryptoModuleResolves() {
        cryptoModule.verify()
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun databaseModuleResolves() {
        databaseModule.verify()
    }
}
