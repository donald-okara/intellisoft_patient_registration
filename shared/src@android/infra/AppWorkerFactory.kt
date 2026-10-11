package ke.don.intellisoft_patient_registration.infra

import dev.brewkits.kmpworkmanager.background.domain.AndroidWorker
import dev.brewkits.kmpworkmanager.background.domain.AndroidWorkerFactory
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult

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
 * Manual registry, not KSP-generated (`kmpworker-ksp` was skipped for this phase - see
 * DESIGN.md). Register each real worker here by the name passed to `enqueue(...)`.
 */
class AppWorkerFactory(
    private val workers: Map<String, () -> AndroidWorker> =
        mapOf(
            "NoOpWorker" to { NoOpWorker() },
        ),
) : AndroidWorkerFactory {
    override fun createWorker(workerClassName: String): AndroidWorker =
        workers[workerClassName]?.invoke() ?: error("Unknown worker: $workerClassName")
}
