# Blossom E-commerce

Full-stack e-commerce built for the Blossom technical assignment. Customers browse a product catalog, search and
filter it, fill a cart, place orders and review their purchase history. Administrators manage the catalog through
the API.

- **Backend:** Java 21, Spring Boot 3.5, Spring Security with JWT, Spring Data JPA, PostgreSQL 16, Flyway, springdoc-openapi.
- **Frontend:** React 18, TypeScript, Vite, React Router, Context API, react-hook-form + zod, Tailwind CSS, axios.
- **Tests:** JUnit 5, Mockito, MockMvc, Testcontainers (real PostgreSQL), JaCoCo; Vitest and Testing Library on the frontend.

Answers to the theoretical questions: [`docs/THEORETICAL_ANSWERS.md`](docs/THEORETICAL_ANSWERS.md).

## Requirements

| To run | You need |
|---|---|
| Everything in Docker | Docker with Docker Compose |
| Locally | Java 21, Maven 3.9+, Node.js 20.19+ or 22.12+, Docker (for PostgreSQL and the backend tests) |

## Running the application

### Option A: Docker (recommended)

```bash
docker compose up --build
```

This starts three containers:

| Service | URL | Notes |
|---|---|---|
| `frontend` | http://localhost:5173 | Production build served by nginx, with fallback to `index.html` for React Router routes |
| `backend` | http://localhost:8080/api/v1 | Runs Flyway migrations and loads the demo catalog on first start |
| `postgres` | `localhost:5432` | Database `ecommerce`, user and password `ecommerce` |

`VITE_API_URL` is a build argument of the frontend image (see `docker-compose.yml`). It is baked into the bundle
and used by the browser, so it points at the host port of the backend. `CORS_ALLOWED_ORIGINS` on the backend
must contain the origin the frontend is served from (`http://localhost:5173`). If you change ports, update both.

### Option B: local development

```bash
# 1. Database
docker compose up -d postgres

# 2. Backend (http://localhost:8080)
cd backend
mvn spring-boot:run

# 3. Frontend (http://localhost:5173), in another terminal
cd frontend
npm install
npm run dev
```

The frontend reads `VITE_API_URL` from `frontend/.env` (copy `frontend/.env.example`); the default is
`http://localhost:8080/api/v1`.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local PostgreSQL, `ecommerce` / `ecommerce` | Database connection |
| `JWT_SECRET` | dev-only key in `application.yml` | Base64 HMAC key, at least 256 bits (`openssl rand -base64 48`) |
| `JWT_EXPIRATION` | `1h` | Access token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins allowed to call the API |
| `SERVER_PORT` | `8080` | Backend port |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Frontend: API base URL (build time) |

## Demo credentials

| Role | Email | Password |
|---|---|---|
| Admin | `admin@blossom.com` | `Admin123!` |
| Customer | register one from the **Register** screen | 8+ characters, letters and digits |

The admin is created by migration `V3`; change it outside a local or demo setup. A demo catalog is loaded from
`db/seed` on first start (tests do not use it).

## Screens

| Route | Screen | Access |
|---|---|---|
| `/login`, `/register` | Login and sign-up | guests |
| `/`, `/products` | Catalog with search by name, category and price range, paginated | public |
| `/products/:id` | Product detail with add to cart | public |
| `/cart` | Cart and checkout (asks to log in before placing the order) | public; checkout for customers |
| `/orders` | Purchase history | customers |
| `/orders/:id` | Order detail | customers |

## API

Interactive documentation: **Swagger UI at http://localhost:8080/swagger-ui.html** (OpenAPI JSON at
`/v3/api-docs`). Authenticated requests send `Authorization: Bearer <accessToken>`; in Swagger UI, use **Authorize**.

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | Create a customer account |
| POST | `/api/v1/auth/login` | public | Returns `{accessToken, tokenType, expiresIn, user}` |
| GET | `/api/v1/users/me` | authenticated | Current user's profile |
| GET | `/api/v1/products?name=&category=&minPrice=&maxPrice=&page=&size=` | public | Search (all filters optional, combinable) |
| GET | `/api/v1/products/{id}` | public | Product detail |
| POST | `/api/v1/products` | ADMIN | Create |
| PUT | `/api/v1/products/{id}` | ADMIN | Update |
| DELETE | `/api/v1/products/{id}` | ADMIN | Remove from catalog (soft delete) |
| POST | `/api/v1/orders` | CUSTOMER | Place an order: body `{"items": [{"productId": 1, "quantity": 2}]}` |
| GET | `/api/v1/orders?page=&size=` | CUSTOMER | Own orders, most recent first |
| GET | `/api/v1/orders/{id}` | CUSTOMER | Own order detail (404 if it belongs to someone else) |

