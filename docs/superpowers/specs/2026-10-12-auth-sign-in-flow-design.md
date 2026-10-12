# Auth: Sign In / Sign Up Flow — Design

**Goal:** Add the sign-in/sign-up screens, a startup auth check, and sign-out, so the app
actually authenticates against the real API instead of sending every request with no
`Authorization` header (its current state — `httpClientModule` wires `provideHttpClient(get())`
with no `AuthTokenProvider`, so every Bearer-protected call would 401 against production today).

**Not part of the graded five screens.** The assignment brief (Patient Registration, Vitals,
General Assessment, Overweight Assessment, Patient Listing) never specifies a sign-in screen —
this is infrastructure the app needs to call the API at all, built on top of already-existing
plumbing (`AuthApi`, `CachedAuthTokenProvider`, `TokenStorage`) that was deliberately left unwired
until a real sign-in flow existed to call it.

## Why now, and the refresh-token gap

The API (Laravel + Sanctum bearer tokens, confirmed via `docs/endpoints.md`) has no refresh-token
endpoint — `user/signin` returns one `access_token` and the only documented recovery from an
expired/invalid token is `401 Unauthorized` → sign in again. This shapes the whole design: there
is nothing to silently refresh, so every token-expiry case ends at the Sign In screen, not a
background renewal.

## Scope decisions

1. **Both sign-up and sign-in get screens**, not sign-in only — confirmed with the user rather
   than assumed, even though the assignment brief doesn't call for either.
2. **Startup auth check is a dedicated `Route.Splash`**, not logic embedded in Sign In's own
   ViewModel. `TokenStorage`/`CachedAuthTokenProvider.hydrate()` is suspend, so there's a real
   async gap between "app launched" and "do we have a token" — a splash route avoids ever
   rendering Sign In's UI for an already-authenticated user, and avoids a flash of the wrong
   screen.
3. **Authenticated home is `Route.PatientListing()`, not `Route.PatientRegistration`** — resolves
   the original screens-and-viewmodels plan's own flagged open question now that sign-in makes
   "an already-in-use clinic app reopening to today's patients" the realistic case. Patient
   Registration is still reachable via the existing FAB on Patient Listing.
4. **Sign-out is a manual action on Patient Listing only.** No global 401 interceptor that
   force-navigates to Sign In from anywhere in the app. The per-form `NeedsAttention(Unauthorized)`
   handling that already exists (purges the local row on a 401 during a form push) is unchanged
   and is a separate, already-shipped concern from this flow. A global interceptor would need
   `Navigator` reachable from a non-Compose, non-ViewModel context (`HttpClientProvider`) — real
   plumbing cost for a scenario the assignment's graded flows never exercise.
5. **No refresh-token handling** — there's nothing on the backend to refresh against (see above).

## Architecture

