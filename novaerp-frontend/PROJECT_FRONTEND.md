# NovaERP — Frontend

Next.js 16 (App Router) frontend for NovaERP, a small business ERP (stock/inventory focused). Talks to the `novaerp-backend` Spring Boot API. Sibling repo lives at `../novaerp-backend` (see `PROJECT_BACKEND.md` there for the API side).

## Stack

- **Next.js 16** (App Router, `output: "standalone"`)
- **React 19**
- **TypeScript**, strict mode
- **Tailwind CSS 4**
- **shadcn/ui** (`components/ui/`) + **@base-ui/react** primitives
- **@tanstack/react-query** for server-state
- **axios** for HTTP
- **next-intl** for i18n (English + Arabic, incl. RTL)
- **recharts** for charts
- **@hugeicons/react** for icons

## Directory layout

```
app/[locale]/                 # all routes are locale-prefixed (/en/..., /ar/...)
  (dashboard)/                # authenticated app shell (route group)
    dashboard/page.tsx
    articles/page.tsx
    categories/page.tsx
    suppliers/page.tsx
    units/page.tsx
    stock-movements/page.tsx
    import-export/page.tsx
    layout.tsx               # sidebar + header shell for the above
  login/page.tsx
  register/page.tsx
  forgot-password/page.tsx
  reset-password/page.tsx
  layout.tsx                  # root locale layout (providers, fonts, dir=rtl/ltr)
  page.tsx                    # redirects to /dashboard or /login

components/
  ui/                          # shadcn primitives (button, dialog, table, sidebar, etc.)
  layout/                      # app-sidebar, nav-main, nav-user, site-header, language-switcher
  articles/ categories/ suppliers/ units/ stock-movements/ import-export/ dashboard/
  shared/                      # data-table, pagination-table (generic table plumbing)
  clients/ products/           # present but not wired to a real backend module yet

services/                      # one file per backend domain, thin axios wrappers
  auth.service.ts
  articles.service.ts
  categories.service.ts
  suppliers.service.ts
  units.service.ts
  stock-movements.service.ts
  import-export.service.ts
  services.mock.ts             # mock data for domains without a real API yet

providers/
  auth-provider.tsx            # cookie-based JWT session, exposes useAuth()
  query-provider.tsx           # react-query client

lib/
  axios.ts                     # shared `api` instance, attaches Bearer token, 401 -> redirect to /login
  api-error.ts                 # normalizes backend ApiError shape
  cookies.ts                   # get/set/delete cookie helpers
  csv.ts                       # CSV import/export helpers
  format.ts                    # number/date/currency formatting
  utils.ts                     # cn() etc.

i18n/
  routing.ts                   # locales: en, ar — default en
  navigation.ts                # locale-aware Link/useRouter wrappers
  request.ts                   # next-intl server config

messages/en.json, messages/ar.json   # translation strings (namespaced, e.g. "nav", "common")

types/models.ts                 # single source of truth for all API DTO types (must mirror backend DTOs)

proxy.ts                        # Next.js middleware: locale routing + auth gate
                                 # (blocks /dashboard/** without cookie, blocks auth pages when logged in)
```

## Auth flow

- JWT stored in a cookie named `novaerp_token` (see `lib/axios.ts` / `lib/cookies.ts`), 24h max-age (matches backend default `JWT_EXPIRATION_MS`).
- `providers/auth-provider.tsx` (`useAuth()`) hydrates the user via `GET /auth/me` on mount if a cookie exists.
- `proxy.ts` (Next middleware) redirects unauthenticated users away from `/dashboard/**` to `/login`, and authenticated users away from auth pages to `/dashboard`.
- `lib/axios.ts` attaches `Authorization: Bearer <token>` to every request and force-redirects to `/login` on a 401.

## i18n

- Locales: `en` (default), `ar` (RTL). Every route is under `app/[locale]/...`.
- Use `Link`/`useRouter` from `@/i18n/navigation` (not `next/link` / `next/navigation`) so locale prefixing stays correct.
- Add new UI strings to **both** `messages/en.json` and `messages/ar.json` under the right namespace, then `useTranslations("namespace")`.

## Data fetching pattern

Each backend domain has a `services/<domain>.service.ts` with plain async functions (`getX`, `createX`, `updateX`, `deleteX`) wrapping the shared `api` axios instance from `lib/axios.ts`. Components consume these through `@tanstack/react-query` (`useQuery`/`useMutation`). List endpoints are paginated and return `Page<T>` (`{ content, totalElements, totalPages, number, size }`), matching Spring's `Page<T>`.

`types/models.ts` is the contract file — every request/response DTO used against the real backend lives here and **must be kept in sync with the backend's Java DTOs** (`../novaerp-backend/src/main/java/com/novaerp/backend/**/dto/*.java`). When the backend DTO shape changes, update this file first.

## Environment

```
NEXT_PUBLIC_API_URL=http://localhost:8081/api   # backend base URL (see .env.example)
```

## Run locally

```bash
npm install
npm run dev        # http://localhost:3000
```

Requires the backend running (see `../novaerp-backend`, typically on `http://localhost:8081`).

Other scripts: `npm run build`, `npm run start`, `npm run lint`.

## Conventions / gotchas

- This project is on a bleeding-edge Next.js version — **read `node_modules/next/dist/docs/` before assuming any API**, per `AGENTS.md`. Don't rely on training-data Next.js conventions blindly.
- Middleware file is `proxy.ts`, not `middleware.ts` (Next 16 convention).
- `components/clients/` and `components/products/` reference a `Client`/`Product`/`Invoice`/`StockAlert`/`AuditLog` model shape in `types/models.ts` that has **no corresponding backend module yet** — treat these as future/mock-only, not wired to a real API (see `services/services.mock.ts`).
- Real, backend-wired domains: **auth, articles, categories, suppliers, units, stock movements, CSV import/export**.
