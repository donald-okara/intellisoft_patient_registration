# API Reference — Patient Management App Backend

Deep dive into the backend API this app integrates with, reverse-engineered from the
[Postman collection](https://documenter.getpostman.com/view/18832855/2sB3Wnx2PF) linked in
[`Mobile App Development Practical - v2.pdf`](./Mobile%20App%20Development%20Practical%20-%20v2.pdf).
Every request below was pulled directly from the collection's example requests/responses, not
retyped from the assignment prose — see [Discrepancies vs. the assignment brief](#discrepancies-vs-the-assignment-brief)
for where the two disagree.

## Base URLs

| Environment | Base URL |
|---|---|
| Production | `https://patientvisitapis.intellisoftkenya.com/api/` |
| Local dev (backend devs only, not reachable from this app) | `http://localhost:8181/api` |

Stack behind the API: Laravel + Sanctum (bearer-token auth) + MySQL, containerized with Docker.
All responses are JSON and follow the same envelope:

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": { }
}
```

`data` is an object for single-record responses and an array for list responses — except
`patients/show/{id}`, which wraps its single record in a one-element array (see below).

## Authentication

1. `POST user/signup` once to create an account, or `POST user/signin` if one already exists.
2. Both return `data.access_token` (signin) — signup does not return a token, so sign in
   immediately after signing up.
3. Send the token on every other endpoint as:

   ```
   Authorization: Bearer <access_token>
   ```

   The collection's public share doesn't bake a token into its example `curl` commands (no
   environment is attached), so those commands appear to work without the header — in practice
   `patients/*`, `vital/add`, and `visits/*` are marked **Bearer Token** auth in the collection
   and will 401 without it. The examples below include the header.
4. Typical auth error responses: `401 Unauthorized` (invalid credentials / missing or expired
   token), `422 Unprocessable Entity` (validation errors, e.g. missing `email`/`password`),
   `400 Bad Request` (malformed body), `500 Internal Server Error`.
5. **No refresh-token endpoint exists.** The Postman collection has no `refresh`/`renew` request
   anywhere, and Sanctum's own default personal-access-token flow (what's in use here, per the
   `access_token` field name and the single-step `user/signin`) doesn't issue a refresh token
   alongside the access token the way an OAuth2 flow would. The only documented recovery from an
   expired or invalid token is `401 Unauthorized` → sign in again; there is nothing to refresh.

## Endpoint summary

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `user/signup` | none | Create a user account |
| POST | `user/signin` | none | Log in, obtain `access_token` |
| POST | `patients/register` | Bearer | Register a new patient |
| GET | `patients/view` | Bearer | List all registered patients |
| GET | `patients/show/{id}` | Bearer | Get one patient by id |
| POST | `vital/add` | Bearer | Record a vitals submission |
| POST | `visits/add` | Bearer | Record an assessment visit (General or Overweight form) |
| POST | `visits/view` | Bearer | List patients + latest BMI status, filterable by visit date |

---

## `POST user/signup`

Creates a user account (app staff login, not a patient).

**Request body**

```json
{
  "email": "imeja11@gmail.com",
  "firstname": "Japheth",
  "lastname": "Kiprotich",
  "password": "123456"
}
```

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/user/signup' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "email": "jkiprotich1@gmail.com",
    "firstname": "Japheth",
    "lastname": "Kiprotich",
    "password": "123456"
  }'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": {
    "proceed": 0,
    "message": "Account creation Successfull"
  }
}
```

No token is returned here — follow up with `user/signin`.

---

## `POST user/signin`

Authenticates and returns a bearer token.

**Request body**

```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/user/signin' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "email": "jkiprotich@gmail.com",
    "password": "123456"
  }'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": {
    "id": 1,
    "name": "Japheth Kiprotich",
    "email": "jkiprotich@gmail.com",
    "updated_at": "2025-10-30T15:43:00.000000Z",
    "created_at": "2025-10-30T15:43:00.000000Z",
    "access_token": "8|jNxfsFEFqIdrfhrtWPj9Zyx2m6ABvrsz6MY4Cy22"
  }
}
```

Persist `data.access_token` and attach it as `Authorization: Bearer <token>` to every
subsequent call.

---

## `POST patients/register`

Maps to the **Patient registration page**.

**Request body**

```json
{
  "firstname": "John",
  "lastname": "Mdoe",
  "unique": "12345611qq",
  "dob": "2002-01-01",
  "gender": "Male",
  "reg_date": "2025-10-31"
}
```

`unique` is the mockup's **Patient ID** field — the server-side unique identifier. The app
spec requires a patient be registered only once; enforce uniqueness locally (Room) before
calling this endpoint, since the sample response gives no explicit "duplicate" error shape to
rely on.

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/patients/register' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <access_token>' \
  --data '{
    "firstname": "John",
    "lastname": "Mdoe",
    "unique": "12345611qq",
    "dob": "2002-01-01",
    "gender": "Male",
    "reg_date": "2025-10-31"
  }'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": {
    "proceed": 0,
    "message": "Patient Added successfully"
  }
}
```

---

## `GET patients/view`

Lists every registered patient. Backs a "load all patients" use case (e.g. a picker, or a
baseline for the Patient Listing page before per-visit BMI status is overlaid).

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/patients/view' \
  --header 'Accept: application/json' \
  --header 'Authorization: Bearer <access_token>'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": [
    {
      "id": 3,
      "unique": "12345611qq",
      "firstname": "John",
      "lastname": "Mdoe",
      "dob": "2002-01-01",
      "gender": "Male",
      "reg_date": "2025-10-31",
      "created_at": "2025-10-30 16:35:41",
      "updated_at": "2025-10-30 16:35:41"
    }
  ]
}
```

---

## `GET patients/show/{id}`

Fetches one patient by numeric id (not the `unique` patient ID field).

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/patients/show/1' \
  --header 'Accept: application/json' \
  --header 'Authorization: Bearer <access_token>'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": [
    {
      "id": 1,
      "unique": "123456",
      "firstname": "Jane",
      "lastname": "Mdoe",
      "dob": "2002-01-01",
      "gender": "Male",
      "reg_date": "2025-10-31",
      "created_at": "2025-10-30T16:07:37.000000Z",
      "updated_at": "2025-10-30T16:07:37.000000Z"
    }
  ]
}
```

Note `data` is a **one-element array** here, not a bare object like every other single-record
response in this API — deserialize accordingly.

---

## `POST vital/add`

Maps to the **Vitals form**. Note the path is singular `vital/add`, not `vitals/add` as the
assignment brief's endpoint overview states.

**Request body**

```json
{
  "visit_date": "2025-10-31",
  "height": "156",
  "weight": "80",
  "bmi": "24.5",
  "patient_id": "1"
}
```

`height` is centimeters, `weight` is kilograms, `bmi` is computed client-side as
`weight(kg) / height(m)^2` before submitting (the server does not appear to recompute it).

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/vital/add' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <access_token>' \
  --data '{
    "visit_date": "2025-10-31",
    "height": "156",
    "weight": "80",
    "bmi": "24.5",
    "patient_id": "1"
  }'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": {
    "id": 5,
    "patient_id": "1",
    "slug": 1,
    "message": "Vital Added Successfully"
  }
}
```

`data.id` is the new vitals record id — pass it as `vital_id` to `visits/add` next. Use
`data.id`/this vitals record's BMI to decide whether to route to the General or Overweight
assessment form.

---

## `POST visits/add`

Maps to **both** assessment forms (General *and* Overweight). The collection exposes a single
endpoint with a combined schema rather than two separate ones:

**Request body**

```json
{
  "general_health": "Good",
  "on_diet": "Yes",
  "on_drugs": "Yes",
  "comments": "Test Comment",
  "visit_date": "2025-10-30",
  "patient_id": "1",
  "vital_id": "1"
}
```

| Field | Belongs to | Notes |
|---|---|---|
| `general_health` | both forms | `"Good"` \| `"Poor"` |
| `on_diet` | General Assessment only | "Have you ever been on a diet to lose weight?" — `"Yes"` \| `"No"` |
| `on_drugs` | Overweight Assessment only | "Are you currently using any drugs?" — `"Yes"` \| `"No"` |
| `comments` | both forms | free text |
| `visit_date`, `patient_id`, `vital_id` | both forms | links the visit back to the triggering vitals record |

The example request sends `on_diet` and `on_drugs` together, even though the UI only shows one
of the two per form (General vs. Overweight, chosen by BMI). The safest client behavior is to
send whichever field matches the active form and omit (or leave empty-string) the other — this
should be confirmed/defended live, per the assignment's own framing ("every part of the
submission must be something you can explain and defend").

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/visits/add' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <access_token>' \
  --data '{
    "general_health": "Good",
    "on_diet": "Yes",
    "on_drugs": "Yes",
    "comments": "Test Comment",
    "visit_date": "2025-10-30",
    "patient_id": "1",
    "vital_id": "1"
  }'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": {
    "slug": 0,
    "message": "Visit Added Successfully"
  }
}
```

---

## `POST visits/view`

Despite the collection's own description ("retrieves a list of visits for a given patient"),
the example request/response pair matches the **Patient Listing page**, not a single patient's
visit history: it takes only a date filter and returns one row per patient with their
already-computed BMI status.

**Request body**

```json
{
  "visit_date": "2025-10-30"
}
```

**curl**

```bash
curl --location 'https://patientvisitapis.intellisoftkenya.com/api/visits/view' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <access_token>' \
  --data '{
    "visit_date": "2025-10-30"
  }'
```

**200 OK**

```json
{
  "message": "success",
  "success": true,
  "code": 200,
  "data": [
    {
      "name": "Jane Mdoe",
      "age": 23,
      "bmi": "24.5",
      "status": "Normal"
    }
  ]
}
```

`status` is the server's own BMI classification (`Underweight` / `Normal` / `Overweight`) and
matches the thresholds in the brief, so the app's listing page doesn't strictly need to
recompute it client-side if it's sourcing data from the backend — though the app also needs a
Room-only offline path, so implement the classification locally too (see `docs/endpoints.md`
callers in the app, i.e. the BMI utility, should be unit tested either way per the assignment's
test requirements).

---

## Discrepancies vs. the assignment brief

The PDF's "Endpoints Overview" section (page 8) doesn't exactly match the live Postman
collection. Trust the collection (and this document) over the PDF prose:

| PDF says | Collection actually has |
|---|---|
| `POST user/login` | `POST user/signin` |
| `GET patients/list` | `GET patients/view` |
| `POST vitals/add` | `POST vital/add` (singular) |
| `GET visits/add` | `POST visits/add` |
| `POST visits/view` (requires visit date) | matches — `POST visits/view` |

## Open questions to raise in the live session

- Does `visits/add` truly accept `on_diet` and `on_drugs` in the same call for both form
  types, or should the client only send the field relevant to the active form?
- `patients/register`'s and `vital/add`'s `data.proceed` field — its meaning (success flag?
  dedupe signal?) isn't documented; confirm before relying on it for duplicate-patient
  detection.
- Whether `visits/view` is in fact the Patient Listing data source (given its response shape)
  despite being documented as a per-patient visit history.
- Confirm there's genuinely no refresh-token endpoint (not just an undocumented one in the
  collection) — it drives the sign-in flow's design: a 401 on any Bearer-protected call has to
  send the user back to a sign-in screen, not attempt a silent token refresh.
