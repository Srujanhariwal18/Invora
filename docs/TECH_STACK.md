# INVORA — Technical Architecture & Technology Stack Specification

## 1. Executive Summary

**Invora** is an enterprise-grade Warehouse Inventory Management and Real-Time Threshold Alert System built with **Java 21**, **Spring Boot 3.x**, **Spring Data JPA**, and **Supabase (PostgreSQL 15+)**. The frontend is a modern, high-performance vanilla web client featuring a dark "warehouse control center" visual aesthetic and an interactive 3D digital twin warehouse powered by Three.js.

The architecture emphasizes strict separation of concerns, enterprise concurrency controls (pessimistic locking and optimistic lock versioning), atomic inventory transactions, automated threshold alert lifecycle management, and real-time updates via Server-Sent Events (SSE).

---

## 2. Technology Stack Overview

### 2.1 Backend Platform

| Component | Technology | Version | Rationale & Specifications |
|---|---|---|---|
| **Language** | Java | 21 (LTS) | Modern LTS features: Record types for DTOs, Pattern Matching, Sequenced Collections, Virtual Threads (optional), switch expressions. |
| **Framework** | Spring Boot | 3.4.x | Standard enterprise micro-framework providing auto-configuration, robust dependency management, and production-grade monitoring. |
| **Build Tool** | Apache Maven + Maven Wrapper | 3.9+ / `mvnw` | Deterministic builds without requiring global Maven installation on host machines. |
| **Web Layer** | Spring Web (MVC / REST) | 3.4.x | High-throughput REST API architecture serving JSON endpoints and static assets. |
| **Persistence Layer** | Spring Data JPA / Hibernate | 6.x | Object-relational mapping, custom JPQL queries, pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`), and optimistic locking (`@Version`). |
| **Database** | Supabase (PostgreSQL) | 15+ | Cloud-hosted managed PostgreSQL via Supabase, `TIMESTAMPTZ` with millisecond precision, explicit foreign keys, indexes, optional Row Level Security (RLS), and `CHECK (quantity >= 0)` constraints. Connection pooling via Supavisor / HikariCP. |
| **Database Migrations** | Flyway | 10.x (`flyway-core` + `flyway-database-postgresql`) | Versioned, immutable SQL schema migrations (`V1__schema.sql`, `V2__seed_data.sql`). |
| **Security & Auth** | Spring Security | 6.x | Stateless JWT bearer authentication, BCrypt password hashing, and role-based access control. |
| **Validation** | Jakarta Bean Validation | 3.0 (`hibernate-validator`) | Declarative DTO validation (`@NotNull`, `@Positive`, `@Size`, `@Pattern`, custom constraints). |
| **Scheduling** | Spring `@Scheduled` | 3.4.x | Automated background tasks for daily inventory summaries, expiry tracking, and alert checks. |
| **Real-Time Streaming** | Server-Sent Events (`SseEmitter`) | Spring Web | Low-overhead, unidirectional HTTP push channel for live stock updates, threshold status changes, and alert broadcasts. |
| **API Documentation** | springdoc-openapi | 2.6+ (Swagger UI) | Interactive API exploration and OpenAPI 3.0 specification available at `/swagger-ui.html`. |
| **Code Style & Encapsulation** | Pure Java (No Lombok) | — | Explicit private fields, getters, setters, and constructors to demonstrate pure encapsulation and educational clarity. |
| **Object Mapping** | Manual DTO Mappers | — | Explicit Java mappers without bytecode enhancement (no MapStruct), maximizing transparency and debugging ergonomics. |

---

### 2.2 Frontend Platform (No-Build Architecture)

| Component | Technology | Rationale & Specifications |
|---|---|---|
| **Architecture** | Vanilla HTML5, CSS3 & ES Modules | Zero build step, instant reloading, served directly by Spring Boot from `src/main/resources/static/`. |
| **Design System** | Custom CSS3 with Design Tokens | Dark industrial aesthetic (`#05070A` background, cyan accent `#22D3EE`, glassmorphism panels, soft borders). No generic Bootstrap. |
| **Typography** | Inter & Space Grotesk | Google Fonts with system fallback stack for crisp readability across dense warehouse data. |
| **Icons** | Lucide Icons (SVG) | Lightweight, crisp vector icons for industrial status and warehouse equipment. |
| **Data Visualization** | Chart.js (vendored) | Interactive line, bar, doughnut, and forecast charts in dark-mode styling. |
| **3D Digital Twin** | Three.js (vendored ES module) | Procedural 3D warehouse view with OrbitControls, custom rack shaders/colors representing worst-product health status, raycasting tooltips, and camera fly-to animations. |
| **2D Fallback** | CSS Grid / Canvas | Automatic responsive fallback if WebGL is disabled or on mobile devices. |
| **Real-Time Client** | Native `EventSource` (SSE) | Automatic connection handling with 15-second polling fallback upon disconnection. |

---

### 2.3 Testing & Quality Assurance

| Layer | Tools | Scope |
|---|---|---|
| **Unit Testing** | JUnit 5, Mockito, AssertJ | Business logic verification: `ThresholdService`, `ForecastService`, entity methods, custom multipliers, mathematical boundaries. |
| **Integration Testing** | Spring Boot Test, MockMvc | Full REST API lifecycle, role permissions, CSRF validation, HTTP error handling. |
| **Concurrency Testing** | Parallel ExecutorService + MockMvc / Services | Parallel stock-out requests verifying negative-stock prevention and database lock isolation. |
| **Database Testing** | H2 (PostgreSQL Mode) & Testcontainers PostgreSQL | Test isolation without modifying development databases. |

