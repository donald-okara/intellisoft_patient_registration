# Screens & ViewModels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the five missing screens (Patient Registration, Vitals, General Assessment, Overweight Assessment, Patient Listing), their ViewModels, and the navigation graph connecting them, on top of the already-complete data layer (`PatientRepository`/`VitalsRepository`/`GeneralAssessmentRepository`/`OverweightAssessmentRepository`).

**Architecture:** Navigation 3 (Compose Multiplatform build, common code) with a user-owned `SnapshotStateList<Route>` back stack; one `androidx.lifecycle.ViewModel` per screen, resolved via Koin's `koinViewModel()`; a reusable `FieldState`/`SelectionState` pair (shared "touch-gated" validation contract) backing every form field; a reusable `SubmissionOutcome` sealed interface that every ViewModel maps its repository's result onto, so all four forms react to a push result the same way.

**Tech Stack:** Jetpack Compose Multiplatform, Navigation 3 (`org.jetbrains.androidx.navigation3`), AndroidX ViewModel (`org.jetbrains.androidx.lifecycle`, already a dependency), Koin 4.2.2 + `koin-compose-viewmodel`, kotlinx-datetime `Clock`.

**Spec:** `CLAUDE.md` (business rules, package layout, data flow, testing requirements) and `docs/endpoints.md` (`visits/view` for the Patient Listing data source).

## Global Constraints

