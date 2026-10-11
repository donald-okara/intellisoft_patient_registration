# Design note

One page max. Filled in incrementally as each phase lands, not reconstructed at the end.

## Key design decisions

- **ktlint runs standalone, not through Amper.** Amper (this project's build tool instead of
  Gradle) has no built-in lint check. `scripts/ktlint.sh` pins and checksum-verifies the ktlint
  CLI itself, so local dev and CI run the exact same tool.
- **Package layout is by architectural layer**, not a flat `infra` catch-all:
  `datasource.remote`/`datasource.local` (Ktor/Room/token storage), `di` (Koin modules only — no
  logic), `work` (kmpworkmanager), `data`/`feature.<name>` reserved for repositories and
  per-screen code once they exist.
- **kmpworkmanager is Android-only; Ktor/kotlinx.serialization are common with per-platform
  engines; Room and DataStore target both Android and iOS.** Room's KSP processor is registered
  under both `settings@android` and `settings@ios` (Amper's KSP is per-platform). CI only
  builds/tests Android (the brief accepts an Android-only submission) and this machine has no
  Xcode, so everything iOS here is dependency-resolution-verified only, never compiled/run
  locally — true of the Darwin Ktor engine, Room's iOS KSP/driver, and `KeychainTokenStorage`.
- **Token storage uses a different secure-storage primitive per platform** behind one common
  `TokenStorage` interface: Android encrypts with an `AndroidKeyStore` AES/GCM key
  (`TokenEncryptor`) before writing to DataStore; iOS stores directly in Keychain (already
  encrypted at rest), bypassing DataStore for this value. Real iOS crypto via CommonCrypto
  cinterop was considered and rejected as unverifiable here and not the idiomatic iOS pattern
  when Keychain already solves this directly.
- **Crypto/test-only choices worth knowing**: `TokenEncryptor`'s tests inject a plain in-memory
  AES key rather than going through `AndroidKeyStore` (a real-device/emulator-only provider
  Robolectric can't shadow) — the injected seam (`internal constructor(secretKeyProvider: () ->
  SecretKey)`) tests the AES/GCM + IV logic, not Android's keystore daemon. `TokenEncryptor` uses
  `java.util.Base64`, not `android.util.Base64` (unmocked in plain JVM unit tests) — available
  since API 26, already this project's floor (kmpworkmanager's requirement).
- **No KSP-generated worker factories yet.** `kmpworkmanager`'s optional `kmpworker-ksp`
  processor would auto-generate factories; a hand-written `AppWorkerFactory` does the same job
  with one fewer processor to debug while several libraries were wired at once.
- **Koin for DI, not yet started** (no `startKoin {}` anywhere) — same "prove the wiring, don't
  fake production usage" approach as the rest of this phase, verified with `Module.verify()`
  instead of a real app-wide instance.

## What was simplified or left out

- The Room entity/DAO/database added while wiring up Room is infra-proof scaffolding, not the
  real data model — it lives in `shared/test/datasource/local` (common, so KSP runs for both
  platforms) and is clearly marked throwaway, superseded once real patient entities exist.
- `kmpworkmanager-http` was skipped — pins Ktor 3.1.x, conflicting with this project's own Ktor
  3.6.0 client. Real sync workers will call the project's own `HttpClient` instead.
- No `KmpWorkManager.initialize(...)` or `startKoin {}` call exists yet in a real `Application`
  class — nothing real to run in production yet; deferred to the phase that adds one.

## Shared KMP module vs. platform-specific

- Ktor client configuration (`shared/src/datasource/remote/HttpClientProvider.kt`), Room
  entities/DAOs, and the `TokenStorage` interface are fully common. Platform-specific: HTTP
  engines (OkHttp/Darwin), Room's database builder and SQLite driver
  (`sqlite-framework`/`sqlite-bundled`), and token storage's actual implementation
  (`EncryptedTokenStorage` vs. `KeychainTokenStorage`) — different enough per platform that a
  shared implementation wouldn't make sense even though the interface does.
- kmpworkmanager (`AppWorkerFactory`) is Android-only; extending it to iOS would mean iOS
  workers/factories too — not attempted.
