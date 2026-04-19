# Viva Guide: Warehouse Management System (WMS)

This document is a viva-focused walkthrough of the codebase, with special focus on the Java implementation under `src` (you wrote `scr`, which appears to mean `src`).

It explains:
- what the software does overall,
- how the Java/Spring Boot architecture is organized,
- what each file in `src` is responsible for,
- and common viva questions with short model answers.

---

## 1) Overall Software Purpose

This project is an Enterprise Warehouse Management System (WMS) built with Spring Boot.
It handles operational warehouse workflows end-to-end:

- inbound receiving (GRN)
- putaway location suggestion
- inventory and movement tracking
- outbound order allocation and picking
- shipping flow
- transfer between warehouses
- analytics and alerts (low stock / expiry)
- audit logging
- JWT-based security and role-based access control
- optional LLM and voice integrations

The UI is server-rendered (Thymeleaf templates) for dashboard/login, plus one React component under static assets.

---

## 2) Technology Stack

- Language: Java (Spring Boot)
- Web: Spring MVC + REST controllers
- Security: Spring Security + JWT
- Persistence: Spring Data JPA (Hibernate)
- Database: PostgreSQL (schema from `schema.sql`)
- Scheduling: Spring `@EnableScheduling` + `@Scheduled`
- UI: Thymeleaf templates + static assets
- Optional AI/Voice: OpenAI + pluggable STT/TTS providers

---

## 3) Java Architecture (Very Important for Viva)

The app follows layered architecture:

1. Controller Layer
- Receives HTTP requests
- Validates/accepts DTOs
- Calls services
- Returns JSON or page views

2. Service Layer
- Contains core business logic
- Coordinates multiple repositories
- Applies warehouse rules (FEFO, slotting, alerts)

3. Repository Layer
- Spring Data JPA repositories
- Encapsulates DB access and custom query methods

4. Domain Layer
- JPA entities and enums
- Models warehouses, locations, inventory, orders, users, alerts, etc.

Cross-cutting layers:
- Security (`SecurityConfig`, `JwtAuthFilter`, `JwtService`)
- Auditing (`AuditInterceptor`, `AuditService`)
- Global exception handling (`GlobalExceptionHandler`)
- Seed data (`DataInitializer`)

---

## 4) Main Request Flow (Controller -> Service -> Repository)

Example: Inbound GRN
1. `POST /api/wms/inbound/grn` hits `WmsController`
2. `WmsController` calls `InboundService.receiveGoods(...)`
3. Service fetches product/warehouse, creates lot if needed, updates inventory
4. Service records movement in `MovementHistory`
5. Repository persists data in PostgreSQL

Example: Login
1. `POST /api/auth/login` hits `AuthController`
2. `AuthenticationManager` validates username/password
3. `JwtService` issues signed token
4. Client sends `Authorization: Bearer <token>` for protected APIs
5. `JwtAuthFilter` validates token and loads SecurityContext

---

## 5) Security Model

- Public: `/`, `/login`, `/app`, `/api/auth/**`
- Admin only: `/api/users/**`
- Admin/Manager: `/api/wms/analytics/**`, `/api/wms/audit/**`, `/api/wms/alerts/**`
- All authenticated users: `/api/wms/**`

Roles used in code:
- `ADMIN`
- `MANAGER`
- `WORKER`

---

## 6) File-by-File Explanation of `src`

Total files under `src`: 65

## 6.1 Java entry point

### `src/main/java/com/enterprise/wms/WmsApplication.java`
- Spring Boot main class.
- Starts application context.
- Enables scheduled jobs via `@EnableScheduling`.

## 6.2 Config package (`config`)

### `src/main/java/com/enterprise/wms/config/SecurityConfig.java`
- Defines Spring Security filter chain.
- Configures route authorization rules.
- Adds `JwtAuthFilter` before username/password auth filter.
- Sets stateless sessions and BCrypt password encoder.

### `src/main/java/com/enterprise/wms/config/JwtAuthFilter.java`
- Intercepts incoming requests.
- Extracts JWT from `Authorization` header.
- Validates token and sets authenticated user in `SecurityContext`.

