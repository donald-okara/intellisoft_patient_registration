package ke.don.intellisoft_patient_registration.work

import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.enqueuePeriodic

/** One periodic background task: an id (for KEEP/REPLACE semantics) paired with the worker it runs. */
data class PeriodicTaskSpec(
    val id: String,
    val workerClassName: String,
    val intervalMs: Long = TaskTrigger.Periodic.MIN_INTERVAL_MS,
    val constraints: Constraints = Constraints(requiresNetwork = true),
)

/**
 * The five periodic tasks this app schedules at startup - split out as plain data so it's
 * testable against FakeBackgroundTaskScheduler without real WorkManager. All run at the
 * library's minimum allowed interval (15 min): the four outbox retries are a safety net behind
 * offlineFirstWrite's own immediate exponential backoff, and the patient-list sync has no
 * tighter deadline than "eventually."
 */
val backgroundSyncTasks =
    listOf(
        PeriodicTaskSpec(id = "retry-unsynced-patients", workerClassName = "RetryUnsyncedPatientsWorker"),
        PeriodicTaskSpec(id = "retry-unsynced-vitals", workerClassName = "RetryUnsyncedVitalsWorker"),
        PeriodicTaskSpec(
            id = "retry-unsynced-general-assessments",
            workerClassName = "RetryUnsyncedGeneralAssessmentsWorker",
        ),
        PeriodicTaskSpec(
            id = "retry-unsynced-overweight-assessments",
            workerClassName = "RetryUnsyncedOverweightAssessmentsWorker",
        ),
        PeriodicTaskSpec(id = "sync-patients", workerClassName = "SyncPatientsWorker"),
    )

/**
 * ExistingPolicy.KEEP - a relaunch of the app must not reset each task's interval timer, or
 * none of them would ever reach their first real execution on a device that's opened often.
 */
suspend fun BackgroundTaskScheduler.scheduleBackgroundSyncTasks(tasks: List<PeriodicTaskSpec> = backgroundSyncTasks) {
    tasks.forEach { task ->
        enqueuePeriodic(
            id = task.id,
            workerClassName = task.workerClassName,
            intervalMs = task.intervalMs,
            constraints = task.constraints,
            policy = ExistingPolicy.KEEP,
        )
    }
}
