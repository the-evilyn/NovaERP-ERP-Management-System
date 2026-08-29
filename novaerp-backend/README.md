# NovaERP Backend

Fresh Spring Boot 4 / Java 21 backend for `novaerp-frontend`. Includes a minimal JWT auth module, PostgreSQL, MailHog for local email testing, and Swagger UI.

## Stack

- Spring Boot 4, Java 21, Maven
- Spring Web, Spring Data JPA, PostgreSQL driver
- Spring Security + JWT (jjwt)
- Spring Mail (SMTP, points at MailHog locally)
- springdoc-openapi (Swagger UI)

## Run everything with Docker

```bash
docker-compose up --build
```

This starts:

| Service    | URL                                      |
|------------|-------------------------------------------|
| Backend    | http://localhost:8081                     |
| Swagger UI | http://localhost:8081/swagger-ui.html     |
| Postgres   | localhost:5433 (db/user/pass: `novaerp`)  |
| MailHog UI | http://localhost:8025                     |
| MailHog SMTP | localhost:1025                          |

The backend is exposed on host port `8081` and Postgres on `5433` (instead of the usual `8080`/`5432`) to avoid clashing with other local services. Copy `.env.example` to `.env` to override any defaults.

## Run the backend locally (without Docker)

Start just the infra:

```bash
docker-compose up postgres mailhog
```

Then run the app, pointing it at the mapped port:

```bash
DB_PORT=5433 ./mvnw spring-boot:run
```

## Auth endpoints

| Method | Path              | Auth       | Description               |
|--------|-------------------|------------|----------------------------|
| POST   | `/api/auth/register` | Public  | Create a user, returns JWT |
| POST   | `/api/auth/login`    | Public  | Login, returns JWT         |
| GET    | `/api/auth/me`        | Bearer  | Current authenticated user |

Example:

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test User","email":"test@novaerp.local","password":"password123"}'
```

Use the returned `token` as `Authorization: Bearer <token>` on subsequent requests, or click "Authorize" in Swagger UI.

## Notes

- `spring.jpa.hibernate.ddl-auto=update` is used for now (fresh app) — swap for migrations (Flyway/Liquibase) before this goes anywhere near production.
- JWT secret / expiration are configurable via `JWT_SECRET` / `JWT_EXPIRATION_MS` env vars — the checked-in default is for local dev only.
