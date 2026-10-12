package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.Logger

actual fun platformHttpClientEngine(): HttpClientEngine = OkHttp.create()

actual fun platformPushLogger(): Logger = Logger.ANDROID
