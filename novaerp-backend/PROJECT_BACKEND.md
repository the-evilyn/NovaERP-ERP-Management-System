# NovaERP — Backend

Spring Boot 4 / Java 21 REST API for NovaERP, a small business ERP (stock/inventory focused). Consumed by the `novaerp-frontend` Next.js app. Sibling repo lives at `../novaerp-frontend` (see `PROJECT_FRONTEND.md` there for the client side).

## Stack

- **Spring Boot 4**, **Java 21**, Maven (`./mvnw`)
- Spring Web (MVC), Spring Data JPA, **PostgreSQL**
- Spring Security + **JWT** (`jjwt`), stateless sessions
- Spring Mail (SMTP → **MailHog** locally, for password reset emails)
- **springdoc-openapi** (Swagger UI)
- Lombok

## Package layout

```
com.novaerp.backend
  NovaerpBackendApplication.java

  auth/                  # register/login/me/forgot-password/reset-password
    AuthController.java
    PasswordResetService.java, PasswordResetToken(+Repository).java
    dto/  (AuthResponse, LoginRequest, RegisterRequest, ForgotPasswordRequest,
           ResetPasswordRequest, UserResponse)

  user/                  # User entity, Role enum (ADMIN, USER), UserRepository

  security/
    SecurityConfig.java          # filter chain, CORS, public endpoints, role rules
    JwtAuthenticationFilter.java # reads Bearer token, sets auth context
    JwtService.java              # issue/parse/validate JWT
    CustomUserDetailsService.java

  stock/                 # the real business domain — everything under /api/stock/**
    Article(+Repository).java, ArticleSupplierPrice(+Repository).java
    Category(+Repository).java, Unit(+Repository).java, Supplier(+Repository).java
    StockMovement(+Repository).java, StockMovementType.java (IN, OUT, ADJUSTMENT)
    ArticleController.java, CategoryController.java, SupplierController.java,
    UnitController.java, StockMovementController.java
    StockMovementService.java, StockImportExportService.java   # CSV import/export
    dto/  (Request/Response pair per entity, ImportResultResponse, ImportRowIssue)

  common/
    GlobalExceptionHandler.java  # -> ApiError shape: {timestamp, status, error, message, path}
    csv/  CsvUtils.java, CsvResponses.java

  config/
    DataSeeder.java        # demo data, only runs under "seed" profile, skips if users exist
    OpenApiConfig.java

  mail/MailService.java
```

## Domain model

- **Category** (name, description) → referenced by Article
- **Unit** (name, symbol) → referenced by Article
- **Supplier** (name, email, phone, address)
- **Article** — the core stock item: reference, designation, brand, barcode, categoryId, unitId, purchasePriceHt, unitCostTtc, salePriceHt, stockQuantity, minStockQuantity, serialTracked, description, notes
- **ArticleSupplierPrice** — a supplier's quoted price for an article (supports multiple suppliers per article, one marked `primary`)
- **StockMovement** — IN / OUT / ADJUSTMENT against an article, with quantity, reference, note, and the user who created it
- **User** — fullName, email, password (bcrypt), role (`ADMIN` | `USER`)

`Client`, `Product` (generic), `Invoice`, `StockAlert`, `AuditLog` appear in the frontend's `types/models.ts` as forward-looking types but **have no backend entity/table yet** — do not assume they exist here.

## API surface

Base path: `/api`. All list endpoints (`GET` with no `/{id}`) are paginated Spring `Page<T>` responses (`?page=0&size=20`).

### Auth (`/api/auth`) — public except `/me`

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/register` | public | creates user, returns JWT |
| POST | `/login` | public | returns JWT |
| GET | `/me` | Bearer | current user |
| POST | `/forgot-password` | public | sends reset email via MailHog |
| POST | `/reset-password` | public | consumes reset token |

### Stock (`/api/stock/**`) — read requires auth, write requires `ROLE_ADMIN`

- `GET/POST /categories`, `GET/PUT/DELETE /categories/{id}`, `GET /categories/export`, `POST /categories/import`
- `GET/POST /units`, `GET/PUT/DELETE /units/{id}`
- `GET/POST /suppliers`, `GET/PUT/DELETE /suppliers/{id}`, `GET /suppliers/export`, `POST /suppliers/import`
- `GET/POST /articles`, `GET/PUT/DELETE /articles/{id}`, `GET /articles/export`, `POST /articles/import`
  - `GET/POST /articles/{id}/supplier-prices`, `DELETE /articles/{id}/supplier-prices/{priceId}`, `PUT /articles/{id}/supplier-prices/{priceId}/primary`
- `GET /movements`, `GET /movements/article/{articleId}`, `POST /movements`

CSV import endpoints accept multipart file upload (max 20MB, see `application.yml`) and return `ImportResultResponse { created, skipped, failed, errors[], warnings[] }`.

### Authorization rule (see `SecurityConfig`)

- Public: `/api/auth/**`, Swagger UI, `/v3/api-docs/**`, `/actuator/health`, `/actuator/info`
- `POST`/`PUT`/`DELETE` on `/api/stock/**` → requires `ROLE_ADMIN`
- Everything else → any authenticated user (`ROLE_USER` or `ROLE_ADMIN`)

## Error shape

All errors from `GlobalExceptionHandler` follow:
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/..." }
```
This must match `ApiError` in the frontend's `lib/api-error.ts` / `types/models.ts`.

## Run locally

Full stack via Docker (Postgres + MailHog + backend):
```bash
docker-compose up --build
```

| Service | URL |
|---|---|
| Backend | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Postgres | localhost:5433 (db/user/pass: `novaerp`) |
| MailHog UI | http://localhost:8025 |
| MailHog SMTP | localhost:1025 |

Backend is on host port `8081` and Postgres on `5433` to avoid clashing with other local services.

App-only, against dockerized infra:
```bash
docker-compose up postgres mailhog
DB_PORT=5433 ./mvnw spring-boot:run
```

To seed demo data, activate the `seed` Spring profile (`SPRING_PROFILES_ACTIVE=seed`); `DataSeeder` skips if users already exist, so it's safe to leave on.

## Configuration (env vars, see `.env.example` / `application.yml`)

| Var | Default | Notes |
|---|---|---|
| `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` | localhost/5432/novaerp/novaerp/novaerp | Postgres |
| `SERVER_PORT` | 8080 (mapped to host `8081` via `HOST_PORT`) | |
| `MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD` | localhost/1025 | MailHog locally |
| `JWT_SECRET` | dev default in `application.yml` — **change before prod** | base64 |
| `JWT_EXPIRATION_MS` | 86400000 (24h) | must match frontend's `TOKEN_MAX_AGE_SECONDS` in `lib/axios.ts` |
| `RESET_PASSWORD_URL` | http://localhost:3000/reset-password | must point at frontend |
| `CORS_ALLOWED_ORIGINS` | http://localhost:3000 | must include frontend origin(s) |

## Conventions / gotchas

- `spring.jpa.hibernate.ddl-auto=update` — no migrations (Flyway/Liquibase) yet, schema is derived from entities. Be careful with destructive entity changes.
- DTOs are the contract with the frontend: any change to a `dto/*Request.java` / `*Response.java` field must be mirrored in `../novaerp-frontend/types/models.ts`.
- Role checks are coarse: any `ROLE_ADMIN` can write to *all* of `/api/stock/**` (no per-resource permission granularity).
- `open-in-view: false` — don't rely on lazy-loading outside a transaction/service method; map to DTOs inside the service/controller layer.
