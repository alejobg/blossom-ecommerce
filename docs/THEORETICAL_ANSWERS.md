# Theoretical Answers

Answers to the theoretical questions of the Blossom technical assignment. Examples refer to the
code in `backend/src/main/java/com/blossom/ecommerce`.

---

## 1. How would you implement the Repository pattern in this project?

The project uses a hexagonal (ports and adapters) take on the Repository pattern. The repository the
business logic sees is **an interface owned by the domain**, written in domain terms, and the
database technology lives behind it as an infrastructure detail.

Each module has three pieces:

| Piece | Example (product module) | Role |
|---|---|---|
| Output port | `domain/port/out/ProductRepositoryPort` | The domain's repository: `save`, `findActiveById`, `searchActive`, `findActiveByIdsForUpdate`. It takes and returns `Product`, `ProductFilter`, `PageQuery` and `PageResult`: no JPA, no Spring types. |
| Adapter | `infrastructure/out/persistence/ProductPersistenceAdapter` | Implements the port. Translates domain objects to `ProductEntity` and back with `ProductPersistenceMapper`, and Spring `Page` into our own `PageResult`. It is package-private, so nothing outside the persistence package can depend on it. |
| Spring Data repository | `ProductJpaRepository` | A technical detail of the adapter: derived queries (`findByIdAndActiveTrue`), Criteria `Specification`s for search, and a `@Lock(PESSIMISTIC_WRITE)` query for stock reservation. |

The same structure is repeated in `user` (`UserRepositoryPort`, with `PasswordHasher` and
`TokenIssuer` as further output ports) and `order` (`OrderRepositoryPort`).

The port expresses **intent**, not storage mechanics. For example, `findActiveByIdsForUpdate`
documents that rows are locked in ascending id order to avoid deadlocks; how that happens
(`SELECT ... ORDER BY id FOR UPDATE`) is the adapter's concern. Soft delete works the same way:
the domain asks for "active" products and does not know an `active` column exists.

**Why the domain does not depend on JPA.** `Product`, `Order` and `User` are plain Java with no
annotations. Their invariants (positive price, stock never negative, an order's total calculated
from its items) live in constructors and methods, not in column constraints. This keeps the model
free of JPA requirements (no-arg constructors, mutable fields, proxies, lazy collections) and lets
domain objects be immutable where that makes sense (`Order`). The cost is a mapper per module and
some duplication between `Product` and `ProductEntity`; we accept that because the two models
change for different reasons.

**How it helps testing.** Each layer is tested with the right tool:

- Domain tests (`ProductTest`, `OrderTest`) are pure JUnit with no Spring context and run in
  milliseconds.
- Service tests (`ProductServiceTest`, `OrderServiceTest`) mock `ProductRepositoryPort` with
  Mockito, so business flows like "reject the whole reservation if one item lacks stock" are
  verified without a database.
- Adapter tests (`ProductPersistenceAdapterTest`, `ProductSpecificationsTest`) run against a real
  PostgreSQL through Testcontainers, which is where mapping, queries and locking have to be proven.

Modules also respect the boundary between them: `OrderService` reserves stock through
`ProductUseCase.reserveStock`, never through `ProductJpaRepository`.

---

## 2. Differences between JPA and Hibernate, and when you would use each

**JPA (Jakarta Persistence)** is a specification: a set of interfaces and annotations
(`jakarta.persistence.*`) that define how Java objects map to relational tables, how the
`EntityManager` and persistence context behave, JPQL, the Criteria API and lock modes. It has no
code that talks to a database by itself.

**Hibernate** is an implementation of that specification (the default one in Spring Boot). It
provides everything JPA requires, plus its own features: its own annotations (`@Formula`,
`@BatchSize`, `@SoftDelete`, `@NaturalId`, `@Type`), HQL extensions, second-level cache
integrations, `StatelessSession`, and many tuning properties.

**In this project we code against JPA only.** Searching the main sources finds no `org.hibernate`
import:

- Mappings use standard annotations: `@Entity`, `@Table`, `@Column`, `@Enumerated(EnumType.STRING)`,
  `@OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)` with `@OrderBy` in
  `OrderEntity`.
- Inheritance is standard JPA twice: `@MappedSuperclass` in `BaseEntity` (id and audit columns,
  filled by the `@PrePersist`/`@PreUpdate` callbacks) and `@Inheritance(SINGLE_TABLE)` with
  `@DiscriminatorColumn(name = "role")` in `UserEntity` for `CustomerEntity` / `AdminEntity`.
- Queries are JPQL (`@Query` in `ProductJpaRepository`), the Criteria API (`ProductSpecifications`)
  and `@EntityGraph` for the order history.
- Concurrency uses `jakarta.persistence.LockModeType.PESSIMISTIC_WRITE`, which Hibernate turns into
  `SELECT ... FOR UPDATE` on PostgreSQL.

