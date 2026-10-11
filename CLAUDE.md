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

This repo is still the generated Amper/Compose Multiplatform template (`App.kt`, `Greeting.kt`,
`Platform.kt` are boilerplate "Hello World" scaffolding). None of the five screens, the Room
database, or the API client exist yet — treat this as a greenfield build, not a refactor.

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

1. Unit tests for BMI calculation and BMI-status classification, including boundary values.
2. Unit tests for form validation: mandatory fields, duplicate Patient ID, one submission per
   date.
3. One Room DAO test.
4. One repository test using a faked or mocked API (not a live network call).

## Deliverables this repo still owes

- **Git history across ≥2 working sessions** — commit incrementally as features land, not as
  one bulk commit at the end; this is graded. (Note: as of this writing the directory is not
  yet a git repository — don't `git init` or make the first commit without the user's
  go-ahead.)
- **`DESIGN.md`** (root, one page max): 2–3 key design/architecture/library/data-model
  decisions and why, plus what was deliberately simplified or left out. Keep it factual and
  short — detailed trade-off discussion happens live, not in the doc.
- A screen recording (<5 min) of the full flow — not something Claude can produce; flag it as
  outstanding rather than attempting it.
- State in `DESIGN.md` which parts of the code could move to the shared KMP module vs. stay
  platform-specific, even if the final submission ends up Android-only.
