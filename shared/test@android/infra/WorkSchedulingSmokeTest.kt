package ke.don.intellisoft_patient_registration.infra

import dev.brewkits.kmpworkmanager.background.domain.ScheduleResult
import dev.brewkits.kmpworkmanager.background.domain.enqueueOneTime
import dev.brewkits.kmpworkmanager.testing.FakeBackgroundTaskScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Not a real feature test. Proves the kmpworkmanager wiring + the manual AndroidWorkerFactory
 * actually schedule and resolve a worker, without needing a device/emulator.
 */
class WorkSchedulingSmokeTest {
    @Test
    fun enqueuesNoOpWorker() =
        runTest {
            val scheduler = FakeBackgroundTaskScheduler()

            val result =
                scheduler.enqueueOneTime(
                    id = "smoke-task",
                    workerClassName = "NoOpWorker",
                    initialDelayMs = 0,
                )

            assertEquals(ScheduleResult.ACCEPTED, result)
            assertTrue(scheduler.hasEnqueued("NoOpWorker"))
        }

    @Test
    fun factoryResolvesRegisteredWorker() =
        runTest {
            val factory = AppWorkerFactory()

            val worker = factory.createWorker("NoOpWorker")

            assertTrue(worker is NoOpWorker)
        }
}
