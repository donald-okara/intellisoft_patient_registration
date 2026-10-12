package ke.don.intellisoft_patient_registration.di

import ke.don.intellisoft_patient_registration.data.mapper.GeneralAssessmentMapper
import ke.don.intellisoft_patient_registration.data.mapper.OverweightAssessmentMapper
import ke.don.intellisoft_patient_registration.data.mapper.PatientMapper
import ke.don.intellisoft_patient_registration.datasource.local.FieldEncryptor
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import org.koin.test.verify.verifyAll
import kotlin.test.Test

/**
 * Not a real feature test. Proves cryptoModule/databaseModule/mapperModule/repositoryModule
 * actually resolve, without starting a real app-wide Koin instance - same pattern as
 * KoinModuleSmokeTest.
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

    /**
     * verify() reflects each mapper's constructor by raw type, blind to the piiEncryptorQualifier
     * actually passed in mapperModule - telling it to treat FieldEncryptor as externally supplied
     * avoids a false failure on a (real, qualified) binding it can't see.
     */
    private val mapperInjections =
        injectedParameters(
            definition<PatientMapper>(FieldEncryptor::class),
            definition<GeneralAssessmentMapper>(FieldEncryptor::class),
            definition<OverweightAssessmentMapper>(FieldEncryptor::class),
        )

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun mapperModuleResolvesAgainstCryptoModule() {
        listOf(cryptoModule, mapperModule).verifyAll(injections = mapperInjections)
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun repositoryModuleResolvesAgainstFullGraph() {
        listOf(httpClientModule, apiModule, cryptoModule, mapperModule, databaseModule, repositoryModule)
            .verifyAll(injections = mapperInjections)
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun workManagerModuleResolvesAgainstFullGraph() {
        listOf(
            httpClientModule,
            apiModule,
            cryptoModule,
            mapperModule,
            databaseModule,
            repositoryModule,
            workManagerModule,
        ).verifyAll(injections = mapperInjections)
    }
}
