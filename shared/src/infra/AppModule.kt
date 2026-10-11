package ke.don.intellisoft_patient_registration.infra

import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.module

expect fun platformHttpClientEngine(): HttpClientEngine

val httpClientModule =
    module {
        single<HttpClientEngine> { platformHttpClientEngine() }
        single { provideHttpClient(get()) }
    }
