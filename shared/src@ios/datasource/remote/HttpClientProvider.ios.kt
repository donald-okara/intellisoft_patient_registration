package ke.don.intellisoft_patient_registration.datasource.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE

actual fun platformHttpClientEngine(): HttpClientEngine = Darwin.create()

/** No Android-style Logcat equivalent needed - println already reaches Xcode's console on iOS. */
actual fun platformPushLogger(): Logger = Logger.SIMPLE
