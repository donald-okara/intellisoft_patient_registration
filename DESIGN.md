# Design note

One page max. Filled in incrementally as each phase lands, not reconstructed at the end.

## Key design decisions

<!-- 2-3 decisions on architecture, libraries, and data model, and why. Add one short entry per
     phase as it's made, e.g.:
     - **Local-first writes.** Every form save commits to Room before the API call fires, so the
       UI never blocks on network and the repository is testable with a faked API. -->

- **ktlint runs standalone, not through Amper.** Amper (the Kotlin Toolchain build tool this
  project uses instead of Gradle) has no built-in lint check. `scripts/ktlint.sh` pins and
  checksum-verifies the ktlint CLI itself, so local dev and CI run the exact same tool.
- **Room, Ktor + kotlinx.serialization, and kmpworkmanager are scoped to Android only** for now.
  Amper's KSP support (used for Room's compiler) is explicitly documented as per-platform, and
  this project's CI only builds/tests Android, consistent with the brief accepting an
  Android-only submission. Ktor's client plugins/content negotiation are common code either way;
  only the platform engine (OkHttp) and Room/kmpworkmanager themselves are Android-scoped.
- **Room's Android builder needs Robolectric for tests**, not plain JVM unit tests - Room's
  Android APIs require a real `Context`, which only a Robolectric-simulated Android environment
  provides without a device/emulator. That pulls in a JUnit4 runner (`@RunWith(RobolectricTestRunner)`)
  for just those tests, run via the JUnit5 Vintage engine alongside the project's usual
  `kotlin.test`-based tests elsewhere.
- **No KSP-generated worker factories yet.** `kmpworkmanager`'s optional `kmpworker-ksp`
  processor would auto-generate `AndroidWorkerFactory`/`IosWorkerFactory`; a hand-written
  `AppWorkerFactory` (`shared/src@android/work/AppWorkerFactory.kt`) does the same job with one
  fewer KSP processor to debug while three libraries were being wired at once. Revisit once
  there are enough real workers for the manual registry to get unwieldy.
- **Koin for DI**, modules defined but not yet started (no `startKoin {}` anywhere) - same
  "prove the wiring, don't fake production usage" approach as the rest of this phase, verified
  with Koin's `Module.verify()` instead of starting a real app-wide instance.
- **Package layout is by architectural layer**, not a flat `infra` catch-all:
  `datasource.remote`/`datasource.local` (Ktor/Room), `di` (Koin modules only — no logic), `work`
  (kmpworkmanager), and `data`/`feature.<name>` reserved for repositories and per-screen code
  once they exist.

## What was simplified or left out

<!-- What was deliberately not built, and why, so it doesn't read as an oversight. -->

- The Room entity/DAO/database added while wiring up Room is infra-proof scaffolding, not the
  real data model - it lives in `shared/test@android/datasource/local` and is clearly marked as
  throwaway, superseded once real patient entities exist.
- `kmpworkmanager-http` (its built-in HTTP upload/download workers) was skipped - it pins Ktor
  3.1.x, which would conflict with this project's own Ktor 3.6.0 client. Real sync workers will
  call the project's own `HttpClient` instead.
- No `KmpWorkManager.initialize(...)` call exists yet in a real `Application` class - there's no
  real worker to run in production yet, so that wiring is deferred to the phase that adds one.
  Same for Koin: no `startKoin {}` call, no Android `Application` class/manifest change.

## Shared KMP module vs. platform-specific

<!-- Per the brief: state which parts of the code could move to the shared module and which
     could not, even if the final submission is Android-only. -->

- Ktor client configuration (`shared/src/datasource/remote/HttpClientProvider.kt`) and
  kotlinx.serialization models are fully common - only the HTTP engine (`ktor-client-okhttp` for
  Android, `ktor-client-darwin` is wired for iOS too) is platform-specific.
- Room and kmpworkmanager usage is currently Android-only (`shared/src@android`), since this
  phase scoped both to Android per the brief's accepted Android-only submission path. Both
  libraries do support iOS; extending them there would mean adding `settings@ios` KSP scoping
  for Room and iOS workers/factories for kmpworkmanager - not attempted here.