### `src/main/java/com/enterprise/wms/config/DataInitializer.java`
- Seed/bootstrap component (runs at startup).
- Creates initial roles/users, warehouses, locations, products, stock, orders, etc. for demo/testing.

### `src/main/java/com/enterprise/wms/config/GlobalExceptionHandler.java`
- Centralized exception-to-HTTP mapping.
- Returns consistent API error responses.

### `src/main/java/com/enterprise/wms/config/AuditInterceptor.java`
- Intercepts API requests and logs request metadata (user, method, path, status, time) into audit storage.

### `src/main/java/com/enterprise/wms/config/WebMvcConfig.java`
- Registers MVC interceptors (notably `AuditInterceptor`) for API routes.

## 6.3 Controller package (`controller`)

### `src/main/java/com/enterprise/wms/controller/AuthController.java`
- Auth endpoints under `/api/auth`.
- `POST /login` authenticates credentials and returns JWT token.

### `src/main/java/com/enterprise/wms/controller/UserController.java`
- User administration endpoints under `/api/users`.
- List users, create users, delete users, update user roles.
- Works with `UserManagementService` and user DTOs.

### `src/main/java/com/enterprise/wms/controller/WmsController.java`
- Main WMS REST API under `/api/wms`.
- Handles inbound, putaway, outbound, inventory, analytics, alerts, audit, LLM and voice endpoints.
- Contains helper paging/sorting builder.
- Also includes `WmsUiController` class in same file for page routes (`/`, `/login`, `/app`).

## 6.4 Domain enums (`domain`)

### `src/main/java/com/enterprise/wms/domain/WmsEnums.java`
- Central enum definitions:
  - location/movement types
  - order and picking statuses
  - picking strategies
  - alert types
  - role names
  - velocity/weight classes
- Prevents magic strings and keeps business states consistent.

## 6.5 Domain entities (`domain/entity`)

### `src/main/java/com/enterprise/wms/domain/entity/AppUser.java`
- User account entity.
- Stores username and password hash.
- Many-to-many with roles.

### `src/main/java/com/enterprise/wms/domain/entity/Role.java`
- Role entity (`ADMIN`, `MANAGER`, `WORKER`).
- Linked to users through join table.

### `src/main/java/com/enterprise/wms/domain/entity/Warehouse.java`
- Warehouse master entity (code, name).
- Parent context for locations, inventory, and orders.

### `src/main/java/com/enterprise/wms/domain/entity/Location.java`
- Physical storage location (zone/rack/shelf/bin, type, capacity, occupied).
- Belongs to a warehouse.

### `src/main/java/com/enterprise/wms/domain/entity/Product.java`
- Product master (SKU, barcode, name, reorder level, classification).
- Used across inventory and order lines.

### `src/main/java/com/enterprise/wms/domain/entity/LotBatch.java`
- Batch/lot with expiry and receive metadata.
- Enables FEFO allocation and expiry analytics.

### `src/main/java/com/enterprise/wms/domain/entity/Inventory.java`
- Stock record for product at warehouse/location/lot.
- Holds `quantity` and `reservedQty`.

### `src/main/java/com/enterprise/wms/domain/entity/SalesOrder.java`
- Sales order header (order number, status, warehouse, timestamp).

### `src/main/java/com/enterprise/wms/domain/entity/SalesOrderLine.java`
- Sales order line items with requested and allocated quantity.

### `src/main/java/com/enterprise/wms/domain/entity/PickingTask.java`
- Picking execution task linked to orders/workers.
- Stores strategy, status, progress.

### `src/main/java/com/enterprise/wms/domain/entity/MovementHistory.java`
- Immutable movement/audit trail for inventory operations.
- Captures movement type, quantity, from/to locations, reference and operator.

### `src/main/java/com/enterprise/wms/domain/entity/Alert.java`
- System alert entity (low stock, expiry, dead stock, replenishment etc.).
- Stores message, severity and resolved flag.

### `src/main/java/com/enterprise/wms/domain/entity/AuditLog.java`
- API audit record entity from interceptor pipeline.

## 6.6 DTO package (`dto`)

### `src/main/java/com/enterprise/wms/dto/InboundDtos.java`
- Request/response structures for inbound receiving.