### New domain-ish result types (`data`, following `RegisterPatientResult`'s precedent of a
domain-named result per repository method rather than a generic shared `Conflict`/`Outcome`):

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
```

### `AuthRepository` / `DefaultAuthRepository` (`data`)

```kotlin
interface AuthRepository {
    suspend fun signIn(email: String, password: String): SignInResult
    suspend fun signUp(email: String, firstname: String, lastname: String, password: String): SignUpResult
}
```

`DefaultAuthRepository(authApi: AuthApi, cachedAuthTokenProvider: CachedAuthTokenProvider)`:
- `signIn` posts via `AuthApi.signin`, and on success calls
  `cachedAuthTokenProvider.setToken(response.data.accessToken)` itself — callers never touch
  token storage directly, matching `CachedAuthTokenProvider`'s own stated contract ("setToken/
  clearToken/hydrate are the only places touching TokenStorage").
- `HttpClientProvider`'s existing `expectSuccess = true` means a non-2xx throws
  `ClientRequestException`/`ServerResponseException` rather than silently deserializing a
  `success = false` body (same mechanism `classifyPush` already relies on). Catches map
  401 → `InvalidCredentials` (sign-in) / 422 → `EmailAlreadyRegistered` (sign-up, duplicate
  email) / anything else (5xx, timeout, `IOException`) → `Error(cause)`. `CancellationException`
  is explicitly rethrown, matching the existing convention in `offlineFirstWrite`.

### Routing (`navigation`)

`Route` gains three entries: `Splash`, `SignIn`, `SignUp`. `Navigator`'s start destination
changes from `Route.PatientRegistration` to `Route.Splash`.

```kotlin
class SplashViewModel(
    private val cachedAuthTokenProvider: CachedAuthTokenProvider,
    private val navigator: Navigator,
) : ViewModel() {
    init {
        viewModelScope.launch {
            cachedAuthTokenProvider.hydrate()
            if (cachedAuthTokenProvider.currentToken() != null) {
                navigator.replaceAllWith(Route.PatientListing())
            } else {
                navigator.replaceAllWith(Route.SignIn)
            }
        }
    }
}
```

`SplashScreen` itself is just a centered `CircularProgressIndicator` — no form, nothing to
validate, visible only for the duration of one suspend call.

### `SignInViewModel` / `SignInScreen` (`feature.signin`)

- Fields: `email: FieldState`, `password: FieldState` (both `Validators.required()` — no email
  format validator invented beyond what the spec needs, consistent with `patientIdFormat`'s own
  precedent of not inventing a format constraint the brief doesn't ask for).
- `submissionOutcome: StateFlow<SubmissionOutcome?>` reused as-is from `feature.common` —
  `SignInResult` maps onto it the same way the four existing forms' repository results do:
  `Authenticated` → `Submitted(synced = true)` (there's no local/remote split here, so `synced`
  is always true), `InvalidCredentials` → `Duplicate("Invalid email or password.")` (reusing the
  `Duplicate` case's human-readable-message shape is a mild abuse of its name, but it's exactly
  the "blocking, user-correctable, re-enter and retry" shape `Duplicate` already models — not
  worth a fifth `SubmissionOutcome` case for one screen; flagged here rather than decided
  silently, open to reviewer veto on this specific reuse at plan time)*, `Error` → `Failed(cause)`.
- `onSubmit()`: touches both fields, returns early if invalid, else calls
  `authRepository.signIn(...)`, maps result, navigates to `Route.PatientListing()` (via
  `navigator.replaceAllWith`, not `goTo` — signing in should not leave Sign In on the back stack)
  on `Authenticated`.

*(Reviewer/plan-writing note: if this mapping feels too cute, the alternative is a dedicated
`SignInSubmissionOutcome` with its own two cases — more type-safety, one more small file. Leaving
the call to whoever writes the actual plan task for this ViewModel.)*

### `SignUpViewModel` / `SignUpScreen` (`feature.signup`)

- Fields: `email`, `firstname`, `lastname`, `password` — all `FieldState`, matching
  `SignupRequestDto` field-for-field (no `confirmPassword` — not part of the DTO, not invented).
- On `AccountCreated`: `navigator.replaceAllWith(Route.SignIn)` with a one-shot "account created,
  sign in" message (reusing the existing `SubmissionOutcomeCard` pattern on the Sign In screen,
  or a simple `Snackbar` — left to the implementation task). On `EmailAlreadyRegistered`/`Error`:
  inline error, same shape as every other form.
- A "Already have an account? Sign in" text link navigates to `Route.SignIn` via `goTo` (not
  `replaceAllWith` — backing out of an abandoned sign-up should return to Sign In cleanly via a
  normal back-stack pop too).

### `PatientListingViewModel` changes

Gains a `CachedAuthTokenProvider` constructor dependency and a `SignOutClicked` intent:
`cachedAuthTokenProvider.clearToken()` then `navigator.replaceAllWith(Route.SignIn)`. A sign-out
icon goes in the screen's existing `AppScreenColumn` title bar area.

## DI wiring

Extends the already-established "eager-resolve-then-rebind-context-free" pattern from
`PatientRegistrationApp.onCreate()` (Design Decision #4 of the screens-and-viewmodels plan,
post-recursion-fix):

- New `tokenStorageModule`: Android — `single { (context: Context) -> EncryptedTokenStorage(...) }`
  (real `AesGcmEncryptor` via `tokenEncryptorQualifier`, same key-alias pattern already wired in
  `cryptoModule`); iOS — parameterless `single { KeychainTokenStorage() }`.
- `authModule`'s `CachedAuthTokenProvider` becomes a sixth entry resolved eagerly in
  `PatientRegistrationApp.onCreate()` alongside the five existing repositories, then rebound
  context-free the same way.
- `httpClientModule` changes `single { provideHttpClient(get()) }` to
  `single { provideHttpClient(get(), get()) }` — this is the line that actually turns on real
  `Authorization` headers for every API call, which is the whole point of this feature existing.
- `viewModelModule` gains `SplashViewModel`, `SignInViewModel`, `SignUpViewModel` entries, and
  `PatientListingViewModel`'s existing entry gains the new `CachedAuthTokenProvider` dependency.

## Testing

- `DefaultAuthRepositoryTest` against a `FakeAuthApi` (no live network, matching the existing
  `DefaultPatientRepositoryTest` convention) — covers the 401/422/success/IOException mapping for
  both `signIn` and `signUp`.
- `SplashViewModelTest`: token present → `replaceAllWith(PatientListing)`; token absent →
  `replaceAllWith(SignIn)`. Uses a real `CachedAuthTokenProvider` backed by a `FakeTokenStorage`
  (no new interface needed — `CachedAuthTokenProvider` is already a thin, directly testable
  class).
- `SignInViewModelTest` / `SignUpViewModelTest`: valid-submit-navigates, missing-field-blocks-and-
  force-touches-every-field, each failure case (`InvalidCredentials`/`EmailAlreadyRegistered`/
  `Error`) surfaces the right `SubmissionOutcome` and does **not** navigate — same Review-Focus
  shape the plan already applies to the four existing forms.
- `PatientListingViewModelTest` gains a `signOutClickedClearsTokenAndNavigatesToSignIn` case.

## What's deliberately out of scope

- Global 401 → forced sign-out interceptor (scope decision 4 above).
- Refresh-token handling — no such endpoint exists.
- Password-reset / forgot-password flow — not in the Postman collection at all, not raised by
  the user, not invented here.
- `confirmPassword` on Sign Up — not part of `SignupRequestDto`.
