# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

A take-home interview assignment for Intellisoft (see
[`docs/Mobile App Development Practical - v2.pdf`](docs/Mobile%20App%20Development%20Practical%20-%20v2.pdf)):
an Android app, built with Kotlin Multiplatform, to register patients, capture vitals (with
auto-calculated BMI), route to one of two BMI-driven assessment forms, and show a
date-filterable patient listing. Full product spec lives in `README.md`; full API reference
lives in `docs/endpoints.md`.

The grading is 40% submission quality (correctness, validation, error handling, tests, code
quality, UI) and 60% a live walkthrough where the author explains and defends every part of the
submission — so prefer explainable, conventional choices over clever ones, and keep anything
non-obvious commented with *why*, not *what*.

## Current state

`App.kt`/`Greeting.kt`/`Platform.kt` are still the generated template's "Hello World"
scaffolding — none of the five screens exist yet. Infra (Room, Ktor client, kmpworkmanager,
Koin, encrypted token/PII storage) is wired and proven, and the real data model now exists too:
domain models (with value-class IDs/PII), Room entities/DAOs, DTOs matching `docs/endpoints.md`,
and the mappers between them (see **Data model** below) — but none of it is wired into a real
running app yet (no `startKoin {}`, no repositories, no ViewModels). Treat the five screens and
the repository layer as a greenfield build; everything under `domain`, `datasource`, `di`,
`work`, and `data.mapper` already exists — extend it, don't re-derive it.

## Package layout

Under root package `ke.don.intellisoft_patient_registration`, by layer — not a grab-bag `infra`
package:

- **`domain`** — business types with no framework dependency: `Patient`, `Vitals`,
  `GeneralAssessment`, `OverweightAssessment`, `Gender`/`HealthStatus` enums, the BMI
  calculator/classifier, value-class IDs, and the `Redacted`-implementing PII value classes.
- **`datasource.remote`** — API clients (the shared Ktor `HttpClient` setup) and `dto` (request/
  response types matching `docs/endpoints.md` exactly, including its field-type quirks).
- **`datasource.local`** — on-device persistence: the real Room entities/DAOs/`AppDatabase`, and
  encrypted storage — token (`AesGcmEncryptor`/`EncryptedTokenStorage` on Android,
  `KeychainTokenStorage` on iOS, behind `TokenStorage`) and PII columns (same `AesGcmEncryptor`
  with a different key alias on Android, `AppleFieldEncryptor` on iOS, behind `FieldEncryptor`).
- **`data`** — repositories (combine datasources, expose domain-facing APIs). Doesn't exist yet.
  **`data.mapper`** — entity ↔ domain and domain ↔ DTO conversions already exist here, ahead of
  the repositories that will use them.
- **`di`** — Koin modules only. A module should wire things together (`single { ... }` calling
  into `datasource`/`work`/`data`), not contain business logic itself.
- **`work`** — kmpworkmanager `Worker`/`WorkerFactory` implementations.
- **`feature.<name>`** — per-screen UI + its ViewModel, one subpackage per feature. Doesn't exist
  yet (the five screens are still unbuilt).

## Development workflow

These are working rules for this repo, not suggestions — follow them for every change:

1. **TDD, result-based.** Write a failing test before writing the implementation it covers.
   Tests assert on *results* (output, final state, returned value) — not on internal call
   sequences, mock-verify chains, or implementation steps. A test that only passes by asserting
   "method X was called" is testing the wrong thing.
2. **Branch + PR for everything.** No direct commits to the main branch. Every change — plan,
   feature, fix — lives on its own branch and lands via a pull request.
3. **Merge gate.** ktlint and the full test suite must pass before any PR merges. Amper has no
   built-in ktlint integration, so `scripts/ktlint.sh` runs the standalone ktlint CLI directly
   (pinned version, checksum-verified, self-bootstraps like `./kotlin` does); `.github/workflows/ci.yml`
   runs that same script plus `./kotlin test -p android` and `./kotlin build -m androidApp` on
   every PR, and `main` is branch-protected on that check passing.
4. **No co-authorship on commits/PRs.** Do not add `Co-Authored-By` trailers or any Claude
   attribution to commit messages or PR descriptions in this repo.
