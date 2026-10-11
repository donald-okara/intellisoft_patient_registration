package ke.don.intellisoft_patient_registration.di

import io.ktor.client.engine.HttpClientEngine
import ke.don.intellisoft_patient_registration.datasource.remote.platformHttpClientEngine
import ke.don.intellisoft_patient_registration.datasource.remote.provideHttpClient
import org.koin.dsl.module

val httpClientModule =
    module {
        single<HttpClientEngine> { platformHttpClientEngine() }
        single { provideHttpClient(get()) }
    }