Hibernate is still present, but only as runtime and configuration:
`spring.jpa.hibernate.ddl-auto: validate` (Hibernate checks the entities against the schema that
Flyway creates) and `spring.jpa.properties.hibernate.format_sql`. Our soft delete is a plain
`active` flag filtered in queries instead of Hibernate's `@SoftDelete`, which keeps the rule
visible in the code.

**When to use each.** Use JPA by default: it covers the large majority of an application like this
one, keeps the code portable and makes it easier to reason about. Use Hibernate-specific features
when there is a concrete need JPA cannot cover, and keep them in the persistence adapters. Examples
would be `@BatchSize` to fix an N+1 problem, a JSONB column type, `StatelessSession` for bulk
imports, or the second-level cache for read-heavy reference data. In a hexagonal design that choice
never reaches the domain, so it can be made later without changing business code.

---

## 3. What advantages does React offer compared to other frontend frameworks?

React is a UI library rather than a full framework, and most of its advantages and drawbacks come
from that.

**Advantages**

- **Small, stable core model.** UI is a function of state; components are functions; hooks handle
  state and side effects. Once that is understood, most of React is plain JavaScript/TypeScript:
  conditionals and `map` instead of a template syntax.
- **TypeScript and JSX work well together.** Props and state are typed like any other function, and
  the compiler checks templates because they are code.
- **Ecosystem and hiring.** It has the largest ecosystem (routing, forms such as react-hook-form,
  validation with zod, data fetching, component libraries such as MUI) and the largest pool of
  developers, which lowers onboarding and long-term risk.
- **Flexibility.** It can be adopted incrementally and used from small SPAs (Vite) up to
  server-rendered apps (Next.js), and React Native reuses the same mental model on mobile.

**How it compares**

- **Angular** is a complete, opinionated framework: router, forms, HTTP client, dependency injection
  and testing tools ship together, with strong conventions. That consistency is valuable for large
  enterprise teams. React needs more upfront decisions, but is lighter and has a shorter learning
  curve for small and medium projects.
- **Vue** offers templates that feel close to HTML, fine-grained reactivity and official router and
  state libraries (Vue Router, Pinia). Many developers find it easier to start with, and it needs
  less manual optimization. React has the bigger ecosystem and job market; Vue is a more integrated
  experience.

**Trade-offs to be honest about.** Freedom means choosing and maintaining more libraries; re-renders
sometimes need `memo`/`useMemo` or a state library, where Vue and Angular signals track dependencies
automatically; and the recommended way to build React apps has changed several times (classes,
hooks, server components).

For this assignment React fits well: a small SPA with a handful of screens, Context API for auth and
cart state, and react-hook-form + zod for validated forms.

---

## 4. How would you organize your time over 2 days to deliver this assignment?

The rule is simple: required features (backend modules, 80% coverage, at least 5 screens) first,
optional ones only when those are done. Each module is finished with its tests green
(`mvn clean verify`) before moving on, so a working build exists at the end of every block.

### Day 1: backend

| Block | Time | Work |
|---|---|---|
| Setup and model | 1 h | Spring Boot project, PostgreSQL in `docker-compose.yml`, Flyway, JaCoCo with the 80% threshold, Testcontainers base config. Design entities and relationships. |
| Product | 2 h | Domain model and invariants, hexagonal layers, CRUD, search with filters and pagination, soft delete, RFC 7807 errors. This module becomes the template for the rest. |
| User and JWT | 2 h | Registration, login, BCrypt, JWT filter, role rules (ADMIN manages products, CUSTOMER places orders), 401/403 responses. |
| Order | 2.5 h | Checkout with stock reservation in one transaction and pessimistic locking, frozen prices, purchase history. |
| Close the backend | 0.5 h | Coverage check, Swagger review, CORS for the frontend, commit. |

Tests are written together with each module, not at the end: unit tests for domain and services,
`@WebMvcTest` for controllers, Testcontainers for adapters and full API flows.

### Day 2: frontend, delivery and documentation

| Block | Time | Work |
|---|---|---|
| Frontend base | 1 h | Vite + React + TypeScript, routing, API client with the JWT, auth context. |
| Required screens | 4 h | Login, register, product catalog with search and filters, product detail, cart and checkout, order history and order detail. Forms with react-hook-form + zod. Product management stays in the API (Swagger UI): an admin screen is optional and goes to the end of the list. |
| Docker | 1 h | Dockerfiles and a `docker-compose.yml` that starts database, backend and frontend; secrets through environment variables with `.env.example`. |
| Documentation and demo | 1.5 h | README (setup, architecture, decisions), these theoretical answers, screenshots or a short video of the full flow. |
| Buffer | 0.5–1 h | Fixes found during the end-to-end run and a final review. |

### Priorities and cuts

If time runs short, the order of cuts is: visual polish, optional extras (frontend tests, CI,
additional filters), then Docker for the frontend. Coverage, the required screens, a working
checkout and a clear README are never cut: a smaller scope that runs and is well tested is worth
more than a larger one that half works.
