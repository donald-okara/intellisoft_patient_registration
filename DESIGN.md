# Design note

One page max. Filled in incrementally as each phase lands, not reconstructed at the end.

## Key design decisions

- **Package layout is by architectural layer**, not a flat `infra` catch-all: `domain` (business
  types, no framework dependency), `datasource.remote`/`datasource.local` (Ktor/Room/token &
  PII storage), `di` (Koin modules only — no logic), `work` (kmpworkmanager), `data.mapper`
  (entity/DTO ↔ domain conversions). All four `data` repositories (Patient/Vitals/General/
  Overweight) now exist; `feature.<name>`'s per-screen code (ViewModels, the five screens) is
  the only piece not built yet.
- **IDs and PII are `@JvmInline value class` wrappers, domain-only** — `PatientId`/`VitalsId`/etc.
  for type safety (can't pass a `VitalsId` where a `GeneralAssessmentId` is expected); `FirstName`/
  `LastName`/`DateOfBirth`/`Comments` additionally implement a shared `Redacted` interface so
  `toString()` never prints the real value. Kotlin interfaces can't override `toString()` itself
  (a hard restriction on `Any`'s methods) and value classes can't use class inheritance, so each
  PII type has one line — `override fun toString() = redact()` — delegating to the interface's
  one real implementation, rather than repeating the redaction text/logic per type. Considered
  and rejected: a generic `Redacted<Tag, T>` value class that removes that one line entirely —
  its generic-value-class boxing behavior isn't reliably zero-cost the way a plain value class
  is, and plain type aliases of one generic type would make e.g. `FirstName`/`LastName`
  interchangeable, losing exactly the safety this was for. Entities/DTOs stay on plain types,
  wrapped/unwrapped at the mapper boundary — Room/kotlinx.serialization's handling of value-class
  columns wasn't verified, and the actual goal (safety, redaction) only matters in domain logic.
- **PII is encrypted at rest in Room on both platforms, decrypted on read.** `firstName`/
  `lastName`/`dateOfBirth` (patients) and `comments` (both assessment forms) are ciphertext in
  the database. Not encrypted: `patientId` (primary key — needed in plaintext for the uniqueness
  constraint) and any `visitDate` (part of a `(patientId, visitDate)` unique index). Android
  reuses the `AndroidKeyStore` AES/GCM approach already built for the auth token
  (`TokenEncryptor` generalized to `AesGcmEncryptor`, keyed by alias — a separate key for the
  token vs. PII). iOS gets *real* AES-GCM too (not a Keychain-direct shortcut, which doesn't fit
  per-column ciphertext across many rows) via `dev.whyoleg.cryptography` — a maintained
  multiplatform crypto library wrapping native per-platform crypto (JDK/CryptoKit), chosen over
  hand-written Kotlin/Native CommonCrypto cinterop specifically because that would be far
  riskier to get right unverified on a machine with no Xcode. The generated AES key itself is
  stored as a single Keychain entry. Both sit behind one common `FieldEncryptor` interface.
- **DTOs mirror the API exactly, including the surprising parts**: `vital/add`'s `height`/
  `weight`/`bmi`/`patient_id` are sent as JSON *strings* in every example, not numbers — kept
  that way rather than "corrected" to `Int`/`Double`, since real numbers could silently fail
  against the real API. `AddVisitRequestDto.onDiet`/`onDrugs` are nullable so the client sends
  only the field for the active form (General vs. Overweight), per `docs/endpoints.md`'s own
  recommendation on that ambiguity.
- **`GeneralAssessment`/`OverweightAssessment` stay separate domain types**, not one `Assessment`
  with nullable "only applies to one form" fields — matches the two distinct mockup screens, and
  avoids a `GeneralAssessment` that could hold a `currentlyUsingDrugs` value.
- **kmpworkmanager is Android-only; Room/DataStore/crypto target both platforms** (iOS
  dependency-resolution-verified only, never compiled — no Xcode here). No KSP-generated worker
  factories (`AppWorkerFactory` is hand-written) or `startKoin {}` call anywhere yet — Koin
  modules (including the new `databaseModule`/`cryptoModule`) are verified with `Module.verify()`
  rather than started for real, same "prove the wiring, don't fake production usage" pattern as
  the rest of this project's infra.
- **Considered and deliberately not built**: crash/error reporting (e.g. Sentry). Worth flagging
  given this handles sensitive health data, but not worth scaffolding code nobody runs — a
  living decision, not a line item to ship.
- **`data.sync`: a reusable pull-sync contract, built ahead of the repositories that will use
  it** — `syncList`/`syncOne`, modeled on the `Synchronizer` pattern audited in
  `now-in-android`/`droidconKeKotlin` (their shape, not their code — both are Android/Gradle,
  this project isn't). `droidconKeKotlin`'s fetch-and-reconcile-by-key shape fit better than
  `now-in-android`'s, which needs a change-list/version endpoint this API doesn't have. Neither
  repo's `Synchronizer` needed to be an interface/shared-instance for us — that exists in NIA
  specifically to thread one instance through several repositories in a single `WorkManager` run,
  a pattern not wired here — so this is plain functions. This is the *pull* half of sync (remote
  → local cache); the *push* half (local write → remote, with retry) is a separately-flagged,
  still-deferred outbox concern this doesn't depend on.

- **Repositories wrap a shared `offlineFirstWrite` local-first pattern** (`data/sync/
  OfflineWriteResult.kt`): insert to Room first (whose own uniqueness constraint *is* the
  duplicate/already-submitted-today check — no separate validation query needed), then one
  best-effort remote push; a push failure never changes the local result the caller sees. Each
  repository maps the generic `OfflineWriteResult` to a domain-named result
  (`RegisterPatientResult.DuplicatePatientId`, not a generic `Conflict`) so the reusable logic
  doesn't cost readability. Built once with `PatientRepository`, reused as-is by Vitals/General/
  Overweight. The one thing that doesn't carry over: only `PatientRepository` implements
  `Syncable` — `patients/view` is the only endpoint shaped for pulling records back; `vital/add`
  and `visits/add` have no corresponding list/view endpoint to pull from, so the other three
  repositories are write-only (push + local-only reads).
- **Pagination uses plain Room `LIMIT`/`OFFSET`, not AndroidX Paging.** `getPatients(page,
  pageSize)` and `getPatient(id)` return `Flow<List<Patient>>`/`Flow<Patient?>` - reactive, so a
  later write is reflected without a manual re-fetch - but real Paging 3 on this Room version
  (2.8.5) + iOS wasn't available to reach for: it needs Google's new `room3`/`room3-paging`
  artifacts (a separate, breaking rewrite, still alpha/RC), or else Android-only
  `androidx.room:room-paging`, which would fork `getPatients`' signature per platform. Not worth
  either cost for a `List<Patient>` return this size. All display-facing DB reads
  (`PatientDao.findByPatientId`/`findPage`, `VitalsDao`/`GeneralAssessmentDao`/
  `OverweightAssessmentDao`'s `findByPatientId`) return `Flow` for the same reason; `findAll()`
  (sync's key reconciliation) and `findUnsynced()` (the retry worker's pass) stay one-shot
  `suspend` - both want a single snapshot, not an ongoing subscription.

## What was simplified or left out

- `kmpworkmanager-http` was skipped — pins Ktor 3.1.x, conflicting with this project's own Ktor
  3.6.0 client. Real sync workers will call the project's own `HttpClient` instead.
- No `KmpWorkManager.initialize(...)` or `startKoin {}` call exists yet — nothing real to run in
  production until a repository/`Application` wiring phase adds one.
- ViewModels and the five screens don't exist yet. All four repositories do now, including the
  app-layer translation from a DB constraint violation to a domain-named result
  (`DuplicatePatientId`/`DuplicateVisitDate`) - what's left is wiring that into UI-facing
  validation messages.
- **Outbox: push failures are classified, not retried blindly** (`data/sync/PushOutcome.kt`,
  `classifyPush`). `HttpClientProvider` sets `expectSuccess = true` so a non-2xx response throws
  Ktor's `ClientRequestException`/`ServerResponseException` instead of silently deserializing a
  `success = false` body; `classifyPush` turns that into `Retriable` (timeout/IOException/5xx) or
  `NeedsAttention` (401 → `Unauthorized`, any other 4xx → `ValidationFailed`). No schema
  migration was needed: `Retriable` just leaves `synced = false` (unchanged from before), and
  `NeedsAttention` deletes the local row immediately, synchronously, as part of the same write
  that discovered it.
- **No paginated/chunked sync for large remote fetches.** `syncList`'s `remoteFetcher` is a
  single-shot `() -> List<T>`, and `DefaultPatientRepository.sync()`'s `localUpsert` maps/
  encrypts/writes that whole list in one pass — fine for this API (`patients/view` has no
  cursor/paging contract to design against) but not something that would hold up against a
  genuinely large remote dataset. If it needed to: stream pages through a cursor-based
  `remoteFetcher`, chunk the Room `upsert` transaction per page, and only parallelize the
  per-row PII encryption if profiling actually shows cipher cost (not DB/network I/O) as the
  bottleneck — speculative plumbing against an API that doesn't support paging wasn't worth
  building now. Related: `AesGcmEncryptor` previously re-resolved the AndroidKeyStore key on
  every single `encrypt`/`decrypt` call rather than caching it, which would have made even a
  single large fetch pay for N redundant keystore lookups — fixed independent of the paging
  question, since it's wrong at any scale, not just a large one.
- **No auto-requeue on re-authentication, and no edit-and-resubmit flow for a purged row** — two
  deliberate scope cuts, not oversights. A fresh sign-in does not silently resurrect a stale push
  behind the user's back; they resubmit, same as for a validation failure. That in turn means a
  purged `ValidationFailed` row has **no path back** until a real edit flow exists — worth
  stating plainly rather than treating "resubmit" as a complete answer. Each repository has its
  own `RetryUnsyncedXWorker` (kmpworkmanager) retrying only its own `Retriable` rows, on its own
  periodic interval - no retry-count cap or backoff math, the interval itself is the backoff.

## Shared KMP module vs. platform-specific

- `domain` (models, value classes, BMI), Room entities/DAOs, DTOs, the mapper logic that doesn't
  touch encryption, and `data.sync` (no platform API touched at all) are fully common.
  Platform-specific: HTTP engines (OkHttp/Darwin),
  Room's database builder + SQLite driver (`sqlite-framework`/`sqlite-bundled`), and
  `FieldEncryptor`'s implementation (`AesGcmEncryptor` vs. Apple's Keychain+cryptography-kotlin
  implementation) — different enough per platform that a shared implementation wouldn't make
  sense even though the interface does.
- kmpworkmanager (`AppWorkerFactory`) is Android-only; extending it to iOS would mean iOS
  workers/factories too — not attempted.
