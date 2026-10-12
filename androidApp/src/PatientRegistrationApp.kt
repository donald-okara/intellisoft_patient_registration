package ke.don.intellisoft_patient_registration

import android.app.Application
import dev.brewkits.kmpworkmanager.KmpWorkManager
import dev.brewkits.kmpworkmanager.KmpWorkManagerConfig
import ke.don.intellisoft_patient_registration.data.CachedAuthTokenProvider
import ke.don.intellisoft_patient_registration.data.GeneralAssessmentRepository
import ke.don.intellisoft_patient_registration.data.OverweightAssessmentRepository
import ke.don.intellisoft_patient_registration.data.PatientListingRepository
import ke.don.intellisoft_patient_registration.data.PatientRepository
import ke.don.intellisoft_patient_registration.data.VitalsRepository
import ke.don.intellisoft_patient_registration.datasource.local.TokenStorage
import ke.don.intellisoft_patient_registration.datasource.remote.AuthTokenProvider
import ke.don.intellisoft_patient_registration.di.apiModule
import ke.don.intellisoft_patient_registration.di.authModule
import ke.don.intellisoft_patient_registration.di.clockModule
import ke.don.intellisoft_patient_registration.di.cryptoModule
import ke.don.intellisoft_patient_registration.di.databaseModule
import ke.don.intellisoft_patient_registration.di.httpClientModule
import ke.don.intellisoft_patient_registration.di.mapperModule
import ke.don.intellisoft_patient_registration.di.navigationModule
import ke.don.intellisoft_patient_registration.di.repositoryModule
import ke.don.intellisoft_patient_registration.di.tokenStorageModule
import ke.don.intellisoft_patient_registration.di.viewModelModule
import ke.don.intellisoft_patient_registration.di.workManagerModule
import ke.don.intellisoft_patient_registration.work.AppWorkerFactory
import ke.don.intellisoft_patient_registration.work.scheduleBackgroundSyncTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
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
                    tokenStorageModule,
                    repositoryModule,
                    workManagerModule,
                    clockModule,
                    navigationModule,
                    viewModelModule,
                )
            }.koin

        // Must complete, in its own loadModules call, before any repository below is resolved -
        // resolving a repository transitively forces HttpClient to resolve, which calls
        // provideHttpClient(get(), get()), and the second get() needs AuthTokenProvider to
        // already be bound context-free. Without this rebind first, that get() would fall back
        // to authModule's original parameterized single (which needs a TokenStorage runtime
        // parameter Koin has no way to supply here), and the app would silently send no
        // Authorization header at all.
        val tokenStorage = koin.get<TokenStorage> { parametersOf(this) }
        val cachedAuthTokenProvider = koin.get<CachedAuthTokenProvider> { parametersOf(tokenStorage) }

        koin.loadModules(
            listOf(
                module {
                    single { cachedAuthTokenProvider } bind AuthTokenProvider::class
                },
            ),
        )

        // Only now is it safe to resolve repositories - their API clients pull HttpClient, which
        // pulls AuthTokenProvider, which must already be rebound context-free (see above).
        // Resolve against the original repositoryModule bindings BEFORE loadModules below
        // overrides them - otherwise each single{}'s own koin.get<X>(...) would resolve lazily,
        // after the override, finding only itself and recursing infinitely (see task-2.3-report.md).
        val patientRepository = koin.get<PatientRepository> { parametersOf(this) }
        val vitalsRepository = koin.get<VitalsRepository> { parametersOf(this) }
        val generalAssessmentRepository = koin.get<GeneralAssessmentRepository> { parametersOf(this) }
        val overweightAssessmentRepository = koin.get<OverweightAssessmentRepository> { parametersOf(this) }
        val patientListingRepository = koin.get<PatientListingRepository> { parametersOf(this) }

        koin.loadModules(
            listOf(
                module {
                    single { patientRepository }
                    single { vitalsRepository }
                    single { generalAssessmentRepository }
                    single { overweightAssessmentRepository }
                    single { patientListingRepository }
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