### `src/main/java/com/enterprise/wms/dto/OutboundDtos.java`
- DTOs for picking task creation and status updates.

### `src/main/java/com/enterprise/wms/dto/OrderDtos.java`
- DTOs for sales order creation and wave planning.

### `src/main/java/com/enterprise/wms/dto/TransferDtos.java`
- DTOs for inter-warehouse stock transfer.

### `src/main/java/com/enterprise/wms/dto/UserDtos.java`
- DTOs for user create/update/list APIs.

### `src/main/java/com/enterprise/wms/dto/LlmDtos.java`
- DTOs for natural-language queries and parsed action commands.

## 6.7 Repository package (`repository`)

### `src/main/java/com/enterprise/wms/repository/AppUserRepository.java`
- Data access for users (lookup by username).

### `src/main/java/com/enterprise/wms/repository/RoleRepository.java`
- Data access for roles (lookup by enum name).

### `src/main/java/com/enterprise/wms/repository/WarehouseRepository.java`
- Data access for warehouses (lookup by code).

### `src/main/java/com/enterprise/wms/repository/LocationRepository.java`
- Data access for locations, including capacity/occupancy filters.

### `src/main/java/com/enterprise/wms/repository/ProductRepository.java`
- Product lookups by SKU/barcode.

### `src/main/java/com/enterprise/wms/repository/LotBatchRepository.java`
- Batch queries, including expiry-based retrieval.

### `src/main/java/com/enterprise/wms/repository/InventoryRepository.java`
- Inventory stock queries by warehouse/product/lot and availability.

### `src/main/java/com/enterprise/wms/repository/SalesOrderRepository.java`
- Sales order persistence and retrieval.

### `src/main/java/com/enterprise/wms/repository/SalesOrderLineRepository.java`
- Sales order line persistence and line-by-order lookup.

### `src/main/java/com/enterprise/wms/repository/PickingTaskRepository.java`
- Picking task persistence and task listing.

### `src/main/java/com/enterprise/wms/repository/MovementHistoryRepository.java`
- Movement history queries by time/product.

### `src/main/java/com/enterprise/wms/repository/AlertRepository.java`
- Alert retrieval for unresolved and type-filtered alerts.

### `src/main/java/com/enterprise/wms/repository/AuditLogRepository.java`
- Audit log retrieval, generally paginated.

## 6.8 Service package (`service`)

### `src/main/java/com/enterprise/wms/service/JwtService.java`
- Creates and validates JWTs.
- Extracts username and checks expiry/signature validity.

### `src/main/java/com/enterprise/wms/service/CustomUserDetailsService.java`
- Spring Security `UserDetailsService` implementation.
- Converts application users/roles into security authorities.

### `src/main/java/com/enterprise/wms/service/UserManagementService.java`
- User lifecycle and role updates.
- Enforces business checks (e.g., safe deletion).

### `src/main/java/com/enterprise/wms/service/InboundService.java`
- Inbound GRN processing.
- Creates/updates lot and inventory records.
- Writes receive movement history.

### `src/main/java/com/enterprise/wms/service/PutawayService.java`
- Suggests optimal storage location.
- Uses slotting score rules (zone/shelf/velocity/weight aware).

### `src/main/java/com/enterprise/wms/service/OutboundService.java`
- Order creation, allocation, picking task creation, shipping transition.
- Core FEFO allocation logic reserves oldest-expiring stock first.

### `src/main/java/com/enterprise/wms/service/InventoryService.java`
- Real-time stock and movement history fetch.
- Cycle count adjustments.
- Replenishment and movement-classification analytics helpers.

### `src/main/java/com/enterprise/wms/service/TransferService.java`
- Warehouse-to-warehouse transfer logic.
- Updates source/destination inventory and writes transfer movements.

### `src/main/java/com/enterprise/wms/service/AnalyticsService.java`
- Rule engine for alerts.
- Low-stock and expiry checks.
- Scheduled execution plus manual trigger.

### `src/main/java/com/enterprise/wms/service/AuditService.java`
- Audit log query facade for controller layer.