Errors use RFC 7807 (`application/problem+json`) with the same shape everywhere, including 401/403 from the
security layer; validation errors add an `errors` map per field. Placing an order returns `201`, or:

- `400`: invalid body (no items, quantity < 1, the same product twice).
- `404`: a product does not exist or was removed. The body includes `productId`.
- `409`: not enough stock. The body includes `productId`, `requested` and `available`.

The frontend uses these properties to point at the cart line that failed, without parsing messages.

## Tests and coverage

```bash
# Backend: unit, web and integration tests + coverage check (Docker must be running)
cd backend
mvn clean verify

# Frontend
cd frontend
npm test
```

- Backend integration tests start a real PostgreSQL with Testcontainers.
- `mvn verify` **fails if line coverage drops below 80%** (currently about 99%). HTML report:
  `backend/target/site/jacoco/index.html`.
- Test layers: pure domain tests, service tests with Mockito over the ports, `@WebMvcTest` controller tests with
  the real security config, persistence adapter tests and full API flows on PostgreSQL (including concurrent purchases).

## Architecture

Backend: hexagonal (ports and adapters), **one package per module**:

```
com.blossom.ecommerce
├── product/
│   ├── domain/                  Product, ProductFilter, ProductCategory  (plain Java, no frameworks)
│   │   └── port/in | port/out   ProductUseCase, ProductRepositoryPort
│   ├── application/             ProductService (implements the use case)
│   └── infrastructure/
│       ├── in/rest/             Controller, DTOs, mapper
│       └── out/persistence/     JPA entity, Spring Data repository, Specifications, adapter
├── user/
│   ├── domain/                  User (sealed) -> Customer | Admin, Email, PasswordPolicy
│   │   └── port/in | port/out   UserUseCase, UserRepositoryPort, PasswordHasher, TokenIssuer
│   ├── application/             UserService
│   └── infrastructure/
│       ├── in/rest/             AuthController, UserController
│       └── out/                 persistence (SINGLE_TABLE entities), security (BCrypt, JWT adapters)
├── order/
│   ├── domain/                  Order, OrderItem (frozen name and unit price, computed total)
│   │   └── port/in | port/out   OrderUseCase, PlaceOrderCommand, OrderRepositoryPort
│   ├── application/             OrderService (uses ProductUseCase.reserveStock, never product's repository)
│   └── infrastructure/
│       ├── in/rest/             OrderController, DTOs, mapper
│       └── out/persistence/     OrderEntity 1-* OrderItemEntity, two-query history (no N+1)
└── shared/                      BaseEntity, domain exceptions, pagination, error handler,
                                 security/ (SecurityConfig, JwtService, JWT filter)
```

Frontend (`frontend/src`): `api/` (axios client and resources), `context/` (auth and cart state), `pages/`,
`components/`, `routes/` (route tree and access guards), `hooks/`, `types/`, `utils/`.

## Design decisions

### Backend

- **Framework-free domain.** Business rules (price > 0, stock ≥ 0, stock decrease, order total) live in plain Java
  classes and are tested without Spring. JPA entities are a separate model mapped in the persistence adapters.
- **Modules talk through use cases.** `OrderService` calls `ProductUseCase.reserveStock(...)`, which checks that the
  products exist, are active and have enough stock, decreases stock and returns the current name and price. The
  order module never touches the product tables or repository.
- **Frozen prices.** Orders trust nothing from the client but product ids and quantities: the customer comes from
  the token, name and price from the catalog. `OrderItem` keeps a copy of the name and unit price, so a later price
  change or a removed product does not alter past orders.
- **One transaction per order.** `reserveStock` uses `Propagation.MANDATORY`, so it always joins the checkout
  transaction. All lines are checked in memory before anything is written; if any line fails, nothing is stored and
  no stock is taken.
