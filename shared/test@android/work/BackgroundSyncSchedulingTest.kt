package ke.don.intellisoft_patient_registration.work

import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.testing.FakeBackgroundTaskScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * backgroundSyncTasks/scheduleBackgroundSyncTasks is the actual scheduling *decision* (which
 * workers, what interval, what constraints) - split out from startAppBackgroundWork so it's
 * testable against FakeBackgroundTaskScheduler without needing real WorkManager/Robolectric.
 */
class BackgroundSyncSchedulingTest {
    @Test
    fun schedulesAllFiveWorkersByName() =
        runTest {
            val scheduler = FakeBackgroundTaskScheduler()

            scheduler.scheduleBackgroundSyncTasks()

            assertTrue(scheduler.hasEnqueued("RetryUnsyncedPatientsWorker"))
            assertTrue(scheduler.hasEnqueued("RetryUnsyncedVitalsWorker"))
            assertTrue(scheduler.hasEnqueued("RetryUnsyncedGeneralAssessmentsWorker"))
            assertTrue(scheduler.hasEnqueued("RetryUnsyncedOverweightAssessmentsWorker"))
            assertTrue(scheduler.hasEnqueued("SyncPatientsWorker"))
            assertEquals(5, scheduler.enqueuedTasks.size)
        }

    @Test
    fun everyTaskIsPeriodicAtTheMinimumAllowedIntervalAndRequiresNetwork() =
        runTest {
            val scheduler = FakeBackgroundTaskScheduler()

            scheduler.scheduleBackgroundSyncTasks()

            scheduler.enqueuedTasks.forEach { task ->
                val trigger = assertIs<TaskTrigger.Periodic>(task.trigger)
                assertEquals(TaskTrigger.Periodic.MIN_INTERVAL_MS, trigger.intervalMs)
                assertTrue(task.constraints.requiresNetwork)
            }
        }

    @Test
    fun reschedulingKeepsTheExistingIntervalTimerInsteadOfResettingIt() =
        runTest {
            val scheduler = FakeBackgroundTaskScheduler()

            scheduler.scheduleBackgroundSyncTasks()

            scheduler.enqueuedTasks.forEach { task ->
                assertEquals(ExistingPolicy.KEEP, task.policy)
            }
        }

    @Test
    fun everyTaskHasAUniqueId() =
        runTest {
            val scheduler = FakeBackgroundTaskScheduler()

            scheduler.scheduleBackgroundSyncTasks()

            val ids = scheduler.enqueuedTasks.map { it.id }
            assertEquals(ids.toSet().size, ids.size)
        }
}
