package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClientEngine(): HttpClientEngine = Darwin.create()
