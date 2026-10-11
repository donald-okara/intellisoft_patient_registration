package ke.don.intellisoft_patient_registration.data.sync

import kotlinx.coroutines.CancellationException

/**
 * Catches everything except CancellationException (which must keep propagating for coroutine
 * cancellation to work) and returns it as a Result instead - the same small utility
 * independently written in now-in-android and droidconKeKotlin.
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
