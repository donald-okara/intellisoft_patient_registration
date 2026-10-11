package ke.don.intellisoft_patient_registration.work

import dev.brewkits.kmpworkmanager.background.domain.AndroidWorker
import dev.brewkits.kmpworkmanager.background.domain.AndroidWorkerFactory
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import ke.don.intellisoft_patient_registration.data.PatientRepository

/**
 * No real sync worker exists yet - that's the next phase, once there's a repository to call.
 * This proves the scheduler -> factory -> worker wiring end to end.
 */
class NoOpWorker : AndroidWorker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult = WorkerResult.Success()

    override fun close() = Unit
}

/**
 * Retries every PatientEntity row left `synced = false` by a Retriable push failure - a repeat
 * NeedsAttention classification purges the row the same way the original write did (see
 * DefaultPatientRepository.retryPush). Always returns Success even when some rows are still
 * unsynced afterward - the worker's own periodic interval is the retry cadence, not
 * WorkManager's Result.retry()/backoff machinery.
 */
class RetryUnsyncedPatientsWorker(
    private val patientRepository: PatientRepository,
) : AndroidWorker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        patientRepository.retryUnsyncedPushes()
        return WorkerResult.Success()
    }

    override fun close() = Unit
}

/**
 * Manual registry, not KSP-generated (`kmpworker-ksp` was skipped for this phase - see
 * DESIGN.md). Register each real worker here by the name passed to `enqueue(...)`.
 * [patientRepository] is optional so existing no-dependency callers (and the smoke test) don't
 * need one - real app wiring will always pass it once it exists.
 */
class AppWorkerFactory(
    patientRepository: PatientRepository? = null,
    private val workers: Map<String, () -> AndroidWorker> =
        buildMap {
            put("NoOpWorker") { NoOpWorker() }
            if (patientRepository != null) {
                put("RetryUnsyncedPatientsWorker") { RetryUnsyncedPatientsWorker(patientRepository) }
            }
        },
) : AndroidWorkerFactory {
    override fun createWorker(workerClassName: String): AndroidWorker =
        workers[workerClassName]?.invoke() ?: error("Unknown worker: $workerClassName")
}
