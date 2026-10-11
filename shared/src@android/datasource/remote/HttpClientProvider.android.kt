package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpClientEngine(): HttpClientEngine = OkHttp.create()
