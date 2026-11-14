# Blossom E-commerce

Full-stack e-commerce case: **Java 21 + Spring Boot 3 + PostgreSQL** backend and **React + Vite + TypeScript** frontend.

> Work in progress. Done so far: the backend: `product` (CRUD + search), `user` (sign-up, login, JWT) and
> `order` (checkout with stock reservation, purchase history), with tests. Next: frontend.

## Run locally

Requirements: Java 21, Maven 3.9+, Docker.

```bash
# Option A: everything in Docker
docker compose up --build

# Option B: database in Docker, backend from the IDE / Maven
docker compose up -d postgres
cd backend && mvn spring-boot:run
```

- API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html

A demo catalog is loaded on first start (`db/seed`). Tests do not use it.

**Initial admin** (created by migration `V3`): `admin@blossom.com` / `Admin123!`. Change it outside a local or demo setup.

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | dev-only key in `application.yml` | Base64 HMAC key, at least 256 bits (`openssl rand -base64 48`) |
| `JWT_EXPIRATION` | `1h` | Access token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated origins allowed to call the API (frontend) |

## Tests and coverage

```bash
cd backend && mvn verify
```

Integration tests start a real PostgreSQL with Testcontainers, so Docker must be running.
`mvn verify` fails if line coverage drops below **80%**. Report: `backend/target/site/jacoco/index.html`.

## Architecture

Hexagonal (ports and adapters), **one package per module**:

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

## Endpoints

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

Placing an order returns `201` with the order, or: `400` for an invalid body (no items, quantity < 1, the same
product twice), `404` if a product does not exist or was removed, and `409` if there is not enough stock. The 409
problem detail says which product failed: `{"title": "Insufficient stock", "productId": 3, "requested": 5, "available": 2, ...}`.

Authenticated requests send `Authorization: Bearer <accessToken>`. In Swagger UI, use the **Authorize** button.

Errors use RFC 7807 (`application/problem+json`) with a consistent shape, including 401/403 from the security layer;
validation errors add an `errors` map per field.

## Design decisions

- **Framework-free domain.** Business rules (price > 0, stock ≥ 0, stock decrease) live in `Product` and are tested without Spring. JPA entities are a separate model mapped in the persistence adapter.
- **Pragmatic application layer.** Services use `@Service` and `@Transactional`: the transaction boundary belongs to the use case (an order must reserve stock and save atomically).
- **Soft delete for products.** Deleted products leave the catalog but stay in the database, so past orders keep a valid reference.
- **Flyway owns the schema** (`ddl-auto: validate`), so the database is versioned and reproducible.
- **Search with JPA Specifications**: each filter is optional and they combine freely; `LIKE` input is escaped so `%` or `_` are searched literally.
- **Inheritance:** `BaseEntity` (`@MappedSuperclass`) provides id and audit timestamps to every entity.
  In the business model, `User` is a sealed abstract class with `Customer` and `Admin` subtypes, stored with
  JPA `SINGLE_TABLE` inheritance (`role` is the discriminator column). Mappers switch over the sealed hierarchy,
  so adding a subtype does not compile until it is mapped.
- **Stateless JWT authentication.** Login returns an HMAC-SHA signed token with the user id, email and role; each request is
  authenticated from the token, without sessions or a database lookup. CSRF is disabled because no cookies are used.
- **The domain does not know about BCrypt or JWT.** It depends on the `PasswordHasher` and `TokenIssuer` ports;
  Spring Security and jjwt live in infrastructure adapters.
- **Account safety:** emails are normalized (case-insensitive and unique, enforced by a DB constraint as well),
  passwords need 8+ characters with letters and digits and at most 72 bytes (BCrypt's limit), login gives the same
  error for an unknown email and a wrong password, and records holding secrets mask them in `toString()`.
- **Orders trust nothing from the client but product ids and quantities.** The customer comes from the token,
  and name and price come from the catalog. `OrderItem` keeps a copy of the name and unit price, so a later price change
  or a removed product does not alter past orders. The domain computes subtotals and the total.
- **Modules talk through use cases.** `OrderService` calls `ProductUseCase.reserveStock(...)`, which validates the
  products (exist, active, enough stock), decreases stock and returns the current name and price. The order module never
  touches the product tables or repository.
- **One transaction per order.** `reserveStock` runs with `Propagation.MANDATORY`, so it always joins the caller's
  transaction. If any line fails, or saving the order fails, everything rolls back: no stock is taken and no order
  is stored. All lines are checked in memory before anything is written.
- **Concurrency: pessimistic locking.** Two customers buying the last unit at the same time must not both succeed.
  Reading the products uses `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`): the second buyer waits until the first
  transaction commits and then sees the updated stock. Rows are locked **in ascending id order**, so two orders with
  the same products listed in a different order cannot deadlock. This was chosen over optimistic locking (`@Version`)
  because checkout contention on popular products is expected, and optimistic locking would turn it into failures the
  client must retry. Locks are held only for the short checkout transaction. The `CHECK (stock >= 0)` constraint stays
  as a last line of defense. Integration tests fire concurrent purchases to verify both properties.
- **Other customers' orders answer 404, not 403**, so the API does not reveal which order ids exist.
- **History without N+1.** Paging a collection fetch join would make Hibernate paginate in memory. The history first
  pages over order ids, then loads those orders with their items in one query (`@EntityGraph`): two queries for any page size.
- **Tests on real PostgreSQL** (Testcontainers) instead of H2, to avoid dialect differences.
