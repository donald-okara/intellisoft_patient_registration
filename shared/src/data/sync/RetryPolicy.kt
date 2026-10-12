package ke.don.intellisoft_patient_registration.data.sync

import kotlin.math.pow

/**
 * Exponential backoff between [offlineFirstWrite]'s own push attempts, for a transient
 * (Retriable) failure - not the same concern as the periodic retry worker's own, much coarser
 * backoff (its own schedule interval), which this doesn't replace.
 *
 * Defaults: 3 attempts total, waiting ~1s then ~2s between them (`1000 * 2.0^attempt`, capped at
 * 10s) - chosen so a brief network blip recovers within the same call instead of waiting for the
 * next periodic worker run, without blocking the caller for long on a dead connection.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1_000,
    val factor: Double = 2.0,
    val maxDelayMs: Long = 10_000,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1, was $maxAttempts" }
    }

    /** [attempt] is 0-indexed: the delay before retrying after the attempt'th failure. */
    fun delayForAttempt(attempt: Int): Long = (initialDelayMs * factor.pow(attempt)).toLong().coerceAtMost(maxDelayMs)
}
