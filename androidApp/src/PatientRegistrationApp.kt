package ke.don.intellisoft_patient_registration

import android.app.Application
import dev.brewkits.kmpworkmanager.KmpWorkManager
import dev.brewkits.kmpworkmanager.KmpWorkManagerConfig
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.PatientListingRepository
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.data.VitalsRepository
import ke.don.intellisoft_patient_registration.di.apiModule
import ke.don.intellisoft_patient_registration.di.authModule
import ke.don.intellisoft_patient_registration.di.clockModule
import ke.don.intellisoft_patient_registration.di.cryptoModule
import ke.don.intellisoft_patient_registration.di.databaseModule
import ke.don.intellisoft_patient_registration.di.httpClientModule
import ke.don.intellisoft_patient_registration.di.mapperModule
import ke.don.intellisoft_patient_registration.di.repositoryModule
import ke.don.intellisoft_patient_registration.di.workManagerModule
import ke.don.intellisoft_patient_registration.work.AppWorkerFactory
import ke.don.intellisoft_patient_registration.work.scheduleBackgroundSyncTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Starts Koin for real - startKoin{} never existed anywhere in this project before this, every
 * module was only ever proven with Module.verify() (see AndroidKoinModuleTest), not actually
 * started. Then initializes KmpWorkManager on top of it and schedules the five periodic
 * background tasks (see BackgroundSyncScheduling.kt in `shared`).
 *
 * Deliberately thin and untested, same status as MainActivity: this module has no test
 * infrastructure, and this project's established substitute for "start Koin in a test" is
 * proving the DI graph resolves via verify() instead - actually exercising KmpWorkManager's
 * real WorkManager integration under Robolectric would need a new androidx.work:work-testing
 * dependency nothing else in this project needs. The scheduling *decision* itself
 * (scheduleBackgroundSyncTasks) is unit-tested against FakeBackgroundTaskScheduler.
 */
class PatientRegistrationApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val koin =
            startKoin {
                modules(
                    httpClientModule,
                    apiModule,
                    authModule,
                    mapperModule,
                    cryptoModule,
                    databaseModule,
                    repositoryModule,
                    workManagerModule,
                    clockModule,
                )
            }.koin

        koin.loadModules(
            listOf(
                module {
                    single { koin.get<PatientRepository> { parametersOf(this@PatientRegistrationApp) } }
                    single { koin.get<VitalsRepository> { parametersOf(this@PatientRegistrationApp) } }
                    single { koin.get<GeneralAssessmentRepository> { parametersOf(this@PatientRegistrationApp) } }
                    single { koin.get<OverweightAssessmentRepository> { parametersOf(this@PatientRegistrationApp) } }
                    single { koin.get<PatientListingRepository> { parametersOf(this@PatientRegistrationApp) } }
                },
            ),
        )

        val workerFactory = koin.get<AppWorkerFactory> { parametersOf(this) }
        KmpWorkManager.initialize(context = this, workerFactory = workerFactory, config = KmpWorkManagerConfig())

        CoroutineScope(Dispatchers.Default).launch {
            KmpWorkManager.getInstance().backgroundTaskScheduler.scheduleBackgroundSyncTasks()
        }
    }
}