5. **Spec-driven, per phase.** For each phase of work: write a plan first, get the user's
   explicit approval before implementing, do the implementation (per rules 1–3), then write a
   walkthrough afterward summarizing what was built and how it was verified.

## Build system

This is a [JetBrains Amper](https://kotlin-toolchain.org/dev/) project, not plain Gradle.
Modules are declared in `module.yaml`/`project.yaml`, dependencies in `libs.versions.toml`.

```bash
./kotlin build                 # build everything
./kotlin run -m androidApp     # run the Android app
./kotlin test                  # run all tests
./kotlin test -m <module>      # run one module's tests
```

Use the `kotlin`/`kotlin.bat` wrapper scripts, not `gradlew` — there is no Gradle wrapper here.
`./kotlin test` runs every target including iOS, which needs a working local Xcode install;
on a machine without one (and in CI), scope it with `./kotlin test -p android`.

- `shared/src` — common code (Compose UI, business logic, data layer interfaces)
- `shared/src@android`, `shared/src@ios` — platform `actual` implementations
- Root package: `ke.don.intellisoft_patient_registration`

## Business rules to implement faithfully

- **BMI** = `weight(kg) / height(m)²`.
- **BMI status**: `< 18.5` Underweight · `18.5–<25` Normal · `≥ 25` Overweight. Test the
  boundary values (18.5 and 25 exactly) explicitly — the assignment calls this out.
- A patient registers **exactly once** (duplicate Patient ID must be rejected).
- Vitals/General/Overweight assessment forms each allow multiple submissions per patient, but
  **at most one per calendar date** per form type.
- Saving Vitals branches: BMI < 25 → General Assessment page; BMI ≥ 25 → Overweight
  Assessment page. Saving either assessment form → Patient Listing page.
- All fields on Vitals, General Assessment, and Overweight Assessment are mandatory.

## Data flow

Every form save: **write to Room/SQLite first**, then push to the backend. Don't block the
local save on network success — the assignment only asks that submission be *triggered*, and a
faked/mocked-API repository test is explicitly required, implying the repository layer should
be testable without a live network call.

## Infra already wired (Room, Ktor, kmpworkmanager, Koin, token storage)

kmpworkmanager is Android-only; Room, Ktor, DataStore, and token storage target both Android and
iOS (iOS is dependency-resolution-verified only — no Xcode on this dev machine, see `DESIGN.md`
for what that does and doesn't mean). Don't re-derive the setup below; extend it.

- **Room** (`androidx.room:room-runtime:2.8.5`, common dependency; KSP processor
  `androidx.room:room-compiler:2.8.5` registered under **both** `settings@android` and
  `settings@ios` in `shared/module.yaml` — Amper's KSP is per-platform, there's no single
  cross-platform shorthand). Android driver: `androidx.sqlite:sqlite-framework:2.7.1` +
  `AndroidSQLiteDriver` (not `sqlite-bundled`/`BundledSQLiteDriver` — that one needs a native lib
  Robolectric can't load). iOS driver: `androidx.sqlite:sqlite-bundled:2.7.1` (the reverse is
  true there — no `AndroidSQLiteDriver` equivalent on iOS). Entities/DAOs/`@Database` live in
  **common** source so KSP processes them for both platforms. Any Android Room DAO test needs
  `@RunWith(RobolectricTestRunner::class)` + `@Config(manifest = Config.NONE)` (JUnit4, via the
  vintage engine) because Room's Android builder requires a real `Context`. `AppDatabase` is the
  real schema now (`PatientEntity`/`VitalsEntity`/`GeneralAssessmentEntity`/
  `OverweightAssessmentEntity` — see **Data model** below), wired into Koin's `databaseModule` as
  a parameterized `single { (context: Context) -> buildAppDatabase(context) }` (Android) /
  parameterless (iOS, no `Context` needed there).
- **Token/PII storage** (`datasource.local`, common `TokenStorage`/`FieldEncryptor` interfaces):
  Android — `AesGcmEncryptor` (`AndroidKeyStore` AES/GCM; `internal constructor(secretKeyProvider:
  () -> SecretKey)` is the test seam — tests inject an in-memory key instead of going through
  the keystore, which Robolectric can't shadow; uses `java.util.Base64`, not `android.util.Base64`,
  unmocked outside Robolectric) is instantiated **twice** via Koin's `cryptoModule`
  (`tokenEncryptorQualifier`/`piiEncryptorQualifier`, different key aliases) — once backing
  `EncryptedTokenStorage` (writes to DataStore via `PreferenceDataStoreFactory.createWithPath
  { okio.Path }`, not the Android-only `create { java.io.File }` overload), once for PII columns
  consumed directly by `data.mapper`. iOS — `KeychainTokenStorage` stores the token directly in
  Keychain (no DataStore, no custom crypto needed for a single secret); PII instead uses
  `AppleFieldEncryptor` (`dev.whyoleg.cryptography` AES-GCM, key stored as one Keychain entry) —
  see `DESIGN.md` for why PII needs real crypto on iOS while the token doesn't.
- **Ktor + kotlinx.serialization**: `settings.ktor: enabled` and `settings.kotlin.serialization:
  json` in `shared/module.yaml` — both are Amper built-ins, already pinned to current stable
  (3.6.0 / 1.11.0), don't hand-pin versions. `provideHttpClient(engine, authTokenProvider)` in
  `shared/src/datasource/remote/HttpClientProvider.kt` (`datasource.remote` package) is the one
  client factory — build real API clients on top of it rather than constructing a new
  `HttpClient` elsewhere. Platform engines live alongside it as `actual
  platformHttpClientEngine()`: `ktor-client-okhttp` (Android), `ktor-client-darwin` (iOS, wired
  but unused so far).
- **kmpworkmanager** (`dev.brewkits:kmpworkmanager:3.5.0`, Android-only dependency). Needs
  `minSdk: 26` (already set in `androidApp/module.yaml`). Register real workers in
  `AppWorkerFactory` (`shared/src@android/work/AppWorkerFactory.kt`, `work` package) by the name
  string passed to `enqueue(...)`/`enqueueOneTime(...)` — there's no KSP codegen for this (see
  `DESIGN.md`). Test scheduling logic against `dev.brewkits:kmpworker-testing`'s
  `FakeBackgroundTaskScheduler`, not the real scheduler.
- **Koin** 4.2.2, plain Maven coordinates (no Amper-specific support, no `koin-annotations`
  KSP). All modules live under the `di` package: `httpClientModule`
  (`shared/src/di/AppModule.kt`, common) wires `datasource.remote`'s engine + `HttpClient`;
  `workManagerModule` (`shared/src@android/di/WorkManagerModule.kt`) wires `work`'s
  `AppWorkerFactory`. **No `startKoin {}` call exists anywhere yet** — modules are verified with
  `Module.verify()` (needs `@OptIn(KoinExperimentalAPI::class)`; the older `checkKoinModules()`
  is deprecated, don't reach for it) rather than started for real. Add new bindings to these
  modules as real repositories/view models arrive, rather than creating a new module per
  feature.
- None of these have Amper-specific docs beyond the generic `settings.kotlin.ksp.processors`
  mechanism (Room is literally Amper's own documented example for it) — Ktor/serialization are
  Amper "built-in technologies," Room/kmpworkmanager/Koin are plain Maven coordinates.

## Data model (domain, entities, DTOs, mappers)

The real data model for all five screens — not wired into a repository or UI yet, extend it
rather than re-deriving it:

- **`domain`**: `Patient`/`Vitals`/`GeneralAssessment`/`OverweightAssessment`, `Gender`/
  `HealthStatus` enums, `calculateBmi`/`classifyBmi`/`BmiStatus` (`Bmi.kt`). IDs are
  `@JvmInline value class` (`PatientId`, `VitalsId`, `GeneralAssessmentId`,
  `OverweightAssessmentId`). PII (`FirstName`, `LastName`, `DateOfBirth`, `Comments`) are also
  value classes, each with `override fun toString() = redact()` delegating to the shared
  `Redacted` interface (`Redacted.kt`) — don't try to move that `toString()` override onto the
  interface itself, Kotlin forbids interfaces from overriding `Any`'s methods. `Patient.age(today:
  LocalDate)` takes `today` as a required param, not a hidden clock default — keeps it a pure,
  deterministic function.
- **Entities** (`datasource.local`, common): `PatientEntity` (PK = business `patientId`, not an
  autoincrement id — that's what makes "registered exactly once" a DB constraint, not just app
  logic), `VitalsEntity`/`GeneralAssessmentEntity`/`OverweightAssessmentEntity` (autoincrement PK
  + a `UNIQUE(patientId, visitDate)` index — that's what enforces "one submission per calendar
  date" at the DB level). All four carry `serverId: Long?`/`synced: Boolean` for a future sync
  worker that doesn't exist yet. PII columns hold ciphertext — see token/PII storage above.
- **DTOs** (`datasource.remote.dto`, common): one file per endpoint group, matching
  `docs/endpoints.md` field-for-field including its quirks (string-typed vitals numbers,
  nullable `on_diet`/`on_drugs`). `ApiResponse<T>` is the shared envelope.
- **Mappers** (`data.mapper`, common): `PatientMapper`/`GeneralAssessmentMapper`/
  `OverweightAssessmentMapper` are classes taking a `FieldEncryptor` and are `suspend` (PII goes
  through it on every conversion); `VitalsMapper` has no PII so it's plain extension functions.
  Entity↔domain and domain→DTO/DTO→domain are separate conversions — DTOs always carry
  *plaintext* PII (the API needs real values), only the entity side is encrypted. An entity `id`
  of `0` means "not yet persisted" and maps to a `null` domain id, matching Room's
  autoincrement convention.

## Backend API

See [`docs/endpoints.md`](docs/endpoints.md) for the full reference (curl, payloads,
responses) — it was reverse-engineered from the Postman collection linked in the PDF brief,
since the PDF's own "Endpoints Overview" has several path/method mismatches with reality.
Key things to not re-derive from scratch:

- Base URL: `https://patientvisitapis.intellisoftkenya.com/api/`
- Auth: `POST user/signin` → `data.access_token`, sent as `Authorization: Bearer <token>` on
  every other endpoint.
- The vitals endpoint is `vital/add` (**singular**), not `vitals/add`.
- `patients/show/{id}` wraps its single record in a **one-element array**, unlike every other
  single-record response in this API.
- `visits/add` is shared by both the General and Overweight assessment forms — it accepts both
  `on_diet` and `on_drugs` in one schema. Decide (and be ready to defend) whether the client
  sends only the field for the active form or both.
- `visits/view` takes a `visit_date` filter and returns `{name, age, bmi, status}` rows — this
  is almost certainly the Patient Listing page's data source, despite being documented in the
  collection as "visits for a given patient."

## Testing requirements

The assignment explicitly requires, and graders will check for:

1. ✅ Unit tests for BMI calculation and BMI-status classification, including boundary values —
   `BmiTest.kt`.
2. Unit tests for form validation: mandatory fields are UI-phase work; duplicate Patient ID and
   one-submission-per-date are DB constraints, already covered at the DAO level
   (`PatientDaoTest`/`VitalsDaoTest`/etc.) — a repository test translating the constraint
   violation into a user-facing validation error is still owed once a repository exists.
3. ✅ Room DAO tests — one per entity (`PatientDaoTest`, `VitalsDaoTest`,
   `GeneralAssessmentDaoTest`, `OverweightAssessmentDaoTest`), more than the minimum asked for.
4. Still owed: one repository test using a faked or mocked API — no repository exists yet.

## Deliverables this repo still owes

- **Git history across ≥2 working sessions** — commit incrementally as features land, not as
  one bulk commit at the end; this is graded. Git is set up with a GitHub remote; follow the
  branch + PR workflow above for every change.
- **`DESIGN.md`** (root, one page max): 2–3 key design/architecture/library/data-model
  decisions and why, plus what was deliberately simplified or left out. Keep it factual and
  short — detailed trade-off discussion happens live, not in the doc.
- A screen recording (<5 min) of the full flow — not something Claude can produce; flag it as
  outstanding rather than attempting it.
- State in `DESIGN.md` which parts of the code could move to the shared KMP module vs. stay
  platform-specific, even if the final submission ends up Android-only.
