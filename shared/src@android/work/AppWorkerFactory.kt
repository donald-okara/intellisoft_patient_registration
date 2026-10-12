package ke.don.intellisoft_patient_registration.work

import dev.brewkits.kmpworkmanager.background.domain.AndroidWorker
import dev.brewkits.kmpworkmanager.background.domain.AndroidWorkerFactory
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.data.VitalsRepository

/**
 * Proves the scheduler -> factory -> worker wiring end to end, independent of any repository.
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
 * Retries every VitalsEntity row left `synced = false` by a Retriable push failure - same shape
 * as RetryUnsyncedPatientsWorker (see DefaultVitalsRepository.retryPush).
 */
class RetryUnsyncedVitalsWorker(
    private val vitalsRepository: VitalsRepository,
) : AndroidWorker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        vitalsRepository.retryUnsyncedPushes()
        return WorkerResult.Success()
    }

    override fun close() = Unit
}

/**
 * Retries every GeneralAssessmentEntity row left `synced = false` by a Retriable push failure -
 * same shape as RetryUnsyncedPatientsWorker (see DefaultGeneralAssessmentRepository.retryPush).
 */
class RetryUnsyncedGeneralAssessmentsWorker(
    private val generalAssessmentRepository: GeneralAssessmentRepository,
) : AndroidWorker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        generalAssessmentRepository.retryUnsyncedPushes()
        return WorkerResult.Success()
    }

    override fun close() = Unit
}

/**
 * Retries every OverweightAssessmentEntity row left `synced = false` by a Retriable push failure
 * - same shape as RetryUnsyncedPatientsWorker (see DefaultOverweightAssessmentRepository.retryPush).
 */
class RetryUnsyncedOverweightAssessmentsWorker(
    private val overweightAssessmentRepository: OverweightAssessmentRepository,
) : AndroidWorker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult {
        overweightAssessmentRepository.retryUnsyncedPushes()
        return WorkerResult.Success()
    }

    override fun close() = Unit
}

/**
 * Pulls the server's patient list into local storage - the list-sync direction, not the
 * outbox/push direction the RetryUnsyncedXWorkers cover. Deliberately no backoff on failure
 * (unlike offlineFirstWrite's push retries): a failed sync just waits for its own next periodic
 * run, which doesn't carry the same urgency as a user-visible submission still being unsynced.
 */
class SyncPatientsWorker(
    private val patientRepository: PatientRepository,
) : AndroidWorker {
    override suspend fun doWork(
        input: String?,
        env: WorkerEnvironment,
    ): WorkerResult =
        if (patientRepository.sync()) {
            WorkerResult.Success()
        } else {
            WorkerResult.Failure(message = "Patient list sync failed", shouldRetry = false)
        }

    override fun close() = Unit
}

/**
 * Manual registry, not KSP-generated (`kmpworker-ksp` was skipped for this phase - see
 * DESIGN.md). Register each real worker here by the name passed to `enqueue(...)`.
 * [patientRepository]/[vitalsRepository]/[generalAssessmentRepository]/[overweightAssessmentRepository]
 * are optional so existing no-dependency callers (and the smoke test) don't need one - real app
 * wiring will always pass them once they exist.
 */
class AppWorkerFactory(
    patientRepository: PatientRepository? = null,
    vitalsRepository: VitalsRepository? = null,
    generalAssessmentRepository: GeneralAssessmentRepository? = null,
    overweightAssessmentRepository: OverweightAssessmentRepository? = null,
    private val workers: Map<String, () -> AndroidWorker> =
        buildMap {
            put("NoOpWorker") { NoOpWorker() }
            if (patientRepository != null) {
                put("RetryUnsyncedPatientsWorker") { RetryUnsyncedPatientsWorker(patientRepository) }
                put("SyncPatientsWorker") { SyncPatientsWorker(patientRepository) }
            }
            if (vitalsRepository != null) {
                put("RetryUnsyncedVitalsWorker") { RetryUnsyncedVitalsWorker(vitalsRepository) }
            }
            if (generalAssessmentRepository != null) {
                put("RetryUnsyncedGeneralAssessmentsWorker") {
                    RetryUnsyncedGeneralAssessmentsWorker(generalAssessmentRepository)
                }
            }
            if (overweightAssessmentRepository != null) {
                put("RetryUnsyncedOverweightAssessmentsWorker") {
                    RetryUnsyncedOverweightAssessmentsWorker(overweightAssessmentRepository)
                }
            }
        },
) : AndroidWorkerFactory {
    override fun createWorker(workerClassName: String): AndroidWorker =
        workers[workerClassName]?.invoke() ?: error("Unknown worker: $workerClassName")
}
