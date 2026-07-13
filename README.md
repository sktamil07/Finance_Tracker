# Finance Tracker — Backend

Multi-user personal finance API. Java 21, Spring Boot 3.3, MySQL 8 + Flyway, stateless JWT.

Every user-owned row carries a `user_id` and every query is filtered by the authenticated user.
No derived figure (savings, savings rate, allocation, budget status) is ever stored — it is all
computed at request time from the raw amounts.

---

## Quick start

Prerequisites: **JDK 21**, **Docker** (for the local MySQL and for the integration tests). Maven is
not required — the repo ships the Maven Wrapper.

```bash
cp .env.example .env          # then edit it
set -a && . ./.env && set +a  # export the vars into your shell

docker compose up -d          # MySQL 8 on ${DB_PORT:-3306}
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Swagger UI → <http://localhost:8080/swagger-ui.html>
- OpenAPI spec → <http://localhost:8080/v3/api-docs>

On the `dev` profile a demo user is seeded (see [Sample data](#sample-data)):

```
demo@financetracker.local / Demo@12345
```

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@financetracker.local","password":"Demo@12345"}' | jq -r .accessToken)

curl -s "localhost:8080/api/v1/dashboard?month=2026-04" -H "Authorization: Bearer $TOKEN" | jq
```

### Environment variables

All secrets come from the environment. Nothing has a default except host/port/db-name, and
`JWT_SECRET` has **no** default on purpose — the app refuses to boot without one.

| Variable | Required | Default | Notes |
| --- | --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | no | `localhost` / `3306` / `finance_tracker` | |
| `DB_USERNAME` / `DB_PASSWORD` | **yes** | — | |
| `JWT_SECRET` | **yes** | — | ≥ 32 bytes (HS256). `openssl rand -base64 48` |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173,http://localhost:3000` | comma-separated frontend origins |
| `SERVER_PORT` | no | `8080` | |

### Profiles

| Profile | Flyway | `ddl-auto` | Seeder |
| --- | --- | --- | --- |
| `dev` | on | `validate` | on |
| `prod` | on | `validate` | off |
| `local` | **off** | **`update`** | off |

`local` is the throwaway profile: it lets Hibernate mutate the schema so you can experiment
without writing a migration. Never point it at a database you care about. `dev` and `prod` both
run `validate`, so a drift between the entities and `V1__init_schema.sql` fails the boot.

---

## Testing

```bash
./mvnw test      # 37 unit tests, no Docker needed
./mvnw verify    # + 64 Testcontainers integration tests (needs Docker)
```

- `*Test` → unit tests (surefire). Dashboard/report arithmetic and budget thresholds, with mocks.
- `*IT` → integration tests (failsafe). Boot the real app against a throwaway MySQL 8 container.

Because the integration tests run Flyway and then Hibernate `ddl-auto: validate`, **every one of
them doubles as a check that the migration and the entity mappings agree.**

Each feature has an integration test covering the happy path and an ownership violation (403).
`UserDataIsolationIT` is the headline one: it proves user A cannot read, count, or mutate user B's
expenses — through the grouped month view, the paginated transaction list, `PUT`/`DELETE` by id,
and the dashboard/report aggregates.

> **Docker Engine 29 note.** Engine 29 dropped Docker API versions below 1.44, which is newer than
> the version docker-java negotiates by default; without a fix every Testcontainers strategy fails
> with an opaque HTTP 400. `AbstractIntegrationTest` sets `api.version=1.44` when you have not set
> it yourself. On an older engine, override with `-Dapi.version=1.41` or `DOCKER_API_VERSION`.

---

## Architecture

Base package `com.bharath.financetracker`, organised **package-by-feature**:

```
auth  user  category  income  investment  expense  budget  dashboard  report  settings
common/{error,exception,util}   security   config   mappers
```

Each feature is layered strictly:

```
Controller (REST + DTOs)  ->  Service (interface + impl, all business logic)  ->  Repository
```

- **Entities never leave the service layer.** Controllers speak only DTOs. The few service methods
  that return entities are for other *services* to consume (e.g. `CategoryService.requireOwned`)
  and are marked "service-layer only" in their javadoc.
