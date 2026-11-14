# Blossom E-commerce

Full-stack e-commerce case: **Java 21 + Spring Boot 3 + PostgreSQL** backend and **React + Vite + TypeScript** frontend.

> Work in progress. Done so far: project skeleton, `product` (CRUD + search) and `user` (sign-up, login, JWT) with tests.
> Next: `order`, frontend.

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
└── shared/                      BaseEntity, domain exceptions, pagination, error handler,
                                 security/ (SecurityConfig, JwtService, JWT filter)
```

## Endpoints (so far)

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
- **Tests on real PostgreSQL** (Testcontainers) instead of H2, to avoid dialect differences.
