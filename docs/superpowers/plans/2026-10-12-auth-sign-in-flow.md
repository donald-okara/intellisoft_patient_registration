# Auth Sign-In Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Build the sign-up/sign-in screens, a startup auth check, and sign-out, so the app
sends a real `Authorization` header on every API call instead of none at all (its current state).

**Architecture:** `AuthRepository` wraps `AuthApi` + `CachedAuthTokenProvider`, mapping HTTP
failures onto domain-named result types (`SignInResult`/`SignUpResult`), the same shape
`RegisterPatientResult` already uses. A new `Route.Splash` does the one-time async "is there a
saved token" check via `CachedAuthTokenProvider.hydrate()`, then routes to `Route.SignIn` or the
authenticated home (`Route.PatientListing`). Screens/ViewModels follow this project's existing
MVI + Route/Component split exactly, reusing `FieldState`/`Validators`/`SubmissionOutcome` as-is.

**Tech Stack:** Same as the rest of this project - Koin 4.2.2, Compose Multiplatform, Navigation 3,
kotlinx.datetime, Ktor 3.6.0 (`expectSuccess = true`).

**Spec:** `docs/superpowers/specs/2026-10-12-auth-sign-in-flow-design.md` and
`docs/endpoints.md`'s Authentication section (confirms there is no refresh-token endpoint).

## Global Constraints

- No refresh-token handling - there's nothing on the backend to refresh against. Every
  token-expiry case ends at Sign In, not a silent renewal.
- No global 401 interceptor. The existing per-form `NeedsAttention(Unauthorized)` handling is
  unchanged; sign-out is a manual action on Patient Listing only.
- `SignupRequestDto` fields are exactly `email`/`firstname`/`lastname`/`password` - no
  `confirmPassword`, nothing invented beyond the DTO.