### `src/main/java/com/enterprise/wms/service/OpenAiActionService.java`
- Converts natural-language prompts into structured action commands.
- Uses OpenAI when configured; fallback rule parsing when not.

### `src/main/java/com/enterprise/wms/service/VoiceService.java`
- Speech-to-text and text-to-speech integration.
- Supports configurable providers (simulated/custom/google/vosk/openai depending on mode).

## 6.9 Resources (`src/main/resources`)

### `src/main/resources/application.yml`
- All runtime configuration:
  - DB and JPA
  - JWT secret/expiry
  - slotting bonuses and prefixes
  - OpenAI and voice provider settings
  - server tuning

### `src/main/resources/schema.sql`
- Explicit PostgreSQL schema:
  - all core tables
  - constraints and unique keys
  - audit/movement indexes
  - `progress_pct` migration-safe add for picking task

## 6.10 Templates (`src/main/resources/templates`)

### `src/main/resources/templates/login.html`
- Active login page template.

### `src/main/resources/templates/dashboard.html`
- Active dashboard page template.

### `src/main/resources/templates/login_old.html`
- Older login variant retained as backup/reference.

### `src/main/resources/templates/dashboard_backup.html`
- Backup dashboard version.

### `src/main/resources/templates/dashboard_new.html`
- Alternate/new dashboard variant.

### `src/main/resources/templates/dashboard_old_backup.html`
- Older dashboard backup variant.

### `src/main/resources/templates/dashboard_purple_backup.html`
- Themed dashboard backup variant.

## 6.11 Static assets (`src/main/resources/static`)

### `src/main/resources/static/react/HeroSection.tsx`
- React hero section component with responsive navigation and video background.
- Appears to be design/marketing UI component, not the main WMS business API layer.

---

## 7) Core Business Logic to Explain in Viva

1. FEFO allocation
- In outbound allocation, inventory is consumed by earliest expiry first.
- Helps reduce wastage for expiring goods.

2. Slotting-based putaway
- Location suggestion uses product class (fast/slow, heavy/light) and location metadata (zone/shelf).
- Improves retrieval speed and ergonomics.

3. Reserved quantity pattern
- Uses `quantity` + `reservedQty` in inventory.
- Prevents over-allocation while picking is in progress.

4. Movement history for traceability
- Every major stock operation is logged.
- Useful for audit, analytics, and accountability.

5. Alert rule engine
- Identifies low stock and expiry risk.
- Can run via endpoint and scheduler.

---

## 8) Common Viva Questions (Short Model Answers)

1. Why DTOs instead of exposing entities directly?
- DTOs decouple API contracts from persistence models and reduce accidental overexposure of internal fields.

2. Why service layer when repository exists?
- Services centralize business rules/transactions and coordinate multiple repositories in one use case.

3. Why JWT here?
- Stateless token-based auth scales well for APIs and works cleanly with role claims and filters.

4. What is the role of `JwtAuthFilter`?
- It validates bearer tokens for each request and sets authenticated context before controllers run.

5. Why keep `reservedQty`?
- It separates physically available stock from stock already committed to open picking tasks.

6. Why global exception handler?
- Gives consistent error JSON and HTTP codes instead of scattered try/catch in controllers.

7. Why `@EnableScheduling`?
- To run recurring business checks (like low stock/expiry alerts) automatically.

8. Why audit interceptor?
- For compliance and traceability of who called which endpoint and with what status.

9. Why explicit `schema.sql` if using JPA?
- Gives stronger schema control, stable constraints, and predictable DB structure across environments.

10. How are roles enforced?
- Route-level authorization rules in `SecurityConfig` map endpoint groups to role requirements.

---

## 9) Suggested Viva Narration (1-minute summary)

"This is a layered Spring Boot WMS. Controllers expose REST APIs, services implement warehouse rules, repositories handle JPA persistence, and entities model the domain. Security is JWT-based with role access. Core flows are inbound receipt, putaway recommendation, outbound FEFO allocation, picking, shipping, and transfer. Inventory is traceable through movement history and API calls are audited through an interceptor. The system also includes alert analytics and optional voice/LLM integration. Overall, the design is modular and production-oriented for warehouse operations."