- **Constructor injection only** (Lombok `@RequiredArgsConstructor`). No field injection.
- **No business logic in controllers or entities.** Controllers parse the `month` param, delegate,
  and return. The dashboard and report services own the arithmetic; `BudgetStatusCalculator` and
  `MoneyMath` are pure and separately unit-tested.
- MapStruct maps entity → DTO, with `unmappedTargetPolicy=ERROR`, so a DTO field that nobody maps
  is a compile error rather than a silent `null`.

### Data model

All money is `DECIMAL(15,2)`. Every `month` column is a `DATE` pinned to the **first of the month**
(normalised in the service, so a stray day can never be persisted). On the wire a month is the
string `"2026-04"` (`java.time.YearMonth`).

`BudgetGoal.month` is nullable: `NULL` means the limit **recurs every month**; a value means it
applies to that month only and overrides the recurring row.

---

## Business rules

For a month `M`, all computed per request:

```
salary       = Σ income rows for M
invested     = Σ investment rows for M
expenses     = Σ expense rows dated inside M
savings      = salary - invested - expenses          (may be negative)
savingsRate  = savings / salary * 100                (exactly 0 when salary is 0)
allocation   = each of invested/expenses/savings as a % of salary
```

Percentages are 2dp, `HALF_UP`. **Nothing ever divides by zero** — a zero denominator yields `0.00`
(`MoneyMath.percentage`). The allocation is derived from the rupee amounts; there is no hardcoded
split anywhere.

**Budget status** — against the effective limit (month-specific row if present, else the recurring row):

| Status | Condition |
| --- | --- |
| `NONE` | `spent < 80%` of limit |
| `WARNING` | `spent >= 80%` and `spent <= 100%` |
| `EXCEEDED` | `spent > 100%` |

Spending *exactly* the limit is a `WARNING`, not an overrun. The comparison is exact
(`spent * 100 >= limit * 80`), not a rounded percentage — rounding first would report ₹10,000.01
against a ₹10,000 limit as "100.00%" and therefore only a warning.

**Bond/maturity alerts** — on the dashboard, investments maturing in the **requested month or the
next one**. Distinct from `GET /investments/upcoming`, which is relative to *today* and spans
`[today, end of next month]`, covering both maturities and SIP instalments (a SIP set for the 31st
is clamped to the last day of a shorter month).

---

## API