- **Pessimistic locking for stock.** Two customers buying the last unit at the same time must not both succeed.
  Products are read with `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`), so the second buyer waits for the first
  transaction and then sees the updated stock. Rows are locked **in ascending id order**, so two orders with the same
  products in a different order cannot deadlock. Optimistic locking (`@Version`) was discarded because contention on
  popular products would become failures the client has to retry. `CHECK (stock >= 0)` stays as a last line of defense.
- **Soft delete for products.** Removed products leave the catalog (`active = false`) but stay in the database, so
  past orders keep a valid reference. Inactive products cannot be ordered.
- **Stateless JWT authentication.** Login returns an HMAC-signed token with the user id, email and role; each request
  is authenticated from the token, without sessions. CSRF is disabled because no cookies are used. The domain only
  knows the `PasswordHasher` and `TokenIssuer` ports; BCrypt and jjwt live in adapters.
- **Admins do not buy.** `/api/v1/orders/**` is restricted to the CUSTOMER role. Another customer's order answers
  404, not 403, so the API does not reveal which order ids exist.
- **Inheritance.** `BaseEntity` (`@MappedSuperclass`) provides id and audit timestamps; `User` is a sealed class with
  `Customer` and `Admin`, stored with JPA `SINGLE_TABLE` inheritance (`role` is the discriminator).
- **History without N+1.** The history pages over order ids first, then loads those orders with their items in one
  query (`@EntityGraph`): two queries for any page size, no in-memory pagination.
- **Flyway owns the schema** (`ddl-auto: validate`); search uses JPA Specifications with escaped `LIKE` input.
- **Tests on real PostgreSQL** (Testcontainers) instead of H2, to avoid dialect differences, especially for locking.

### Frontend

- **Public catalog.** Browsing, search and product detail need no account; login is asked for only at checkout, and
  the user returns to the page they came from.
- **Local cart.** The cart lives in `localStorage` (validated with zod when it is read), so it survives reloads and
  works for guests. It is **emptied on logout**, so the next person on the same browser does not inherit it.
- **Admin accounts do not see purchase features.** Matching the backend rule, the cart is cleared when an admin logs
  in, purchase actions are hidden and the order screens show an access-denied message.
- **Stock-aware cart.** Quantities are limited by the last known stock. On a 404 or 409 at checkout, the API's
  `productId` is used to name the product to fix, and on a 409 the cart learns the new available stock.
- **Global 401 handling.** An axios interceptor clears the session on a 401 and redirects to login with a return URL.
- **State with Context API** (auth and cart) instead of a state library: two small, independent stores are enough.

## Development process

The project was built with two AI coding assistants, each with a clear area, while I stayed in charge of the result:

- **Claude Code** worked on the backend (modules, security, tests, Docker) and documentation, following the
  conventions written in `CLAUDE.md`: hexagonal structure, test strategy and git rules.
- **GitHub Copilot** worked on the frontend application in `frontend/`. Each tool stayed in its own area; the only
  exception is the frontend's Docker files, added by Claude Code when wiring up `docker-compose.yml`.
- **My role** was to decide and review: I chose the stack and the architecture, defined the scope and priorities
  (required features first, optional ones only if time allowed), split the work into modules, reviewed the changes
  and decided trade-offs such as pessimistic locking, frozen prices and the cart behavior.
- **Verification** did not rely on the assistants' word. Each backend module was closed only with
  `mvn clean verify` green, which runs unit and integration tests on real PostgreSQL and enforces the 80% coverage
  minimum. The frontend has its own Vitest suite, and the full Docker stack was checked end to end (SPA routes,
  API URL and CORS).

## Known limitations / next steps

- **Secrets in environment variables.** `JWT_SECRET` and the database credentials can already be overridden, but
  `application.yml` and `docker-compose.yml` ship development defaults, and the admin password is fixed in migration
  `V3`. Next step: require them from the environment (no defaults outside local), add an `.env.example` and create
  the initial admin from environment variables.
- **Token expiration on the client.** The frontend does not check `expiresIn`: an expired token is only detected when
  the API answers 401. Next step: store the expiry time and log out (or warn) before it is reached.
- **No admin screens.** Product management is available through the API and Swagger UI only.
- **No refresh tokens.** Sessions end when the access token expires (1 hour by default).
