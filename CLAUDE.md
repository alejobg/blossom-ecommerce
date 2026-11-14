# Contexto del proyecto: Blossom E-commerce (prueba técnica)

Prueba técnica fullstack para Blossom: e-commerce con productos, usuarios y órdenes.
Responder siempre en español. El código, los comentarios, los commits y el README van en inglés.

## Prioridades del usuario
- Lo esencial primero, con código limpio. Mejor entregar menos pero bien.
- Los requisitos obligatorios (80% de cobertura, 5 pantallas mínimas) no se recortan; lo opcional sí.
- Cada módulo se cierra con sus tests en verde (`mvn clean verify`) antes de pasar al siguiente.

## Stack (decidido)
- Backend: Java 21, Spring Boot 3.5.7 (NO migrar a Boot 4), PostgreSQL 16, Flyway, springdoc-openapi.
- Tests: JUnit 5, Mockito, MockMvc, Testcontainers (Postgres real, no H2), JaCoCo con mínimo 80% de líneas.
- Frontend (pendiente): React + Vite + TypeScript, Context API, react-hook-form + zod, Tailwind o MUI.
- Entorno del usuario: Windows + PowerShell + IntelliJ. En PowerShell, los argumentos `-Dx.y=z` van entre comillas.
- Docker Engine 29: Testcontainers 1.21.3 falla con "client version 1.32 is too old"; se resolvió con
  `testcontainers.version` 1.21.4 en el pom y/o `~/.docker-java.properties` con `api.version=1.44`.

## Arquitectura
Hexagonal, un paquete por módulo (`com.blossom.ecommerce.<modulo>`):
- `domain/`: Java puro, sin anotaciones de Spring ni de JPA. Reglas de negocio e invariantes.
- `domain/port/in`: use cases (interfaz + commands). `domain/port/out`: puertos de repositorio.
- `application/`: servicios que implementan los use cases (`@Service` y `@Transactional` permitidos aquí).
- `infrastructure/in/rest`: controllers, DTOs (records con Bean Validation) y mapper estático.
- `infrastructure/out/persistence`: entidad JPA (extiende `BaseEntity`), repositorio Spring Data, adapter y mapper.
- `shared/`: `BaseEntity` (`@MappedSuperclass`), `DomainException`, `ResourceNotFoundException`,
  `DomainValidationException`, `PageQuery`/`PageResult`, `PageResponse`, `GlobalExceptionHandler` (RFC 7807).
- Los módulos se comunican por use cases, nunca usan el repositorio JPA de otro módulo.
- El esquema lo maneja Flyway (`db/migration`, `ddl-auto: validate`). Los datos demo van en `db/seed`
  (migración repetible), que se excluye en el perfil `test`.
- Usar `module product` como plantilla de estilo y estructura de tests.

## Estado
- [x] **Backend completo**: `product` (CRUD, búsqueda, soft delete), `user` (registro, login, JWT), `order`
      (checkout con reserva de stock y bloqueo pesimista, historial) y CORS para el frontend
      (`app.cors.allowed-origins`, por defecto `http://localhost:5173`, probado en `CorsConfigTest`).
      213 tests en verde, cobertura de líneas ~99%. Repo: https://github.com/alejobg/blossom-ecommerce (`main`).
- [ ] **Frontend en progreso con Copilot** en `frontend/`. NO tocar esa carpeta desde aquí
      (tampoco `.github/` ni los `package.json`/`package-lock.json` de la raíz: no hacer commit de ellos).
- [ ] Pendientes antes de entregar:
  - [ ] Mover el secreto JWT y la contraseña del admin a variables de entorno, con `.env.example`.
  - [ ] README final.
  - [ ] Preguntas teóricas en un documento aparte.
  - [ ] Capturas o video del flujo completo.

## Módulo `user` (hecho)
- `User` sealed abstracto -> `Customer` | `Admin`, JPA `SINGLE_TABLE` (`role` = discriminador). `Email` value object.
- Seguridad en `shared/security`: `SecurityConfig` (reglas de acceso, CORS), `JwtService`, filtro JWT, 401/403 en RFC 7807.
- El principal autenticado es `AuthenticatedUser(id, email, role)`: usar `@AuthenticationPrincipal AuthenticatedUser` en controllers.
- Admin inicial por migración `V3`: `admin@blossom.com` / `Admin123!`.
- Tests `@WebMvcTest` necesitan `@Import(SecurityConfig.class)`; usar `@WithMockUser(roles = ...)` o un token real de `JwtService`.
  Tests de integración que limpian usuarios: `DELETE FROM users WHERE role = 'CUSTOMER'` (no borrar el admin).

## Módulo `order` (hecho)
- `Order` (inmutable, total calculado) con `OrderItem` (nombre y precio unitario congelados). Migración `V4`.
- `OrderService` usa `ProductUseCase.reserveStock` (`Propagation.MANDATORY`); nunca el repositorio de product.
- Bloqueo pesimista `PESSIMISTIC_WRITE` ordenado por id en `ProductJpaRepository.findActiveByIdInForUpdate`.
- `InsufficientStockException` (en `shared/domain`) -> 409 con `productId`, `requested`, `available`.
- `/api/v1/orders/**` solo CUSTOMER; orden ajena -> 404. Historial en dos queries (ids paginados + `@EntityGraph`).
- Tests de integración con órdenes: limpiar `orders`/`order_items` en `@AfterEach`, porque otros tests borran
  clientes y productos (FKs).

## Git
- Este repo lo comparten dos agentes: Claude Code (backend) y Copilot (frontend).
- Agregar solo rutas de backend/, docs/, README.md, CLAUDE.md o docker-compose.yml.
  Nunca usar `git add .` ni `git add -A`. Nunca agregar frontend/ ni .github/.
- No hacer push salvo que el usuario lo pida.