---

## 3. System Architecture & Layers

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Vanilla ES Modules)                       │
│   static/                                                                   │
│    ├── css/ (design-system.css, components.css, layout.css)                │
│    ├── js/ (app.js, api.js, auth.js, sse.js, warehouse-3d.js, alerts.js)   │
│    └── *.html (dashboard, inventory, stock-in, stock-out, warehouse, etc.)  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ HTTP / REST / SSE / Cookies
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                    SPRING BOOT REST CONTROLLER LAYER                        │
│   AuthController, ProductController, StockController, AlertController...   │
│   - Global Exception Handler (@RestControllerAdvice)                        │
│   - CSRF & Session Security Filters                                         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ DTOs (Records / Command Objects)
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                        SERVICE INTERFACE & IMPL LAYER                       │
│   InventoryService, ThresholdService, AlertService, AuditService...         │
│   - Pessimistic Locking & Transaction Demarcation (@Transactional)         │
│   - ApplicationEventPublisher -> StockChangedEvent                          │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ JPA Entities
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                        SPRING DATA JPA REPOSITORIES                         │
│   ProductRepository, InventoryRepository, AlertRepository, RackRepository  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ JDBC / SQL
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                  DATABASE (Supabase / PostgreSQL 15+)                      │
│   - Versioned Migrations (Flyway V1, V2)                                    │
│   - CHECK (quantity >= 0), Foreign Keys, Composite Indexes                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Key Business Logic Specifications

### 4.1 Stock Status Evaluation (`ThresholdService`)
Stock status is evaluated on a product's **total quantity across all racks** against its configured threshold:

```text
if (stock == 0)                             -> OUT_OF_STOCK  (Hex: #DC2626)
else if (stock <= threshold * critMult)     -> CRITICAL      (Hex: #EF4444, Default <= 50%)
else if (stock <= threshold)                -> WARNING       (Hex: #F97316, Default <= 100%)
else if (stock <= threshold * lowMult)      -> LOW           (Hex: #F59E0B, Default <= 150%)
else                                        -> NORMAL        (Hex: #22C55E, Default > 150%)
```

#### Boundary Verification Matrix:
| Stock Quantity | Configured Threshold | Calculated Status | Reason |
|:---:|:---:|:---:|---|
| **0** | 100 | **OUT_OF_STOCK** | Zero stock condition |
| **40** | 100 | **CRITICAL** | `40 <= 100 * 0.5` |
| **50** | 100 | **CRITICAL** | Exact critical boundary |
| **82** | 100 | **WARNING** | `50 < 82 <= 100` |
| **100** | 100 | **WARNING** | Exact threshold boundary |
| **140** | 100 | **LOW** | `100 < 140 <= 100 * 1.5` |
| **150** | 100 | **LOW** | Exact low boundary |
| **151** | 100 | **NORMAL** | Above low multiplier |

### 4.2 Concurrency & Negative Stock Safeguards
1. **Application Level**: Validates `requestedQuantity <= currentQuantity`.
2. **JPA Level**: Queries execute with `@Lock(LockModeType.PESSIMISTIC_WRITE)` to serialize parallel transactions touching the same inventory row.
3. **Optimistic Guard**: An `@Version Long version` attribute guards against stale detached modifications.
4. **Database Level**: Table constraint `CHECK (quantity >= 0)` ensures Supabase / PostgreSQL enforces non-negative stock even under unexpected race conditions.

---

## 5. Security & Role-Based Access Control (RBAC)

| Role | Dashboard & Racks | Stock Operations (IN/OUT/Transfer) | Alert Acknowledge/Resolve | Threshold Changes | Catalog / Warehouse Management | User Mgmt & Audit Logs |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **ADMIN** | Full | Full | Full | Full | Full | Full |
| **WAREHOUSE_MANAGER** | Full | Full | Full | Full | Read Only | None |
| **STAFF** | Full | Full | None | None | Read Only | None |
| **VIEWER** | Read Only | None | None | None | Read Only | None |

---

## 6. Project Directory Structure

```text
d:\Projects\Invora\Invora\Invora/
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── docker-compose.yml
├── .env.example
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── TECH_STACK.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── invora/
│   │   │           ├── InvoraApplication.java
│   │   │           ├── config/
│   │   │           ├── controller/
│   │   │           ├── dto/
│   │   │           ├── event/
│   │   │           ├── exception/
│   │   │           ├── mapper/
│   │   │           ├── model/
│   │   │           │   └── enums/
│   │   │           ├── repository/
│   │   │           ├── security/
│   │   │           ├── service/
│   │   │           │   └── impl/
│   │   │           └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── db/
│   │       │   └── migration/
│   │       │       ├── V1__schema.sql
│   │       │       └── V2__seed_data.sql
│   │       └── static/
│   │           ├── css/
│   │           ├── js/
│   │           ├── vendor/
│   │           │   ├── chart.js
│   │           │   └── three.module.js
│   │           ├── index.html
│   │           ├── login.html
│   │           ├── dashboard.html
│   │           ├── inventory.html
│   │           ├── stock-in.html
│   │           ├── stock-out.html
│   │           ├── transfers.html
│   │           ├── alerts.html
│   │           ├── warehouse.html
│   │           ├── analytics.html
│   │           ├── products.html
│   │           ├── suppliers.html
│   │           ├── activity.html
│   │           ├── users.html
│   │           └── settings.html
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── invora/
│       └── resources/
│           └── application-test.yml
```
