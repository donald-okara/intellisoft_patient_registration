package ke.don.intellisoft_patient_registration.work

import dev.brewkits.kmpworkmanager.background.domain.ScheduleResult
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.enqueueOneTime
import dev.brewkits.kmpworkmanager.testing.FakeBackgroundTaskScheduler
import ke.don.intellisoft_patient_registration.data.FakePatientRepository
import ke.don.intellisoft_patient_registration.data.FakeVitalsRepository
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

    @Test
    fun factoryResolvesRetryUnsyncedPatientsWorkerWhenGivenARepository() =
        runTest {
            val factory = AppWorkerFactory(patientRepository = FakePatientRepository())

            val worker = factory.createWorker("RetryUnsyncedPatientsWorker")

            assertTrue(worker is RetryUnsyncedPatientsWorker)
        }

    @Test
    fun retryUnsyncedPatientsWorkerCallsTheRepository() =
        runTest {
            val repository = FakePatientRepository()
            val worker = RetryUnsyncedPatientsWorker(repository)

            worker.doWork(input = null, env = WorkerEnvironment())

            assertEquals(1, repository.retryUnsyncedPushesCallCount)
        }

    @Test
    fun factoryResolvesRetryUnsyncedVitalsWorkerWhenGivenARepository() =
        runTest {
            val factory = AppWorkerFactory(vitalsRepository = FakeVitalsRepository())

            val worker = factory.createWorker("RetryUnsyncedVitalsWorker")

            assertTrue(worker is RetryUnsyncedVitalsWorker)
        }

    @Test
    fun retryUnsyncedVitalsWorkerCallsTheRepository() =
        runTest {
            val repository = FakeVitalsRepository()
            val worker = RetryUnsyncedVitalsWorker(repository)

            worker.doWork(input = null, env = WorkerEnvironment())

            assertEquals(1, repository.retryUnsyncedPushesCallCount)
        }
}