Everything under `/api/v1`. All endpoints except `/auth/**` require `Authorization: Bearer <accessToken>`.

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/auth/register` | 201; seeds the default categories; returns a token pair |
| `POST` | `/auth/login` | `{accessToken, refreshToken, tokenType, expiresIn}` |
| `POST` | `/auth/refresh` | rotates the pair; an access token is rejected here |
| `GET` | `/users/me` | |
| `GET` | `/dashboard?month=YYYY-MM` | metrics + allocation + breakdowns + bond alerts |
| `GET` `POST` | `/income` | list filterable by `?month=` |
| `PUT` `DELETE` | `/income/{id}` | |
| `GET` `POST` | `/investments` | list filterable by `?month=` |
| `GET` | `/investments/upcoming` | SIP dates + maturities, current/next month |
| `PUT` `DELETE` | `/investments/{id}` | |
| `GET` | `/expenses?month=YYYY-MM` | grouped by category, with each category's % of the month |
| `GET` | `/expenses/transactions` | flat list, **paginated** (`page`, `size`, `sort`) |
| `POST` | `/expenses` | |
| `PUT` `DELETE` | `/expenses/{id}` | |
| `GET` `POST` | `/categories` | `?type=EXPENSE\|INVESTMENT` |
| `PUT` `DELETE` | `/categories/{id}` | |
| `GET` | `/budgets?month=YYYY-MM` | status per category: spent, limit, `NONE`/`WARNING`/`EXCEEDED` |
| `GET` | `/budgets/goals` | the raw goal rows, to find an id for `PUT`/`DELETE` |
| `POST` | `/budgets` | |
| `PUT` `DELETE` | `/budgets/{id}` | |
| `GET` | `/reports/summary?month=YYYY-MM&months=6` | `months` is bounded to 1..24 |
| `GET` `PUT` | `/settings` | theme, currency, display name |

Access tokens live ~15 min, refresh tokens ~7 days. The token type is stamped into a `typ` claim, so
a refresh token cannot be replayed as a bearer token (and vice versa).

### Errors

Every failure returns the same body:

```json
{
  "timestamp": "2026-04-12T09:31:04.221Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for 2 field(s)",
  "path": "/api/v1/expenses",
  "fieldErrors": [{ "field": "amount", "message": "must be greater than 0" }]
}
```

`fieldErrors` is present only for validation failures. Login failures are deliberately vague
("Invalid email or password") so the endpoint never reveals whether an email is registered.

---

## Documented decisions

The brief left two choices open. Both are enforced in one place and covered by tests.

**1. Deleting a category that is in use is blocked (409), not reassigned.**
`DELETE /categories/{id}` returns `409 Conflict` while any expense, investment, or budget goal
still references it. Silently moving a user's history into another bucket is worse than making them
move it explicitly. Renaming is always safe: rows reference the category by foreign key, so a rename
shows up everywhere at once. A category's **type is immutable** once created, which is why
`PUT /categories/{id}` accepts only `name` and `icon`.

**2. Ownership: 403 when the row exists but belongs to someone else, 404 when it does not exist.**
This is the split the acceptance criteria ask for (an observable 403 on ownership violations). It
does leak the existence of a foreign id; that trade-off is deliberate and lives in exactly one
place — `OwnershipGuard.requireOwner` — so flipping it to a uniform 404 is a one-line change.

List endpoints simply never return another user's rows, and aggregates (dashboard, report, budget
status) are computed only over the authenticated user's data.

---

## Sample data

`DevDataSeeder` runs on the `dev` profile only and is idempotent. It seeds **raw amounts only** —
never a percentage, a savings figure, or an allocation split, so the SOP's sample percentages (which
do not reconcile against its own rupee amounts) are not baked in. The dashboard derives them:

```
salary 191,000  invested 130,000  expenses 41,098
  -> savings 19,902, savingsRate 10.42%
  -> allocation 68.06% invested / 21.52% expenses / 10.42% savings   (= 100.00%)
```

Seeded for April 2026: the ₹1,91,000 salary and the four investments (a recurring Mutual Funds SIP,
Stocks, an RBI bond maturing 2026-05-18, and an FD maturing 2026-04-28 — the last two drive the bond
alerts). Nine expenses and three budget goals (two recurring, one April-only Travel override) are
also seeded so the expense breakdown, budget status, and report are not empty on a fresh database.

> The SOP's exact per-investment and per-expense amounts were not supplied to me, so those figures
> are representative. The salary (₹1,91,000) and the count of four investments match the brief. Swap
> the constants in `DevDataSeeder` if you have the real numbers — nothing else depends on them.

---

## Layout

```
src/main/java/com/bharath/financetracker/
├── FinanceTrackerApplication.java
├── auth/          register, login, refresh
├── user/          User entity, /users/me
├── settings/      theme, currency, display name
├── category/      CRUD + default-category seed + type/ownership guard
├── income/        income rows
├── investment/    investments, SIPs, maturities
├── expense/       expenses, grouping, pagination
├── budget/        goals + BudgetStatusCalculator
├── dashboard/     metrics, allocation, breakdowns, bond alerts
├── report/        trailing N-month summary
├── common/        ApiError, GlobalExceptionHandler, exceptions, MoneyMath, MonthUtils, OwnershipGuard
├── security/      JwtService, JwtAuthenticationFilter, SecurityConfig, AuthPrincipal
├── config/        CORS, OpenAPI, Clock, DevDataSeeder
└── mappers/       MapStruct entity -> DTO

src/main/resources/db/migration/V1__init_schema.sql
```

`Clock` is a bean so tests can pin "today" instead of depending on the wall clock.
