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

## Main API Endpoints

| Domain | Method | Path | Auth | Description |
|---|---|---|---|---|
| **Auth** | POST | `/api/auth/register` | Public | Register new user |
| **Auth** | POST | `/api/auth/login` | Public | Authenticate and obtain JWT |
| **Auth** | GET | `/api/auth/me` | Bearer | Current user profile |
| **Dashboard** | GET | `/api/stock/dashboard/stats` | Bearer | Real-time global SQL KPI aggregation |
| **Decision** | GET | `/api/stock/decisions` | Bearer | Replenishment recommendations & risk scores |
| **Decision** | GET | `/api/stock/decisions/summary` | Bearer | Aggregate at-risk counts & replenishment budget |
| **Clients** | GET | `/api/clients` | Bearer | Paginated client directory with search |
| **Clients** | POST/PUT/DELETE | `/api/clients/**` | ADMIN | Manage industrial clients |
| **Articles** | GET/POST/PUT/DELETE | `/api/stock/articles/**` | Bearer/ADMIN | Product catalogue management |
| **Movements** | POST | `/api/stock/movements` | ADMIN/USER | Record stock entry/exit/adjustment |
| **Import/Export**| GET/POST | `/api/stock/{articles\|categories\|suppliers}/{export\|import}` | ADMIN | CSV batch processing |

## Tests

Execute the full suite of 45 unit and integration tests:

```bash
./mvnw clean test
```

## Notes

- `spring.jpa.hibernate.ddl-auto=update` is used for schema synchronization.
- Test suite connects automatically to PostgreSQL on localhost:5433 via `src/test/resources/application.yml`.
- Demo users: `admin@novaerp.local` (ADMIN) and `sara.amrani@novaerp.local` (USER).