- BMI = weight(kg) / height(m)². Status: `<18.5` Underweight, `18.5–<25` Normal, `≥25` Overweight (already implemented in `domain/Bmi.kt` — reuse, don't reimplement).
- A patient registers exactly once; duplicate Patient ID is rejected (`RegisterPatientResult.DuplicatePatientId`, already enforced at the DB layer).
- Vitals/General/Overweight forms: multiple submissions per patient allowed, but at most one per calendar date per form type (`*DuplicateVisitDate`, already enforced at the DB layer).
- Vitals save branches: BMI `<25` → General Assessment; BMI `≥25` → Overweight Assessment. Either assessment form save → Patient Listing.
- All fields on Vitals, General Assessment, and Overweight Assessment are mandatory. (Patient Registration's fields are likewise all mandatory per the mockups.)
- Every form save writes to Room first; the remote push is best-effort and never blocks or reverses the local result (already implemented in `Default*Repository` via `offlineFirstWrite`).
- ktlint (`scripts/ktlint.sh`) and `./kotlin test -p android` must pass before merge; everything lands via its own branch + PR; no direct commits to `main`.
- TDD, result-based: write the failing test first, assert on returned values/final state, not on mock call sequences.
- No `Co-Authored-By`/Claude attribution in commits or PR descriptions (project-specific override, see `CLAUDE.md` rule 4).

## Review Focus

- BMI exactly `25.0` after a real Vitals submission routes to the Overweight screen, not General — test the ViewModel's branch decision itself, not just `classifyBmi` in isolation (it's already unit-tested there, but the ViewModel is a second place this threshold can be gotten wrong, e.g. by an off-by-one in the `when`).
- Submitting a Vitals/General/Overweight form a second time on the same calendar date must surface `DuplicateVisitDate` as a blocking UI error and must **not** navigate forward — the one-submission-per-date rule is a DB constraint already, but the ViewModel is new code that could silently treat any non-exceptional return as "proceed."
- A field that has never been touched must show no error even when invalid; pressing Submit must force-touch every field and surface all of their errors at once (not just the first one hit) — otherwise a user who never focuses a required field (e.g. tabs straight to Submit) gets no feedback at all.
- Non-numeric, zero, or negative height/weight input must fail validation before `VitalsViewModel` ever constructs a `Vitals` domain object or calls the repository — a garbage BMI must never reach Room or the branch decision.
- Picking a Patient Listing date with zero matching visits renders an explicit empty state and does not keep showing the previous date's rows (a naive "keep last successful list on reload" implementation would do exactly that).

---

## Design decisions (confirm before implementing)

These are judgment calls this plan makes that go beyond what was explicitly specified. Flagging them here rather than deciding silently:

1. **`FieldState` is String-only** (per the spec: `validator(String) -> Decision`), backing text fields (names, Patient ID, height/weight, comments). Gender, HealthStatus, the two yes/no toggles, and the two date fields (DOB, visit date) aren't free text, so they get a parallel `SelectionState<T>` — same touch-gated `Decision` contract, but wrapping a nullable `T` with a single "must be selected" rule instead of a list of string validators. Both are defined in Phase 0 so every screen reuses the same two primitives.
2. **One generic `SubmissionOutcome`** sealed interface replaces four screens writing their own `when` over `RegisterPatientResult`/`SubmitVitalsResult`/`SubmitGeneralAssessmentResult`/`SubmitOverweightAssessmentResult`. Those four already share an identical shape (`Success(synced)` / `Duplicate*` / `NeedsAttention(reason)` / `Error(cause)`) — the generic type keeps the `Duplicate` case's human-readable message as a string the ViewModel supplies, rather than collapsing to a bare "Conflict" (preserving the readability `CLAUDE.md` calls out for `RegisterPatientResult` vs. the generic `OfflineWriteResult`, one layer up).
3. **Patient Listing is sourced from Room, filtered by `visitDate`**, not from `getPatients(page, pageSize)`. `docs/endpoints.md` is explicit that the listing needs "a Room-only offline path" with local BMI classification, and its shape (`{name, age, bmi, status}` per visit date) matches `VisitApi.view`/`PatientListingRowDto`, not the plain patient-registration list. This needs one new `VitalsDao` query (`findByVisitDate`) and one new repository (`PatientListingRepository`), built in Phase 1.
4. **Context-parameterized repositories get resolved once, eagerly, in `PatientRegistrationApp.onCreate()`**, then re-registered as parameter-free Koin singletons (`koin.loadModules(listOf(module { single { resolvedRepo } }))`). This is the same pattern already used for `AppWorkerFactory`. It's what lets ViewModels — which must stay in **common** `shared/src` so the five `feature.*` packages stay portable per `CLAUDE.md`'s package layout — depend on `PatientRepository` etc. with zero `android.content.Context` in their constructors.
5. **"Today"** is obtained via `kotlinx.datetime.Clock` injected into each ViewModel (bound via Koin as `single<Clock> { Clock.System }`), not `Clock.System` called directly in ViewModel bodies — keeps date-dependent logic (age calculation, visit-date stamping, BMI-boundary tests) deterministic in tests via a fake `Clock`.
6. After a successful Vitals submission, the ViewModel re-reads `VitalsRepository.getVitalsForPatient(patientId)` to find today's row and obtain its generated `VitalsId` — `SubmitVitalsResult.Success` doesn't carry the id, and extending that sealed type is out of scope for a screens-and-viewmodels change (it's shared, tested production code from a prior phase).

---

## Phase 0 — Shared form & navigation primitives

No screens yet. Pure common-code infrastructure, fully unit-testable.

### Task 0.1: Add Navigation 3 and Koin-Compose-ViewModel dependencies

**Files:**
- Modify: `libs.versions.toml`
- Modify: `shared/module.yaml`

**Interfaces:**
- Produces: `org.jetbrains.androidx.navigation3:navigation3-ui`, `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3`, `io.insert-koin:koin-compose-viewmodel` available as common dependencies for every later task in this plan.

- [ ] **Step 1:** Add version entries and library aliases to `libs.versions.toml` for `navigation3-ui` (JetBrains Compose Multiplatform build, per the "Navigation 3 in Compose Multiplatform" doc — start from `1.1.1` and bump to whatever is actually latest-stable at implementation time), `lifecycle-viewmodel-navigation3` (co-versioned with the existing `androidx-lifecycle = "2.11.0"` entry — verify that version has this artifact published; if not, pin the nearest published one explicitly), and `koin-compose-viewmodel` (same `4.2.2` line as the existing `koin-core`/`koin-test` entries).
- [ ] **Step 2:** Add the three resulting libraries to `shared/module.yaml`'s top-level `dependencies:` list (common — all three are multiplatform artifacts, not Android-only).
- [ ] **Step 3:** Run `./kotlin build -p android` and confirm dependency resolution succeeds (no code uses the new artifacts yet, so this just proves the coordinates exist).
- [ ] **Step 4: Commit.**

```bash
git add libs.versions.toml shared/module.yaml
git commit -m "build: add Navigation 3 and koin-compose-viewmodel dependencies"
```

### Task 0.2: `Decision` and `FieldValidator`

**Files:**
- Create: `shared/src/feature/common/Decision.kt`
- Test: `shared/test/feature/common/DecisionTest.kt`

**Interfaces:**
- Produces: `sealed interface Decision { data object Valid : Decision; data class Invalid(val reason: String) : Decision }` and `fun interface FieldValidator { fun validate(value: String): Decision }`, consumed by `FieldState` (Task 0.3) and every screen's `Validators` usage.

- [ ] **Step 1: Write the failing test**

```kotlin
class DecisionTest {
    @Test
    fun validatorReturnsInvalidWithReasonForBlankValue() {
        val validator = FieldValidator { value -> if (value.isBlank()) Decision.Invalid("Required") else Decision.Valid }
        assertEquals(Decision.Invalid("Required"), validator.validate(""))
    }

    @Test
    fun validatorReturnsValidForNonBlankValue() {
        val validator = FieldValidator { value -> if (value.isBlank()) Decision.Invalid("Required") else Decision.Valid }
        assertEquals(Decision.Valid, validator.validate("x"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails** — `./kotlin test -m shared -p android` — expected: FAIL, `Decision`/`FieldValidator` unresolved.
- [ ] **Step 3: Implement** `Decision` (sealed interface, `Valid` object + `Invalid(reason: String)`) and `FieldValidator` (fun interface, single method `validate(value: String): Decision`) in `shared/src/feature/common/Decision.kt`.
- [ ] **Step 4: Run test to verify it passes.**
- [ ] **Step 5: Commit.**

### Task 0.3: `FieldState`

**Files:**
- Create: `shared/src/feature/common/FieldState.kt`
- Test: `shared/test/feature/common/FieldStateTest.kt`

**Interfaces:**
- Consumes: `Decision`, `FieldValidator` (Task 0.2).
- Produces: `class FieldState(initialValue: String = "", private val validators: List<FieldValidator> = emptyList())` with `val value: String`, `val touched: Boolean`, `val decision: Decision`, `val errorMessage: String?` (non-null only when `touched && decision is Invalid`), `fun onValueChanged(newValue: String)` (sets value, marks touched), `fun touch()`, `val isValid: Boolean`.

- [ ] **Step 1: Write the failing tests**

```kotlin
class FieldStateTest {
    @Test
    fun untouchedInvalidFieldHasNoErrorMessage() {
        val field = FieldState(validators = listOf(Validators.required()))
        assertEquals(null, field.errorMessage)
        assertEquals(false, field.touched)
    }

    @Test
    fun touchedInvalidFieldExposesTheFirstFailingReason() {
        val field = FieldState(validators = listOf(Validators.required("Required")))
        field.onValueChanged("")
        assertEquals("Required", field.errorMessage)
        assertEquals(false, field.isValid)
    }

    @Test
    fun touchedValidFieldHasNoErrorMessage() {
        val field = FieldState(validators = listOf(Validators.required()))
        field.onValueChanged("Jane")
        assertEquals(null, field.errorMessage)
        assertEquals(true, field.isValid)
    }

    @Test
    fun touchWithoutChangingValueStillRevealsExistingErrors() {
        val field = FieldState(validators = listOf(Validators.required("Required")))
        field.touch()
        assertEquals("Required", field.errorMessage)
    }
}
```

(This also exercises the "untouched shows no error" / "force-touch on submit" Review Focus item — `touch()` is exactly what a screen's Submit handler calls on every field before checking `isValid`.)

- [ ] **Step 2: Run tests to verify they fail.**
- [ ] **Step 3: Implement `FieldState`.** `decision` evaluates `validators` in order and returns the first `Invalid`, else `Valid` (short-circuiting `firstNotNullOfOrNull`). Backing state can be plain `var` properties — no Compose snapshot state is required for this class to work in a ViewModel (see Task 0.X below on how screens observe it) but using `androidx.compose.runtime.mutableStateOf` for `value`/`touched` is fine and keeps Compose recomposition automatic when a screen reads `field.value` directly inside a composable.
- [ ] **Step 4: Run tests to verify they pass.**
- [ ] **Step 5: Commit.**

### Task 0.4: `Validators`

**Files:**
- Create: `shared/src/feature/common/Validators.kt`
- Test: `shared/test/feature/common/ValidatorsTest.kt`

**Interfaces:**
- Consumes: `FieldValidator`, `Decision`.
- Produces: `object Validators { fun required(message: String = "Required"): FieldValidator; fun positiveDecimal(message: String = "Enter a valid number"): FieldValidator; fun patientIdFormat(message: String = "Enter a valid patient ID"): FieldValidator }`.

- [ ] **Step 1: Write the failing tests** — one per validator, each with a passing and a failing input (e.g. `positiveDecimal` on `"70.5"` → `Valid`, on `"-1"` and on `"abc"` → `Invalid`; `required` on `"  "` → `Invalid`). Use the exact test names `requiredRejectsBlankAndWhitespace`, `positiveDecimalRejectsNonNumericAndNonPositive`, `patientIdFormatRejectsBlank` (the exact allowed Patient ID shape isn't specified by the brief beyond "not blank" — don't invent a regex the spec doesn't ask for; `patientIdFormat` can just alias `required` until a real format constraint shows up).
- [ ] **Step 2: Run tests to verify they fail.**
- [ ] **Step 3: Implement each factory function.** `positiveDecimal` parses with `toDoubleOrNull()` and requires `> 0`.
- [ ] **Step 4: Run tests to verify they pass.**
- [ ] **Step 5: Commit.**

### Task 0.5: `SelectionState<T>`

**Files:**
- Create: `shared/src/feature/common/SelectionState.kt`
- Test: `shared/test/feature/common/SelectionStateTest.kt`

**Interfaces:**
- Consumes: `Decision`.
- Produces: `class SelectionState<T>(initial: T? = null, private val requiredMessage: String = "Required")` with `val selected: T?`, `val touched: Boolean`, `val decision: Decision`, `val errorMessage: String?`, `fun select(value: T)`, `fun touch()`, `val isValid: Boolean`.

- [ ] **Step 1: Write the failing tests** — mirror `FieldStateTest`'s three cases (untouched+null → no error; touched+null → error; touched+non-null → valid), using `SelectionState<Gender>` as the concrete type in the test to prove it's generic.
- [ ] **Step 2: Run tests to verify they fail.**
- [ ] **Step 3: Implement.** `decision` is `Valid` if `selected != null`, else `Invalid(requiredMessage)`.
- [ ] **Step 4: Run tests to verify they pass.**
- [ ] **Step 5: Commit.**

### Task 0.6: `SubmissionOutcome` and the four repository-result mappers

**Files:**
- Create: `shared/src/feature/common/SubmissionOutcome.kt`
- Test: `shared/test/feature/common/SubmissionOutcomeTest.kt`

**Interfaces:**
- Consumes: `RegisterPatientResult`, `SubmitVitalsResult`, `SubmitGeneralAssessmentResult`, `SubmitOverweightAssessmentResult` (all in `data`), `NeedsAttentionReason` (`data.sync`).
- Produces: `sealed interface SubmissionOutcome { data class Submitted(val synced: Boolean) : SubmissionOutcome; data class Duplicate(val message: String) : SubmissionOutcome; data class NeedsAttention(val reason: NeedsAttentionReason) : SubmissionOutcome; data class Failed(val cause: Throwable) : SubmissionOutcome }` plus four extension functions: `RegisterPatientResult.toSubmissionOutcome(): SubmissionOutcome`, `SubmitVitalsResult.toSubmissionOutcome(): SubmissionOutcome`, `SubmitGeneralAssessmentResult.toSubmissionOutcome(): SubmissionOutcome`, `SubmitOverweightAssessmentResult.toSubmissionOutcome(): SubmissionOutcome`.

- [ ] **Step 1: Write the failing tests** — one per sealed type's four cases (16 small cases total), e.g.:

```kotlin
@Test
fun duplicatePatientIdMapsToDuplicateWithPatientSpecificMessage() {
    val outcome = RegisterPatientResult.DuplicatePatientId.toSubmissionOutcome()
    assertEquals(SubmissionOutcome.Duplicate("This Patient ID is already registered."), outcome)
}

@Test
fun duplicateVisitDateMapsToDuplicateWithVisitSpecificMessage() {
    val outcome = SubmitVitalsResult.DuplicateVisitDate.toSubmissionOutcome()
    assertEquals(SubmissionOutcome.Duplicate("Vitals have already been submitted for this patient today."), outcome)
}
```

(Repeat the `Success`/`NeedsAttention`/`Error` shape for all four source types; `Success(synced)` → `Submitted(synced)` and `Error(cause)`/`NeedsAttention(reason)` pass through unchanged — those three are mechanical, don't write sixteen near-identical test bodies by hand, write the two that differ per type — the `Duplicate` message — and one each confirming the mechanical pass-through per source type still compiles and round-trips.)

- [ ] **Step 2: Run tests to verify they fail.**
- [ ] **Step 3: Implement** the sealed interface and the four `when`-based extension functions.
- [ ] **Step 4: Run tests to verify they pass.**
- [ ] **Step 5: Commit.**

### Task 0.7: `Clock` binding

**Files:**
- Create: `shared/src/di/ClockModule.kt`

**Interfaces:**
- Produces: Koin `val clockModule = module { single<Clock> { Clock.System } }`, consumed by every ViewModel in Phases 2–6.

- [ ] **Step 1: Implement** `clockModule` in `shared/src/di/ClockModule.kt` (`import kotlinx.datetime.Clock`).
- [ ] **Step 2: Add `clockModule` to `KoinModuleSmokeTest`'s verified module list** (follow that file's existing `verify()`/`verifyAll()` pattern).
- [ ] **Step 3: Run `./kotlin test -m shared -p android`.**
- [ ] **Step 4: Commit.**

---

## Phase 1 — Patient Listing data source

Still no screens — this gives Phase 6 something real to call.

### Task 1.1: `VitalsDao.findByVisitDate`

**Files:**
- Modify: `shared/src/datasource/local/VitalsEntity.kt`
- Modify: `shared/test@android/datasource/local/VitalsDaoTest.kt`

**Interfaces:**
- Produces: `fun findByVisitDate(visitDate: String): Flow<List<VitalsEntity>>` on `VitalsDao`.

- [ ] **Step 1: Write the failing test** `findByVisitDateReturnsOnlyRowsMatchingTheDate` — insert two vitals rows for two different patients on `"2026-10-12"` and one on `"2026-10-11"`, assert `findByVisitDate("2026-10-12").first()` returns exactly the two matching rows.
- [ ] **Step 2: Run** `./kotlin test -m shared -p android` **to verify it fails** (method doesn't exist).
- [ ] **Step 3: Implement** `@Query("SELECT * FROM VitalsEntity WHERE visitDate = :visitDate") fun findByVisitDate(visitDate: String): Flow<List<VitalsEntity>>`.
- [ ] **Step 4: Run test to verify it passes.**
- [ ] **Step 5: Commit.**

### Task 1.2: `PatientListingRow` and `PatientListingRepository`

**Files:**
- Create: `shared/src/domain/PatientListingRow.kt`
- Create: `shared/src/data/PatientListingRepository.kt` (interface + `DefaultPatientListingRepository`)
- Test: `shared/test/data/PatientListingRepositoryTest.kt`

**Interfaces:**
- Consumes: `VitalsDao.findByVisitDate` (Task 1.1), `PatientRepository.getPatient(id): Flow<Patient?>` (existing), `Patient.age(today)`, `classifyBmi` (existing).
- Produces: `data class PatientListingRow(val name: String, val age: Int, val bmi: Double, val status: BmiStatus)`; `interface PatientListingRepository { fun getListing(visitDate: LocalDate, today: LocalDate): Flow<List<PatientListingRow>> }`; `class DefaultPatientListingRepository(private val vitalsDao: VitalsDao, private val patientRepository: PatientRepository) : PatientListingRepository`.

- [ ] **Step 1: Write the failing test**

```kotlin
class PatientListingRepositoryTest {
    @Test
    fun getListingJoinsVitalsAndPatientAndClassifiesBmiLocally() = runTest {
        val vitalsDao = FakeVitalsDao(rowsByDate = mapOf("2026-10-12" to listOf(
            VitalsEntity(id = 1, patientId = "P1", visitDate = "2026-10-12", heightCm = 180.0, weightKg = 70.0, bmi = 21.6, serverId = null, synced = true),
        )))
        val patientRepository = FakePatientRepository(patients = mapOf(PatientId("P1") to testPatient(firstName = "Jane", lastName = "Mdoe", dob = LocalDate(2000, 1, 1))))
        val repository = DefaultPatientListingRepository(vitalsDao, patientRepository)

        val rows = repository.getListing(LocalDate(2026, 10, 12), today = LocalDate(2026, 10, 12)).first()

        assertEquals(listOf(PatientListingRow(name = "Jane Mdoe", age = 26, bmi = 21.6, status = BmiStatus.NORMAL)), rows)
    }

    @Test
    fun getListingReturnsEmptyListWhenNoVisitsMatchTheDate() = runTest {
        val repository = DefaultPatientListingRepository(FakeVitalsDao(emptyMap()), FakePatientRepository(emptyMap()))
        assertEquals(emptyList(), repository.getListing(LocalDate(2026, 10, 12), today = LocalDate(2026, 10, 12)).first())
    }
}
```

(The second test is the Review Focus "empty date → empty state, not stale data" case, pinned at the repository layer where it originates.) `FakeVitalsDao` and the `patients` constructor param on `FakePatientRepository` are small test-only additions this task also creates (`shared/test/data/FakeVitalsDao.kt`; extend `FakePatientRepository`'s constructor with a `patients: Map<PatientId, Patient> = emptyMap()` default and make `getPatient` look it up, instead of always returning `flowOf(null)`).

- [ ] **Step 2: Run test to verify it fails.**
- [ ] **Step 3: Implement.** `getListing` maps `vitalsDao.findByVisitDate(visitDate.toString())`, and for each `VitalsEntity` suspends on `patientRepository.getPatient(PatientId(it.patientId)).first()` to get the `Patient`, assembling `PatientListingRow(name = "${patient.firstName.value} ${patient.lastName.value}", age = patient.age(today), bmi = it.bmi, status = classifyBmi(it.bmi))`.
- [ ] **Step 4: Run test to verify it passes.**
- [ ] **Step 5: Commit.**

### Task 1.3: Wire `PatientListingRepository` into Koin

**Files:**
- Modify: `shared/src@android/di/RepositoryModule.kt`
- Modify: `shared/src@ios/di/RepositoryModule.kt`
- Modify: `shared/test@android/di/AndroidKoinModuleTest.kt`

- [ ] **Step 1: Add** `single<PatientListingRepository> { (context: Context) -> DefaultPatientListingRepository(vitalsDao = get<AppDatabase> { parametersOf(context) }.vitalsDao(), patientRepository = get { parametersOf(context) }) }` to the Android `repositoryModule`, and the parameterless iOS equivalent.
- [ ] **Step 2: Add the new binding to `AndroidKoinModuleTest`'s verified set** (same `verify()`/`injectedParameters` pattern the file already documents for the other four repositories).
- [ ] **Step 3: Run `./kotlin test -p android`.**
- [ ] **Step 4: Commit.**

---

## Phase 2 — Navigation scaffold, Koin ViewModel wiring, Patient Registration screen

First real screen. Establishes the pattern every later phase repeats.

### Task 2.1: `Route` hierarchy

**Files:**
- Create: `shared/src/navigation/Route.kt`

**Interfaces:**
- Produces:
```kotlin
@Serializable
sealed interface Route : NavKey {
    @Serializable data object PatientRegistration : Route
    @Serializable data class Vitals(val patientId: String) : Route
    @Serializable data class GeneralAssessment(val patientId: String, val vitalsId: Long) : Route
    @Serializable data class OverweightAssessment(val patientId: String, val vitalsId: Long) : Route
    @Serializable data class PatientListing(val visitDate: String? = null) : Route
}
```
Route fields are primitives (`String`/`Long`), not domain value classes — domain types (`PatientId`, `VitalsId`) stay framework-free per `CLAUDE.md`; each screen's ViewModel wraps the primitive back into the domain type at its own boundary.

- [ ] **Step 1: Implement** the file above (single-module sealed-type pattern, per the Navigation 3 Compose Multiplatform doc's recommended approach — no manual `SerializersModule` needed since `kotlinx.serialization` handles a sealed hierarchy automatically).
- [ ] **Step 2: Commit** (nothing to test yet — pure data declarations, exercised by Task 2.4's `Navigator` test).

### Task 2.2: `Navigator`

**Files:**
- Create: `shared/src/navigation/Navigator.kt`
- Test: `shared/test/navigation/NavigatorTest.kt`

**Interfaces:**
- Consumes: `Route` (Task 2.1).
- Produces: `class Navigator(startDestination: Route) { val backStack: SnapshotStateList<Route>; fun goTo(route: Route); fun goBack(); fun replaceAllWith(route: Route) }`.

- [ ] **Step 1: Write the failing tests**

```kotlin
class NavigatorTest {
    @Test
    fun goToPushesOntoTheBackStack() {
        val navigator = Navigator(Route.PatientRegistration)
        navigator.goTo(Route.PatientListing())
        assertEquals(listOf(Route.PatientRegistration, Route.PatientListing()), navigator.backStack)
    }

    @Test
    fun goBackRemovesTheLastEntry() {
        val navigator = Navigator(Route.PatientRegistration)
        navigator.goTo(Route.PatientListing())
        navigator.goBack()
        assertEquals(listOf(Route.PatientRegistration), navigator.backStack)
    }

    @Test
    fun replaceAllWithClearsTheEntireStackBeforePushing() {
        val navigator = Navigator(Route.PatientRegistration)
        navigator.goTo(Route.Vitals(patientId = "P1"))
        navigator.replaceAllWith(Route.PatientListing())
        assertEquals(listOf(Route.PatientListing()), navigator.backStack)
    }
}
```

(`replaceAllWith` is what Phases 4/5 use after a successful assessment submit, so the back button never returns to an already-submitted form.)

- [ ] **Step 2: Run tests to verify they fail.**
- [ ] **Step 3: Implement.** `backStack = mutableStateListOf(startDestination)`; `goTo` appends; `goBack` is `removeLastOrNull()`; `replaceAllWith` is `clear(); add(route)`.
- [ ] **Step 4: Run tests to verify they pass.**
- [ ] **Step 5: Commit.**

### Task 2.3: Eagerly resolve repositories in `PatientRegistrationApp`; rebind context-free

**Files:**
- Modify: `androidApp/src/PatientRegistrationApp.kt`

**Interfaces:**
- Produces: `PatientRepository`, `VitalsRepository`, `GeneralAssessmentRepository`, `OverweightAssessmentRepository`, `PatientListingRepository` all resolvable from Koin with **no** runtime parameter, from this point on — this is what Task 2.5's `viewModelModule` relies on.

- [ ] **Step 1:** After the existing `startKoin { modules(...) }.koin` call, add:

```kotlin
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
```

Also add `navigationModule`, `viewModelModule`, and `clockModule` (Task 2.5/0.7) to the original `startKoin { modules(...) }` list.

- [ ] **Step 2:** No new automated test — this file is already documented as deliberately untested (same status as `MainActivity`, per its own doc comment); verify manually once Task 2.6's screen exists by running the app.
- [ ] **Step 3: Commit** together with Task 2.5 (same file, functionally one change).

### Task 2.4: `AppNavHost` and Koin `viewModelModule`/`navigationModule`

**Files:**
- Create: `shared/src/navigation/AppNavHost.kt`
- Create: `shared/src/di/NavigationModule.kt`
- Create: `shared/src/di/ViewModelModule.kt`
- Modify: `shared/src/App.kt`

**Interfaces:**
- Consumes: `Navigator`, `Route` (Task 2.1/2.2), `PatientRegistrationViewModel` (Task 2.6).
- Produces: `@Composable fun AppNavHost(navigator: Navigator = koinInject())`; `val navigationModule = module { single { Navigator(startDestination = Route.PatientRegistration) } }`; `val viewModelModule = module { viewModelOf(::PatientRegistrationViewModel) }` (grows in Phases 3–6).

- [ ] **Step 1: Implement `AppNavHost`** using `entryProvider` DSL, `NavDisplay(backStack = navigator.backStack, onBack = { navigator.goBack() }, entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()), entryProvider = entryProvider { entry<Route.PatientRegistration> { PatientRegistrationScreen(viewModel = koinViewModel { parametersOf(navigator) }) } ... })` — only the `PatientRegistration` entry exists after this task; later phases each add one `entry<Route.X>` line.
- [ ] **Step 2: Replace `App.kt`'s template body** (the "Click me!" demo) with `AppNavHost()`.
- [ ] **Step 3:** No new unit test for this task (it's Compose wiring, verified visually once Task 2.6 lands — see Task 2.7). Confirm it compiles: `./kotlin build -m androidApp`.
- [ ] **Step 4: Commit.**

### Task 2.5: `PatientRegistrationViewModel`

**Files:**
- Create: `shared/src/feature/patientregistration/PatientRegistrationViewModel.kt`
- Modify: `shared/test/data/FakePatientRepository.kt`
- Test: `shared/test/feature/patientregistration/PatientRegistrationViewModelTest.kt`

**Interfaces:**
- Consumes: `PatientRepository.registerPatient`, `Navigator.goTo`, `Clock`, `FieldState`, `SelectionState<Gender>`, `SubmissionOutcome`.
- Produces: `class PatientRegistrationViewModel(private val patientRepository: PatientRepository, private val navigator: Navigator, private val clock: Clock) : ViewModel()` exposing `val patientId: FieldState`, `val firstName: FieldState`, `val lastName: FieldState`, `val dateOfBirth: SelectionState<LocalDate>`, `val gender: SelectionState<Gender>`, `val submissionOutcome: StateFlow<SubmissionOutcome?>`, `val canSubmit: Boolean` (derived from all fields' `isValid`), `fun onSubmit()`.

- [ ] **Step 1: Extend `FakePatientRepository`** with a constructor param `private var resultToReturn: RegisterPatientResult = RegisterPatientResult.Success(synced = true)` (settable via a test-only `fun stubResult(result: RegisterPatientResult)` or just a `var`), replacing the `error(...)` body of `registerPatient` with `resultToReturn`, and capturing the last submitted `Patient` in a `var lastRegistered: Patient? = null` for assertion.
- [ ] **Step 2: Write the failing tests**

```kotlin
class PatientRegistrationViewModelTest {
    @Test
    fun onSubmitWithAllFieldsValidRegistersPatientAndNavigatesToVitals() = runTest {
        val repository = FakePatientRepository()
        val navigator = Navigator(Route.PatientRegistration)
        val viewModel = PatientRegistrationViewModel(repository, navigator, fixedClock(LocalDate(2026, 10, 12)))
        viewModel.patientId.onValueChanged("P1")
        viewModel.firstName.onValueChanged("Jane")
        viewModel.lastName.onValueChanged("Mdoe")
        viewModel.dateOfBirth.select(LocalDate(2000, 1, 1))
        viewModel.gender.select(Gender.FEMALE)

        viewModel.onSubmit()

        assertEquals(PatientId("P1"), repository.lastRegistered?.patientId)
        assertEquals(listOf(Route.Vitals(patientId = "P1")), navigator.backStack.drop(1))
    }

    @Test
    fun onSubmitWithAMissingFieldDoesNotCallTheRepositoryAndTouchesEveryField() = runTest {
        val repository = FakePatientRepository()
        val viewModel = PatientRegistrationViewModel(repository, Navigator(Route.PatientRegistration), fixedClock(LocalDate(2026, 10, 12)))

        viewModel.onSubmit()

        assertEquals(null, repository.lastRegistered)
        assertEquals("Required", viewModel.firstName.errorMessage)
    }

    @Test
    fun duplicatePatientIdSurfacesAsADuplicateSubmissionOutcomeAndDoesNotNavigate() = runTest {
        val repository = FakePatientRepository(resultToReturn = RegisterPatientResult.DuplicatePatientId)
        val navigator = Navigator(Route.PatientRegistration)
        val viewModel = PatientRegistrationViewModel(repository, navigator, fixedClock(LocalDate(2026, 10, 12)))
        viewModel.patientId.onValueChanged("P1"); viewModel.firstName.onValueChanged("Jane")
        viewModel.lastName.onValueChanged("Mdoe"); viewModel.dateOfBirth.select(LocalDate(2000, 1, 1)); viewModel.gender.select(Gender.FEMALE)

        viewModel.onSubmit()

        assertIs<SubmissionOutcome.Duplicate>(viewModel.submissionOutcome.value)
        assertEquals(listOf(Route.PatientRegistration), navigator.backStack)
    }
}
```

(`fixedClock(date)` is a tiny local test helper: `object : Clock { override fun now() = date.atStartOfDayIn(TimeZone.UTC) }`.) These three tests cover the Review Focus "force-touch on submit" and "duplicate blocks navigation" items for this specific screen.

- [ ] **Step 3: Run tests to verify they fail.**
- [ ] **Step 4: Implement `PatientRegistrationViewModel`.** `onSubmit()` touches all five fields/selections, returns early if any `!isValid`, else builds a `Patient` (with `registrationDate = clock.now()...date`), calls `patientRepository.registerPatient(...)`, maps the result via `toSubmissionOutcome()` into `submissionOutcome`, and on `Submitted` calls `navigator.goTo(Route.Vitals(patientId = patientId.value))`.
- [ ] **Step 5: Run tests to verify they pass.**
- [ ] **Step 6: Commit.**

### Task 2.6: `PatientRegistrationScreen`

**Files:**
- Create: `shared/src/feature/patientregistration/PatientRegistrationScreen.kt`
- Modify: `shared/src/navigation/AppNavHost.kt` (wire the real screen into the `entry<Route.PatientRegistration>` block from Task 2.4)

**Interfaces:**
- Consumes: `PatientRegistrationViewModel` (Task 2.5).
- Produces: `@Composable fun PatientRegistrationScreen(viewModel: PatientRegistrationViewModel)`.

- [ ] **Step 1: Implement** a Material 3 form: `OutlinedTextField`s bound to `patientId`/`firstName`/`lastName` (`value = field.value, onValueChange = field::onValueChanged, isError = field.errorMessage != null, supportingText = { field.errorMessage?.let { Text(it) } }`), a date picker for `dateOfBirth` and a segmented/radio control for `gender` (both backed by `SelectionState`, calling `.select(...)` and showing `.errorMessage` the same way), and a `Button(onClick = viewModel::onSubmit)`.
- [ ] **Step 2:** No new unit test (Composable rendering isn't unit-tested anywhere else in this project either — the ViewModel test is the behavioral coverage). Verify by running the app.
- [ ] **Step 3: Commit.**

### Task 2.7: Manual end-to-end verification

- [ ] **Step 1:** Run `./kotlin run -m androidApp` (or `android run`), fill in the Patient Registration form, submit, and confirm the app navigates forward (to a not-yet-built Vitals screen is fine — Phase 3 fills that in; confirm it doesn't crash and the back stack advanced, e.g. via `android screen capture` + `android layout`).
- [ ] **Step 2:** Submit the same Patient ID again (relaunch or navigate back) and confirm a duplicate message appears instead of a crash.
- [ ] **Step 3:** This task produces no commit — it's verification only, feeding into the phase's PR description per `superpowers:verification-before-completion`.

---

## Phase 3 — Vitals screen + ViewModel

Repeats Phase 2's `ViewModel` → `Screen` → `AppNavHost` entry pattern. Only the parts that differ from Phase 2 are spelled out.

### Task 3.1: Extend `FakeVitalsRepository` for configurable results and lookups

**Files:** Modify `shared/test/data/FakeVitalsRepository.kt`

- [ ] Add `resultToReturn: SubmitVitalsResult`, `lastSubmitted: Vitals?` capture (same pattern as Task 2.5's `FakePatientRepository` extension), and a settable `vitalsForPatient: List<Vitals>` backing `getVitalsForPatient`.
- [ ] Commit alongside Task 3.2.

### Task 3.2: `VitalsViewModel`

**Files:**
- Create: `shared/src/feature/vitals/VitalsViewModel.kt`
- Test: `shared/test/feature/vitals/VitalsViewModelTest.kt`

**Interfaces:**
- Produces: `class VitalsViewModel(private val vitalsRepository: VitalsRepository, private val navigator: Navigator, private val clock: Clock, patientId: String) : ViewModel()` exposing `val heightCm: FieldState`, `val weightKg: FieldState`, `val submissionOutcome: StateFlow<SubmissionOutcome?>`, `fun onSubmit()`.

- [ ] **Step 1: Write the failing tests**, covering exactly the Review Focus items that belong to this screen:
  - `onSubmitWithBmiUnder25NavigatesToGeneralAssessment` — stub `heightCm`/`weightKg` to a BMI of e.g. 21.6, `vitalsForPatient` returning one row with `id = VitalsId(7L)` and today's date after submit, assert `navigator.backStack` ends in `Route.GeneralAssessment(patientId = "P1", vitalsId = 7L)`.
  - `onSubmitWithBmiExactly25NavigatesToOverweightAssessment` — height/weight chosen so `calculateBmi(...) == 25.0` exactly (e.g. `180cm`/`81.0kg`), assert the route is `Route.OverweightAssessment`, not `GeneralAssessment` — this is the Review Focus boundary case, pinned at the ViewModel, not just `classifyBmi`.
  - `nonNumericHeightFailsValidationAndNeverCallsTheRepository` — set `heightCm.onValueChanged("abc")`, call `onSubmit()`, assert the fake's `lastSubmitted` stayed `null`.
  - `duplicateVisitDateBlocksNavigation` — stub `resultToReturn = SubmitVitalsResult.DuplicateVisitDate`, assert the back stack didn't change and `submissionOutcome.value is SubmissionOutcome.Duplicate`.
- [ ] **Step 2: Run tests to verify they fail.**
- [ ] **Step 3: Implement.** `onSubmit()` touches both fields, returns early if invalid, else computes `bmi = calculateBmi(heightCm.value.toDouble(), weightKg.value.toDouble())`, builds a `Vitals`, submits, maps to `SubmissionOutcome`; on `Submitted`, looks up `vitalsRepository.getVitalsForPatient(PatientId(patientId)).first()`, finds the row for `clock`'s today, and navigates via `when (classifyBmi(bmi)) { BmiStatus.OVERWEIGHT -> navigator.goTo(Route.OverweightAssessment(...)); else -> navigator.goTo(Route.GeneralAssessment(...)) }` (the `else` branch covers both `UNDERWEIGHT` and `NORMAL`, matching the brief's binary split, not `BmiStatus`'s three-way one).
- [ ] **Step 4: Run tests to verify they pass.**
- [ ] **Step 5: Commit.**

### Task 3.3: `VitalsScreen` + `AppNavHost` entry

**Files:**
- Create: `shared/src/feature/vitals/VitalsScreen.kt`
- Modify: `shared/src/navigation/AppNavHost.kt` (add `entry<Route.Vitals> { key -> VitalsScreen(viewModel = koinViewModel { parametersOf(navigator, key.patientId) }) }`)
- Modify: `shared/src/di/ViewModelModule.kt` (add `viewModelOf(::VitalsViewModel)` — Koin needs `parametersOf` to supply the non-injectable `patientId: String`, so this is declared with `viewModel { params -> VitalsViewModel(get(), get(), get(), params.get()) }` rather than the zero-arg `viewModelOf(::X)` shorthand Task 2.4 used)

- [ ] Implement the form (two numeric `OutlinedTextField`s) and wire the entry. No new test beyond Task 3.2 (same rationale as Task 2.6).
- [ ] Commit.

### Task 3.4: Manual verification

- [ ] Run the app, register a patient, submit Vitals with a BMI `<25` and confirm it would route to General Assessment (screen itself lands in Phase 4 — confirm via logging/the route change if the screen isn't built yet, or sequence Phase 4 immediately after this task in the same session so there's a real screen to land on).

---

## Phase 4 — General Assessment screen + ViewModel

Same pattern as Phase 3.

- **ViewModel** (`feature/generalassessment/GeneralAssessmentViewModel.kt`): constructor takes `(generalAssessmentRepository: GeneralAssessmentRepository, navigator: Navigator, clock: Clock, patientId: String, vitalsId: Long)`. Fields: `val generalHealth: SelectionState<HealthStatus>`, `val everDietedToLoseWeight: SelectionState<Boolean>` (two explicit choices, not a pre-checked switch — defaults to `null`/unselected so the "all fields mandatory" rule holds even for a yes/no question), `val comments: FieldState`. On successful submit, `navigator.replaceAllWith(Route.PatientListing())` (per Design Decision in `Navigator`'s `replaceAllWith`, Task 2.2).
- **Tests** (`GeneralAssessmentViewModelTest.kt`): `onSubmitWithAllFieldsValidSubmitsAndReplacesBackStackWithPatientListing`; `onSubmitWithNoCommentsFailsValidation`; `duplicateVisitDateBlocksNavigation` (same Review Focus item, this screen's own copy — the DB constraint is per-entity, so each screen needs its own test proving its ViewModel doesn't swallow it).
- **Fake extension**: same `resultToReturn`/`lastSubmitted` pattern on `FakeGeneralAssessmentRepository` as Task 3.1.
- **Screen + wiring**: `GeneralAssessmentScreen.kt`, `AppNavHost` entry, `ViewModelModule` addition — identical shape to Task 3.3.
- [ ] Commit per sub-task (test file, ViewModel, screen+wiring — three commits, same granularity as Phase 3).

---

## Phase 5 — Overweight Assessment screen + ViewModel

Identical structure to Phase 4, swapping `everDietedToLoseWeight` for `currentlyUsingDrugs` and `GeneralAssessmentRepository`/`Route.GeneralAssessment` for the Overweight equivalents. Same three tests (valid-submit-navigates, missing-field-blocks, duplicate-blocks), same three commits.

---

## Phase 6 — Patient Listing screen + ViewModel, final wiring

### Task 6.1: `PatientListingViewModel`

**Files:**
- Create: `shared/src/feature/patientlisting/PatientListingViewModel.kt`
- Test: `shared/test/feature/patientlisting/PatientListingViewModelTest.kt`

**Interfaces:**
- Produces: `class PatientListingViewModel(private val patientListingRepository: PatientListingRepository, private val navigator: Navigator, private val clock: Clock) : ViewModel()` exposing `val selectedDate: StateFlow<LocalDate>`, `val rows: StateFlow<List<PatientListingRow>>`, `fun onDateSelected(date: LocalDate)`, `fun onAddPatientClicked()`.

- [ ] **Step 1: Write the failing tests**:
  - `defaultsToTodayAndLoadsTodaysRows` — construct with a fake repository seeded for today's date, assert `rows.value` matches after the test's dispatcher runs the initial load.
  - `changingTheDateReloadsAndReplacesTheRowsEntirely` — select a different date with a different (or empty) seeded result, assert `rows.value` reflects *only* the new date's rows, not a union with the old ones (the Review Focus "empty state, not stale" case, pinned here at the ViewModel since this is where "reload replaces" actually has to be implemented, not just at the repository which already returns the correct `Flow` per date).
  - `onAddPatientClickedNavigatesToPatientRegistration`.
- [ ] **Step 2: Run tests to verify they fail.**
- [ ] **Step 3: Implement.** `selectedDate` as a `MutableStateFlow(clock-derived today)`; `rows` as `selectedDate.flatMapLatest { date -> patientListingRepository.getListing(date, today) }.stateIn(viewModelScope, ...)` — `flatMapLatest` is what makes a date change supersede (not merge with) the previous query's `Flow`, satisfying the "replaces entirely" requirement structurally rather than by manual bookkeeping.
- [ ] **Step 4: Run tests to verify they pass.**
- [ ] **Step 5: Commit.**

### Task 6.2: `PatientListingScreen` + final `AppNavHost`/`ViewModelModule` wiring

**Files:**
- Create: `shared/src/feature/patientlisting/PatientListingScreen.kt`
- Modify: `shared/src/navigation/AppNavHost.kt`
- Modify: `shared/src/di/ViewModelModule.kt`
- Modify: `shared/src/navigation/Navigator.kt` default start destination stays `Route.PatientRegistration` (first-run flow) — **not** `Route.PatientListing` (open question below if the brief implies the app should instead open on the listing; flagged, not decided silently).

- [ ] Implement a date picker + `LazyColumn` of `PatientListingRow`s (name, age, bmi, status) and a `FloatingActionButton` calling `onAddPatientClicked`.
- [ ] Wire the `entry<Route.PatientListing>` in `AppNavHost` and the `viewModelOf`/`viewModel { params -> ... }` entry in `ViewModelModule`.
- [ ] Commit.

### Task 6.3: Full end-to-end manual verification

- [ ] Run the complete flow on a device/emulator via `android run`: register a patient → submit Vitals with BMI `<25` → submit General Assessment → land on Patient Listing and see the new row. Repeat with a BMI `≥25` patient to confirm the Overweight branch. Use `android screen capture`/`android layout` to confirm each screen renders as expected.
- [ ] Confirm `./kotlin test -p android` and `scripts/ktlint.sh` both pass on the full branch before opening the PR.
- [ ] This task produces no commit of its own.

---

## Open question (not decided by this plan)

Should the app's start destination be `Route.PatientListing` (an already-in-use clinic app reopening to "today's patients") rather than `Route.PatientRegistration`? The assignment brief describes five screens without specifying a home screen. This plan defaults to `PatientRegistration` as the start destination (matches a fresh-install/demo-walkthrough use case, which is what the grading walkthrough actually is), but it's a one-line change in `NavigationModule`'s `Navigator(startDestination = ...)` if the reviewer prefers otherwise — flag it rather than silently picking.