- `Validators.required()` only for email/password fields - no email-format regex invented beyond
  what the spec asks for (matches `patientIdFormat`'s own precedent).
- ktlint (`scripts/ktlint.sh`) and `./kotlin test -p android` must pass before merge; everything
  lands via its own branch + PR; no direct commits to `main`.
- TDD, result-based: write the failing test first, assert on returned values/final state.
- No `Co-Authored-By`/Claude attribution in commits or PR descriptions.

## Review Focus

- A 401 from `user/signin` (wrong password) must surface as `SignInResult.InvalidCredentials`,
  not `Error` - the two are handled differently (inline correctable message vs. generic failure),
  and `ClientRequestException`'s status code is the only way to tell them apart.
- A 422 from `user/signup` (duplicate email) must surface as `SignUpResult.EmailAlreadyRegistered`,
  not silently fall into `Error` - same reasoning as above, one layer up.
- `SplashViewModel` must not navigate before `hydrate()` completes - a naive implementation that
  reads `currentToken()` before the suspend call finishes would always route to Sign In, token or
  not, since the in-memory cache starts empty every launch.
- Submitting Sign In/Sign Up with a blank field must force-touch every field and block the
  repository call entirely - the same "revealed on submit, not before" rule every existing form
  in this project already follows, easy to forget on a new screen written from scratch.
- Wiring a real `AuthTokenProvider` into `httpClientModule` must happen *before*
  `PatientRegistrationApp.onCreate()` resolves any repository - resolving a repository
  transitively forces `HttpClient` to resolve, which calls `provideHttpClient(get(), get())`;
  if `CachedAuthTokenProvider` hasn't been rebound context-free yet at that point, `get()` has no
  way to supply the `TokenStorage` runtime parameter `authModule`'s original binding needs, and
  the app would silently fall back to no-auth-header forever (no crash, no test catches it -
  `PatientRegistrationApp` is deliberately untested glue). Task 3 below pins this ordering exactly.

---

## Phase 1 — Auth data layer

### Task 1.1: `SignInResult`/`SignUpResult` and `AuthRepository`

**Files:**
- Create: `shared/src/data/AuthResult.kt`
- Create: `shared/src/data/AuthRepository.kt` (interface + `DefaultAuthRepository`)
- Create: `shared/test/data/FakeAuthApi.kt`
- Test: `shared/test/data/DefaultAuthRepositoryTest.kt`

**Interfaces:**
- Consumes: `AuthApi.signin`/`AuthApi.signup` (existing), `CachedAuthTokenProvider.setToken`
  (existing, `suspend fun setToken(token: String)`), `SigninRequestDto`/`SignupRequestDto`/
  `AuthDataDto`/`ProceedMessageDataDto` (existing DTOs).
- Produces:
  ```kotlin
  sealed interface SignInResult {
      data object Authenticated : SignInResult
      data object InvalidCredentials : SignInResult
      data class Error(val cause: Throwable) : SignInResult
  }
  sealed interface SignUpResult {
      data object AccountCreated : SignUpResult
      data object EmailAlreadyRegistered : SignUpResult
      data class Error(val cause: Throwable) : SignUpResult
  }
  interface AuthRepository {
      suspend fun signIn(email: String, password: String): SignInResult
      suspend fun signUp(email: String, firstname: String, lastname: String, password: String): SignUpResult
  }
  class DefaultAuthRepository(authApi: AuthApi, cachedAuthTokenProvider: CachedAuthTokenProvider) : AuthRepository
  ```
  Consumed by Task 6's `SignInViewModel` and Task 7's `SignUpViewModel`.

- [x] **Step 1: Write `FakeAuthApi`** (`shared/test/data/FakeAuthApi.kt`), mirroring
  `FakePatientApi`'s shape:
  ```kotlin
  class FakeAuthApi(
      private val signinResponse: suspend (SigninRequestDto) -> ApiResponse<AuthDataDto> = {
          ApiResponse("success", true, 200, AuthDataDto(1, "Jane", it.email, "now", "now", "token-123"))
      },
      private val signupResponse: suspend (SignupRequestDto) -> ApiResponse<ProceedMessageDataDto> = {
          ApiResponse("success", true, 200, ProceedMessageDataDto(proceed = 1, message = "ok"))
      },
  ) : AuthApi {
      override suspend fun signin(request: SigninRequestDto) = signinResponse(request)
      override suspend fun signup(request: SignupRequestDto) = signupResponse(request)
  }
  ```
- [x] **Step 2: Write the failing tests**
  ```kotlin
  class DefaultAuthRepositoryTest {
      @Test
      fun signInWithValidCredentialsSavesTheTokenAndReturnsAuthenticated() = runTest {
          val tokenStorage = FakeTokenStorage()
          val authApi = FakeAuthApi()
          val repository = DefaultAuthRepository(authApi, CachedAuthTokenProvider(tokenStorage))

          val result = repository.signIn("jane@example.com", "password123")

          assertEquals(SignInResult.Authenticated, result)
          assertEquals("token-123", tokenStorage.getToken())
      }

      @Test
      fun signInWithWrongCredentialsReturnsInvalidCredentials() = runTest {
          val authApi = FakeAuthApi(signinResponse = { throwUnauthorized() })
          val repository = DefaultAuthRepository(authApi, CachedAuthTokenProvider(FakeTokenStorage()))

          assertEquals(SignInResult.InvalidCredentials, repository.signIn("jane@example.com", "wrong"))
      }

      @Test
      fun signUpWithAnAlreadyRegisteredEmailReturnsEmailAlreadyRegistered() = runTest {
          val authApi = FakeAuthApi(signupResponse = { throwUnprocessable() })
          val repository = DefaultAuthRepository(authApi, CachedAuthTokenProvider(FakeTokenStorage()))

          val result = repository.signUp("jane@example.com", "Jane", "Mdoe", "password123")

          assertEquals(SignUpResult.EmailAlreadyRegistered, result)
      }

      @Test
      fun signUpOnSuccessReturnsAccountCreatedAndDoesNotTouchTheToken() = runTest {
          val tokenStorage = FakeTokenStorage()
          val repository = DefaultAuthRepository(FakeAuthApi(), CachedAuthTokenProvider(tokenStorage))

          val result = repository.signUp("jane@example.com", "Jane", "Mdoe", "password123")

          assertEquals(SignUpResult.AccountCreated, result)
          assertNull(tokenStorage.getToken())
      }
  }
  ```
  `throwUnauthorized()`/`throwUnprocessable()` are small local test helpers that throw a real
  `ClientRequestException` with the matching `HttpStatusCode` (build via Ktor's `MockEngine` the
  same way `KtorPatientApiTest` already does, or construct the exception directly if that proves
  simpler - either way, this task also creates a minimal `FakeTokenStorage`
  (`shared/test/data/FakeTokenStorage.kt`: an in-memory `MutableMap`-backed `TokenStorage`, reused
  by Task 2's `SplashViewModelTest` too).
- [x] **Step 3: Run tests to verify they fail.**
- [x] **Step 4: Implement `AuthResult.kt`** (the two sealed interfaces above) and
  `AuthRepository.kt`. `DefaultAuthRepository.signIn` calls `authApi.signin(SigninRequestDto(email,
  password))`, and on success calls `cachedAuthTokenProvider.setToken(response.data.accessToken)`
  before returning `Authenticated`. Catches mirror `classifyPush`'s shape: `ClientRequestException`
  with `HttpStatusCode.Unauthorized` → `InvalidCredentials` (signIn) / anything else 4xx →
  `EmailAlreadyRegistered` (signUp, the only 4xx signup actually returns per the collection) →
  else `ServerResponseException`/`IOException` → `Error(cause)`. Explicitly rethrow
  `CancellationException` in both methods' catch blocks, matching `offlineFirstWrite`'s
  convention.
- [x] **Step 5: Run tests to verify they pass.**
- [x] **Step 6: Commit.**

---

## Phase 2 — Real `TokenStorage` Koin binding

### Task 2.1: `buildTokenDataStore` (Android) and `tokenStorageModule`

**Files:**
- Create: `shared/src@android/datasource/local/TokenDataStore.kt`
- Create: `shared/src@android/di/TokenStorageModule.kt`
- Create: `shared/src@ios/di/TokenStorageModule.kt`
- Modify: `shared/test@android/di/AndroidKoinModuleTest.kt`

**Interfaces:**
- Consumes: `EncryptedTokenStorage` (existing, `(dataStore: DataStore<Preferences>, encryptor:
  FieldEncryptor)`), `KeychainTokenStorage` (existing, parameterless), `tokenEncryptorQualifier`
  (existing, in `cryptoModule`).
- Produces: `fun buildTokenDataStore(context: Context): DataStore<Preferences>`;
  `val tokenStorageModule` (Android: `single<TokenStorage> { (context: Context) -> ... }`; iOS:
  `single<TokenStorage> { KeychainTokenStorage() }`) - consumed by Task 3's
  `PatientRegistrationApp.onCreate()` change.

- [x] **Step 1: Implement `buildTokenDataStore`** in the new Android file, following
  `EncryptedTokenStorageTest`'s own production-equivalent shape:
  ```kotlin
  fun buildTokenDataStore(context: Context): DataStore<Preferences> =
      PreferenceDataStoreFactory.createWithPath(
          produceFile = { context.filesDir.resolve("auth_token.preferences_pb").absolutePath.toPath() },
      )
  ```
  (No test for this one-liner - it's exercised end-to-end by Task 2.1's own Koin verify test
  below, same rationale `buildAppDatabase` already has no standalone test.)
- [x] **Step 2: Implement `tokenStorageModule`** (Android):
  ```kotlin
  val tokenStorageModule =
      module {
          single<TokenStorage> { (context: Context) ->
              EncryptedTokenStorage(dataStore = buildTokenDataStore(context), encryptor = get(tokenEncryptorQualifier))
          }
      }
  ```
  and (iOS): `val tokenStorageModule = module { single<TokenStorage> { KeychainTokenStorage() } }`.
- [x] **Step 3: Add a verify test to `AndroidKoinModuleTest`**:
  ```kotlin
  private val tokenStorageInjections =
      injectedParameters(definition<EncryptedTokenStorage>(FieldEncryptor::class))

  @OptIn(KoinExperimentalAPI::class)
  @Test
  fun tokenStorageModuleResolvesAgainstCryptoModule() {
      listOf(cryptoModule, tokenStorageModule).verifyAll(injections = tokenStorageInjections)
  }
  ```
  (Same blind-spot rationale as `mapperInjections` - `verify()` can't see the qualified `get()`
  call inside the lambda body.)
- [x] **Step 4: Run `./kotlin test -p android`.**
- [x] **Step 5: Commit.**

---

## Phase 3 — Wire the real `AuthTokenProvider` into `httpClientModule`

### Task 3.1: `authModule` binds `AuthTokenProvider`; `PatientRegistrationApp` rebind ordering

**Files:**
- Modify: `shared/src/di/AuthModule.kt`
- Modify: `androidApp/src/PatientRegistrationApp.kt`
- Modify: `shared/test/di/KoinModuleSmokeTest.kt`

**Interfaces:**
- Consumes: `tokenStorageModule` (Task 2.1), `AuthTokenProvider` (existing, `fun interface { fun
  currentToken(): String? }`).
- Produces: `CachedAuthTokenProvider` resolvable context-free as **both** `CachedAuthTokenProvider`
  (for Task 8's sign-out) and `AuthTokenProvider` (for `httpClientModule`), from the point
  `PatientRegistrationApp.onCreate()` finishes its rebind - consumed by every later task.

- [x] **Step 1: Add `bind AuthTokenProvider::class`** to `authModule`'s existing single:
  ```kotlin
  val authModule =
      module {
          single { (tokenStorage: TokenStorage) -> CachedAuthTokenProvider(tokenStorage) } bind AuthTokenProvider::class
      }
  ```
- [x] **Step 2: Update `KoinModuleSmokeTest.authModuleResolves`** - no behavior change to the
  test itself, just confirm it still passes after Step 1 (the `bind` clause doesn't change what
  `verify()` checks here).
- [x] **Step 3: Rewrite `PatientRegistrationApp.onCreate()`'s eager-resolve block.** This is the
  one exact-ordering change the Review Focus section calls out - the auth rebind must complete in
  its own `loadModules` call *before* any repository is resolved, because resolving a repository
  transitively forces `HttpClient` to resolve, which needs `AuthTokenProvider` to already be
  context-free by then:
  ```kotlin
  val tokenStorage = koin.get<TokenStorage> { parametersOf(this) }
  val cachedAuthTokenProvider = koin.get<CachedAuthTokenProvider> { parametersOf(tokenStorage) }

  koin.loadModules(
      listOf(
          module {
              single { cachedAuthTokenProvider } bind AuthTokenProvider::class
          },
      ),
  )

  // Only now is it safe to resolve repositories - their API clients pull HttpClient, which pulls
  // AuthTokenProvider, which must already be rebound context-free (see above).
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
  ```
  Also add `tokenStorageModule` to the original `startKoin { modules(...) }` list (alongside
  `httpClientModule`, `apiModule`, `authModule`, etc. - `authModule` and `httpClientModule` are
  already there from earlier phases).
- [x] **Step 4:** No new automated test (same documented status as the rest of
  `PatientRegistrationApp` - this is exactly the ordering the Review Focus section explains why
  no test would catch if wrong). Verify by running the app once real device/emulator access
  exists (tracked in Phase 9); until then, rely on the reasoning above and a careful reread of
  this diff during review.
- [x] **Step 5: Run `./kotlin build -m androidApp` to confirm it still compiles.**
- [x] **Step 6: Commit.**

---

## Phase 4 — Routes

### Task 4.1: `Route.Splash`/`Route.SignIn`/`Route.SignUp`; `Navigator` start destination

**Files:**
- Modify: `shared/src/navigation/Route.kt`
- Modify: `shared/src/di/NavigationModule.kt`

**Interfaces:**
- Produces: `Route.Splash`, `Route.SignIn`, `Route.SignUp` (all `data object`, no payload) added
  to the existing `Route` sealed interface - consumed by Tasks 5-8.

- [x] **Step 1: Add the three `data object` entries** to `Route` in `Route.kt`.
- [x] **Step 2: Change `navigationModule`'s `Navigator(startDestination = ...)`** from
  `Route.PatientRegistration` to `Route.Splash`.
- [x] **Step 3:** No new test (`Route` itself has none today either - exercised by `NavigatorTest`
  and whichever ViewModel test references a given route). Run `./kotlin test -p android` to
  confirm nothing else assumed the old start destination.
- [x] **Step 4: Commit.**

---

## Phase 5 — Splash screen + ViewModel

### Task 5.1: `SplashViewModel`

**Files:**
- Create: `shared/src/feature/splash/SplashViewModel.kt`
- Test: `shared/test/feature/splash/SplashViewModelTest.kt`

**Interfaces:**
- Consumes: `CachedAuthTokenProvider.hydrate()`/`.currentToken()` (existing), `Navigator.replaceAllWith`
  (existing), `Route.SignIn`/`Route.PatientListing` (Task 4.1).
- Produces: `class SplashViewModel(private val cachedAuthTokenProvider: CachedAuthTokenProvider,
  private val navigator: Navigator) : ViewModel()` - no public state beyond the navigation side
  effect; consumed by Task 5.2's screen only for construction via Koin.

- [x] **Step 1: Write the failing tests**
  ```kotlin
  class SplashViewModelTest {
      @Test
      fun aSavedTokenNavigatesToPatientListing() = runTest {
          val tokenStorage = FakeTokenStorage().apply { saveToken("token-123") }
          val navigator = Navigator(Route.Splash)

          SplashViewModel(CachedAuthTokenProvider(tokenStorage), navigator)
          advanceUntilIdle()

          assertEquals(listOf(Route.PatientListing()), navigator.backStack.toList())
      }

      @Test
      fun noSavedTokenNavigatesToSignIn() = runTest {
          val navigator = Navigator(Route.Splash)

          SplashViewModel(CachedAuthTokenProvider(FakeTokenStorage()), navigator)
          advanceUntilIdle()

          assertEquals(listOf(Route.SignIn), navigator.backStack.toList())
      }
  }
  ```
  (Reuses `FakeTokenStorage` from Task 1.1. `advanceUntilIdle()` needs the same
  `UnconfinedTestDispatcher`-on-Main setup `PatientListingViewModelTest` already uses for a
  `viewModelScope.launch` to actually run inside `runTest`.)
- [x] **Step 2: Run tests to verify they fail.**
- [x] **Step 3: Implement.** `init { viewModelScope.launch { cachedAuthTokenProvider.hydrate();
  navigator.replaceAllWith(if (cachedAuthTokenProvider.currentToken() != null)
  Route.PatientListing() else Route.SignIn) } }`.
- [x] **Step 4: Run tests to verify they pass.**
- [x] **Step 5: Commit.**

### Task 5.2: `SplashScreen` + `AppNavHost`/`ViewModelModule` wiring

**Files:**
- Create: `shared/src/feature/splash/SplashScreen.kt`
- Modify: `shared/src/navigation/AppNavHost.kt`
- Modify: `shared/src/di/ViewModelModule.kt`

- [x] **Step 1: Implement `SplashScreen(viewModel: SplashViewModel)`** - a single centered
  `CircularProgressIndicator` inside `AppScreenColumn` (or a bare `Box`, implementer's call -
  there's no form content and nothing to preview meaningfully beyond the one visual state). The
  `viewModel` parameter is required even though the screen reads no state from it - injecting it
  via Koin inside the Compose tree is what constructs `SplashViewModel` and runs its `init{}`
  (the hydrate-then-navigate logic) in the first place; a parameterless `SplashScreen()` would
  never construct the ViewModel at all.
- [x] **Step 2: Add `entry<Route.Splash> { SplashScreen(viewModel = koinViewModel {
  parametersOf(navigator) }) }`** to `AppNavHost` (matching every other entry's
  `parametersOf(navigator, ...)` call style) and `viewModelOf(::SplashViewModel)` to
  `viewModelModule`.
- [x] **Step 3:** No new test (Compose wiring, same rationale as every other screen's own
  wiring task in this project). Run `./kotlin build -m androidApp`.
- [x] **Step 4: Commit.**

---

## Phase 6 — Sign In screen + ViewModel

### Task 6.1: `SignInViewModel`

**Files:**
- Create: `shared/src/feature/signin/SignInViewModel.kt`
- Test: `shared/test/feature/signin/SignInViewModelTest.kt`
- Modify: `shared/test/data/FakeAuthRepository.kt` (create)

**Interfaces:**
- Consumes: `AuthRepository.signIn` (Task 1.1), `FieldState`/`Validators.required()` (existing),
  `SubmissionOutcome` (existing), `Navigator.replaceAllWith`/`Route.SignIn`/`Route.SignUp`/
  `Route.PatientListing` (Task 4.1).
- Produces: `class SignInViewModel(private val authRepository: AuthRepository, private val
  navigator: Navigator) : ViewModel()` exposing `val email: FieldState`, `val password:
  FieldState`, `val submissionOutcome: StateFlow<SubmissionOutcome?>`, `fun onSubmit()`, `fun
  onSignUpClicked()`.

- [x] **Step 1: Create `FakeAuthRepository`** (`shared/test/data/FakeAuthRepository.kt`): a
  settable `signInResult: SignInResult`/`signUpResult: SignUpResult` pair (default
  `Authenticated`/`AccountCreated`), capturing the last submitted email/password for assertion -
  same shape as every other `Fake*Repository` in this project.
- [x] **Step 2: Write the failing tests**
  ```kotlin
  class SignInViewModelTest {
      @Test
      fun onSubmitWithValidCredentialsNavigatesToPatientListing() = runTest {
          val repository = FakeAuthRepository()
          val navigator = Navigator(Route.SignIn)
          val viewModel = SignInViewModel(repository, navigator)
          viewModel.email.onValueChanged("jane@example.com")
          viewModel.password.onValueChanged("password123")

          viewModel.onSubmit()

          assertEquals(listOf(Route.PatientListing()), navigator.backStack.toList())
      }

      @Test
      fun onSubmitWithAMissingFieldDoesNotCallTheRepositoryAndTouchesEveryField() = runTest {
          val repository = FakeAuthRepository()
          val viewModel = SignInViewModel(repository, Navigator(Route.SignIn))

          viewModel.onSubmit()

          assertEquals(null, repository.lastSignIn)
          assertEquals("Required", viewModel.email.errorMessage)
      }

      @Test
      fun invalidCredentialsSurfacesAsADuplicateSubmissionOutcomeAndDoesNotNavigate() = runTest {
          val repository = FakeAuthRepository(signInResult = SignInResult.InvalidCredentials)
          val navigator = Navigator(Route.SignIn)
          val viewModel = SignInViewModel(repository, navigator)
          viewModel.email.onValueChanged("jane@example.com")
          viewModel.password.onValueChanged("wrong")

          viewModel.onSubmit()

          assertIs<SubmissionOutcome.Duplicate>(viewModel.submissionOutcome.value)
          assertEquals(listOf(Route.SignIn), navigator.backStack.toList())
      }
  }
  ```
- [x] **Step 3: Run tests to verify they fail.**
- [x] **Step 4: Implement.** `onSubmit()` touches both fields, returns early if either
  `!isValid`, else calls `authRepository.signIn(email.value, password.value)`; maps
  `Authenticated` → `navigator.replaceAllWith(Route.PatientListing())`, `InvalidCredentials` →
  `submissionOutcome.value = SubmissionOutcome.Duplicate("Invalid email or password.")`, `Error`
  → `SubmissionOutcome.Failed(cause)`. `onSignUpClicked()` calls `navigator.goTo(Route.SignUp)`.
- [x] **Step 5: Run tests to verify they pass.**
- [x] **Step 6: Commit.**

### Task 6.2: `SignInScreen` + `AppNavHost`/`ViewModelModule` wiring

**Files:**
- Create: `shared/src/feature/signin/SignInScreen.kt`
- Modify: `shared/src/navigation/AppNavHost.kt`
- Modify: `shared/src/di/ViewModelModule.kt`

- [x] **Step 1: Implement** using the existing `FormScreenColumn`/`FieldStateOutlinedTextField`/
  `SubmissionOutcomeCard` segments (`feature.common`) the four existing forms already share - two
  text fields (email, password - `visualTransformation = PasswordVisualTransformation()` on the
  password field), a submit button, a "Don't have an account? Sign up" text button calling
  `onSignUpClicked`. Add `@Preview` composables per the standing instruction from the prior
  session (valid state, missing-field state, invalid-credentials state).
- [x] **Step 2: Add `entry<Route.SignIn> { SignInScreen(viewModel = koinViewModel {
  parametersOf(navigator) }) }`** to `AppNavHost` (matching every other entry's
  `parametersOf(navigator, ...)` call style) and `viewModelOf(::SignInViewModel)` to
  `viewModelModule`.
- [x] **Step 3:** No new test (Compose wiring). Run `./kotlin build -m androidApp`.
- [x] **Step 4: Commit.**

---

## Phase 7 — Sign Up screen + ViewModel

Mirrors Phase 6 exactly, swapping in `signUp`/`SignUpResult`/`Route.SignUp`.

### Task 7.1: `SignUpViewModel`

**Files:**
- Create: `shared/src/feature/signup/SignUpViewModel.kt`
- Test: `shared/test/feature/signup/SignUpViewModelTest.kt`

**Interfaces:**
- Produces: `class SignUpViewModel(private val authRepository: AuthRepository, private val
  navigator: Navigator) : ViewModel()` exposing `val email: FieldState`, `val firstname:
  FieldState`, `val lastname: FieldState`, `val password: FieldState`, `val submissionOutcome:
  StateFlow<SubmissionOutcome?>`, `fun onSubmit()`, `fun onSignInClicked()`.

- [x] **Step 1: Write the failing tests** (same three-case shape as Task 6.1's test, using
  `FakeAuthRepository`):
  - `onSubmitWithAllFieldsValidNavigatesToSignIn` - asserts `navigator.backStack` ends in
    `Route.SignIn` after `AccountCreated` (via `replaceAllWith`, since an abandoned sign-up
    shouldn't be reachable via back from Sign In).
  - `onSubmitWithAMissingFieldDoesNotCallTheRepositoryAndTouchesEveryField`.
  - `emailAlreadyRegisteredSurfacesAsADuplicateSubmissionOutcomeAndDoesNotNavigate`.
- [x] **Step 2: Run tests to verify they fail.**
- [x] **Step 3: Implement.** Same shape as `SignInViewModel.onSubmit()`, four fields instead of
  two, `AccountCreated` → `navigator.replaceAllWith(Route.SignIn)`, `EmailAlreadyRegistered` →
  `SubmissionOutcome.Duplicate("This email is already registered.")`, `Error` →
  `SubmissionOutcome.Failed(cause)`. `onSignInClicked()` calls `navigator.goTo(Route.SignIn)`.
- [x] **Step 4: Run tests to verify they pass.**
- [x] **Step 5: Commit.**

### Task 7.2: `SignUpScreen` + `AppNavHost`/`ViewModelModule` wiring

**Files:**
- Create: `shared/src/feature/signup/SignUpScreen.kt`
- Modify: `shared/src/navigation/AppNavHost.kt`
- Modify: `shared/src/di/ViewModelModule.kt`

- [x] Implement the four-field form (same reused segments as Task 6.2) plus a "Already have an
  account? Sign in" text button calling `onSignInClicked`, with `@Preview`s. Add
  `entry<Route.SignUp> { SignUpScreen(viewModel = koinViewModel { parametersOf(navigator) }) }`
  to `AppNavHost` (same `parametersOf(navigator, ...)` style as every other entry) and
  `viewModelOf(::SignUpViewModel)` to `viewModelModule`.
- [x] Commit.

---

## Phase 8 — Sign out

### Task 8.1: `PatientListingViewModel` sign-out

**Files:**
- Modify: `shared/src/feature/patientlisting/PatientListingViewModel.kt`
- Modify: `shared/src/feature/patientlisting/PatientListingScreen.kt`
- Modify: `shared/test/feature/patientlisting/PatientListingViewModelTest.kt`
- Modify: `shared/src/di/ViewModelModule.kt` (gains a `CachedAuthTokenProvider` dependency -
  `viewModelOf(::PatientListingViewModel)` keeps working unchanged since `CachedAuthTokenProvider`
  is plain-resolvable by this point, per Task 3.1)

**Interfaces:**
- Consumes: `CachedAuthTokenProvider.clearToken()` (existing).
- Produces: `PatientListingViewModel` constructor gains `cachedAuthTokenProvider:
  CachedAuthTokenProvider`; `PatientListingIntent` gains `data object SignOutClicked`.

- [x] **Step 1: Write the failing test**
  ```kotlin
  @Test
  fun signOutClickedClearsTheTokenAndNavigatesToSignIn() = runTest {
      val tokenStorage = FakeTokenStorage().apply { saveToken("token-123") }
      val cachedAuthTokenProvider = CachedAuthTokenProvider(tokenStorage).apply { hydrate() }
      val navigator = Navigator(Route.PatientListing())
      val viewModel = PatientListingViewModel(FakePatientListingRepository(), navigator, fixedClock(LocalDate(2026, 10, 12)), cachedAuthTokenProvider)

      viewModel.onIntent(PatientListingIntent.SignOutClicked)

      assertEquals(null, tokenStorage.getToken())
      assertEquals(listOf(Route.SignIn), navigator.backStack.toList())
  }
  ```
  (Existing tests in this file need their `PatientListingViewModel(...)` constructor calls updated
  with a fourth `CachedAuthTokenProvider` argument - a `FakeTokenStorage()`-backed one with no
  saved token is enough for the three pre-existing tests, which don't exercise sign-out.)
- [x] **Step 2: Run tests to verify the new one fails.**
- [x] **Step 3: Implement.** Add the constructor param; `onIntent`'s `when` gains
  `PatientListingIntent.SignOutClicked -> viewModelScope.launch {
  cachedAuthTokenProvider.clearToken(); navigator.replaceAllWith(Route.SignIn) }`.
- [x] **Step 4: Run tests to verify they pass.**
- [x] **Step 5: Add a sign-out icon** to `PatientListingScreen`'s `AppScreenColumn` (e.g. an
  `IconButton` with a logout icon in the title bar area) calling
  `onIntent(PatientListingIntent.SignOutClicked)`. No new test (Compose wiring).
- [x] **Step 6: Run `./kotlin test -p android`.**
- [x] **Step 7: Commit.**

---

## Phase 9 — Manual end-to-end verification

- [ ] Run the app on a real device/emulator. Fresh install (no saved token): confirm it lands on
  Sign In, not Patient Listing. Sign up, confirm it routes to Sign In with a "created" message.
  Sign in with valid credentials, confirm it lands on Patient Listing. Kill and relaunch the app:
  confirm it skips Sign In and lands directly on Patient Listing (the saved-token path). Sign in
  with the wrong password, confirm an inline error and no navigation. Tap sign-out, confirm it
  returns to Sign In and a relaunch no longer skips it.
- [ ] Confirm `./kotlin test -p android` and `scripts/ktlint.sh` both pass on the full branch
  before opening the PR.
- [ ] This task produces no commit of its own.
