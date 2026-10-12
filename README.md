# Intellisoft Patient Registration

An Android app (Kotlin Multiplatform, Compose Multiplatform UI) for registering patients,
recording vital signs with auto-calculated BMI, collecting one of two BMI-driven assessment
forms, and browsing a date-filterable patient listing. Built for Intellisoft's
["Mobile App development practical"](docs/Mobile%20App%20Development%20Practical%20-%20v2.pdf)
take-home assignment.

## What it does

Five screens, chained together by one BMI-driven branch:

1. **Patient registration** — Patient ID, registration date, first/last name, DOB, gender.
   A patient can be registered exactly once. Saving advances to Vitals.
2. **Vitals** — visit date, height (cm), weight (kg), BMI auto-computed as
   `weight(kg) / height(m)²`. A patient can submit vitals multiple times, but at most once per
   date. BMI < 25 routes to General Assessment; BMI ≥ 25 routes to Overweight Assessment.
3. **General Assessment** (BMI < 25) — general health (Good/Poor), ever dieted to lose weight
   (Yes/No), comments.
4. **Overweight Assessment** (BMI ≥ 25) — general health (Good/Poor), currently using any
   drugs (Yes/No), comments.
5. **Patient listing** — name, age, latest BMI status (Underweight `<18.5` / Normal
   `18.5–<25` / Overweight `≥25`), filterable by visit date.

Every form save writes to a local Room/SQLite database first, then syncs to the backend
described in [`docs/endpoints.md`](docs/endpoints.md).

## Status

This repository currently holds the generated Kotlin Multiplatform project skeleton only —
none of the five screens, the local database, or the API client have been implemented yet.
Treat the `shared` module's `App.kt`/`Greeting.kt`/`Platform.kt` as scaffolding to be replaced.

## Project structure

- [`/androidApp`](./androidApp) — Android application entry point.
- [`/iosApp`](./iosApp) — iOS application entry point.
- [`/shared`](./shared) — code shared across platforms (Compose UI, business logic, data
  layer). [`src`](./shared/src) is common code; sibling `src@<platform>` folders
  (e.g. `src@android`) hold platform-specific `actual` implementations.
- [`/docs`](./docs) — assignment brief and API reference:
  - [`Mobile App Development Practical - v2.pdf`](docs/Mobile%20App%20Development%20Practical%20-%20v2.pdf) — the original brief, screen mockups, and grading criteria.
  - [`endpoints.md`](docs/endpoints.md) — full endpoint-by-endpoint reference (curl, payloads,
    responses) reverse-engineered from the brief's linked Postman collection.

This is a [JetBrains Amper](https://kotlin-toolchain.org/dev/) project — modules are declared
via `module.yaml`/`project.yaml` rather than Gradle build scripts directly.

## Backend API

- Production base URL: `https://patientvisitapis.intellisoftkenya.com/api/`
- Auth: `POST user/signup` / `POST user/signin` → bearer token, sent as
  `Authorization: Bearer <token>` on every other call.
- Full endpoint documentation, request/response shapes, and curl examples:
  [`docs/endpoints.md`](docs/endpoints.md).

## Building & running

The `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) scripts in the project root are
self-bootstrapping wrappers for the Kotlin Toolchain: they download the pinned toolchain
version on first use, so no separate installation is required.

```bash
./kotlin build          # build the whole project
./kotlin run -m androidApp   # run the Android app
./kotlin run -m iosApp       # run the iOS app
```

You can also use the run configurations in your IDE's toolbar.

### Testing

```bash
./kotlin test              # run all tests
./kotlin test -m <module>  # run tests for a single module
```

Required test coverage per the assignment: BMI calculation and BMI-status classification
(including boundary values 18.5 and 25), form validation (mandatory fields, duplicate Patient
ID, one submission per date), one Room DAO test, and one repository test against a faked or
mocked API.

## Deliverables checklist

Per the assignment brief — not yet satisfied by this skeleton:

- [ ] Git history spanning at least two working sessions (incremental commits, not one bulk
      commit)
- [x] Screen recording (<5 min) of the full flow: registration → vitals → assessment →
      listing filter — [`assets/part_1.mp4`](assets/part_1.mp4) +
      [`assets/part_2.mp4`](assets/part_2.mp4) (split in two; see
      [`assets/README.md`](assets/README.md) for why)
- [ ] Unit tests: BMI calc/classification, form validation, Room DAO, repository (faked/mocked
      API)
- [ ] `DESIGN.md` (max one page): 2–3 key design decisions and why; what was deliberately
      simplified or left out

## Learn more

[Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Kotlin Toolchain](https://kotlin-toolchain.org/dev/),
[Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/).
