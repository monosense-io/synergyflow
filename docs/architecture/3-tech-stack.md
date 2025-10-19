# 3. Tech Stack

## 3.1 Technology Stack Table

| Category | Technology | Version | Purpose | Rationale |
|----------|-----------|---------|---------|-----------|
| **Frontend Language** | TypeScript | 5.3+ | Type-safe frontend development | Compile-time type checking, better IDE support, reduced runtime errors, seamless integration with Next.js |
| **Frontend Framework** | Next.js | 15.1+ | React framework with SSR/SSG | **Latest stable release** with Turbopack dev mode (30% faster builds), React 19 support, async Request APIs, improved caching defaults, Partial Prerendering (experimental), better TypeScript support, production-ready |
| **Frontend Runtime** | React | 19 | UI component library | **Latest stable release** with React Compiler (automatic memoization), improved Server Components, Actions, useFormStatus, useOptimistic hooks, Document Metadata support, better error handling, ref as prop, cleanup functions in ref callbacks |
| **UI Component Library** | Shadcn/ui + Radix UI | Latest | Accessible, customizable components | Headless components (Radix) with Tailwind styling (Shadcn), WCAG 2.1 AA compliant, copy-paste approach (no npm dependency bloat), full customization control |
| **CSS Framework** | Tailwind CSS | 3.4+ | Utility-first CSS framework | Rapid UI development, consistent design system, optimized bundle size (PurgeCSS), excellent with Shadcn/ui, mobile-first responsive design |
| **State Management (Client)** | Zustand | 4.5+ | Lightweight client state | Simpler than Redux (less boilerplate), TypeScript-first, React hooks API, sufficient for client-only UI state (modals, filters, preferences) |
| **State Management (Server)** | TanStack Query (React Query) | 5.17+ | Server state management | Server state caching and synchronization, automatic refetching, optimistic updates, reduces backend calls, excellent DX |
| **Backend Language** | Java | 21.0.2 LTS | Backend development | LTS release (support until 2029), virtual threads (Project Loom) for better concurrency, records for event types, pattern matching, sealed classes |
| **Backend Framework** | Spring Boot | 3.5.6 | Enterprise Java framework | Production-ready autoconfiguration, Spring Modulith support, Spring Data JPA integration, Spring Security OAuth2 Resource Server, actuator for health checks |
| **Modular Monolith** | Spring Modulith | 1.4.2 | Module boundary enforcement | Enforces module isolation (equivalent to microservices), in-JVM event bus with transactional outbox, event publication registry, module verification tests |
| **ORM / Data Access** | Spring Data JPA | 3.2.0 | Data access abstraction | Declarative repository pattern, automatic CRUD, query method generation, Spring Boot managed (Hibernate 6.4.0 provider), auditing support |
| **Boilerplate Elimination (MANDATORY)** | Lombok | 1.18.30 | Code generation via annotations | **MANDATORY**: Eliminates getters, setters, constructors, builders, equals/hashCode, toString. DTOs use @Value (immutable), entities use @Data, services use @RequiredArgsConstructor + @Slf4j. Reduces codebase by 40%, improves maintainability |
| **DTO Mapping (MANDATORY)** | MapStruct | 1.5.5.Final | Type-safe object mapping | **MANDATORY**: Compile-time DTO ↔ Entity mapping, zero runtime reflection overhead, type-safe conversions, custom mapping methods support. Eliminates manual mapping boilerplate, catches mapping errors at compile-time, integrates with Lombok via lombok-mapstruct-binding |
| **API Documentation (MANDATORY)** | SpringDoc OpenAPI | 2.3.0 | OpenAPI 3.0 spec generation | **MANDATORY**: Auto-generates Swagger UI from @RestController annotations, interactive API docs at /swagger-ui/index.html, eliminates manual OpenAPI YAML maintenance, supports @Schema annotations for DTO documentation, RFC7807 Problem Details support |
| **Workflow Engine** | Flowable | 7.1.0 | BPMN workflow orchestration | BPMN 2.0 timers for SLA tracking, embedded in Spring Boot (no separate server), visual workflow modeler, async job executor, durable state persistence |
| **Policy Engine** | OPA (Open Policy Agent) | 0.68.0 | Policy-driven authorization | Decoupled policy logic (Rego), explainable decision receipts, shadow mode testing, sidecar deployment (<10ms latency), policy-as-code versioned in Git |
| **API Style** | REST (OpenAPI 3.0) | - | Backend API communication | Simple, well-understood, OpenAPI spec for documentation, Spring Boot autoconfiguration, sufficient for MVP (GraphQL/tRPC not needed initially) |
| **Database** | PostgreSQL | 16.8 | Relational database | Shared cluster via CloudNativePG, JSONB for flexible fields (decision receipts), full-text search (knowledge articles), transactional integrity, mature ecosystem |
| **Connection Pooler** | PgBouncer | 1.21+ | Database connection pooling | Transaction mode pooling (1000 clients → 50 DB connections), cross-namespace access (synergyflow → cnpg-system), 3 instances for HA |
| **Cache** | DragonflyDB | Latest | Redis-compatible cache | Shared cluster, Redis protocol (use Spring Data Redis), faster than Redis (better memory efficiency), reference data caching, rate limiting counters |
| **File Storage** | S3-compatible (MinIO) | Latest | Object storage | Audit log cold storage (7 years retention), attachment storage, S3-compatible API (cloud-agnostic), self-hosted option available |
| **Authentication** | OAuth2 Resource Server | - | JWT-based authentication | Stateless JWT validation (RS256 asymmetric signing), Spring Security integration, delegated to external IdP (Keycloak/Auth0), 1-hour token expiration |
| **Frontend Testing** | Vitest + React Testing Library | Latest | Frontend unit/integration tests | Vite-native (faster than Jest), React Testing Library for component tests, TypeScript support, similar API to Jest (easy migration) |
| **Backend Testing** | JUnit 5 + Mockito | - | Backend unit tests | Spring Boot managed, JUnit 5 for parameterized tests, Mockito for mocking, @SpringBootTest for integration tests, target 80% coverage |
| **E2E Testing** | Playwright | 1.40+ | End-to-end testing | Cross-browser testing (Chromium, Firefox, WebKit), better DX than Selenium, TypeScript support, auto-wait for elements, trace viewer for debugging |
| **Build Tool (Backend)** | Gradle | 8.7.0 | Backend build automation | Kotlin DSL (type-safe), better performance than Maven (incremental builds), Spring Boot Gradle plugin, Flyway migration plugin |
| **Build Tool (Frontend)** | npm | 10+ | Frontend package management | Standard Node.js package manager, lockfile for reproducibility, workspace support (if monorepo tool added later) |
| **Bundler** | Vite (Next.js built-in) | - | Frontend bundler | Next.js uses Turbopack (dev) and Webpack (prod), fast HMR, optimized production bundles |
| **IaC Tool** | Kustomize + Flux CD | 2.2.3 | GitOps infrastructure | Declarative Kubernetes manifests, Kustomize overlays for environments (dev/staging/prod), Flux CD for automated deployments, no Terraform overhead |
| **CI/CD** | GitHub Actions | - | Continuous integration/deployment | Integrated with GitHub repo, Docker build and push to Harbor, automated testing, environment promotion gates (dev → staging → prod) |
| **Monitoring (Metrics)** | Victoria Metrics | 1.100.0 | Metrics collection and storage | Prometheus-compatible, better performance and storage efficiency than Prometheus, long-term retention, 15-second scrape interval |
| **Monitoring (Visualization)** | Grafana | 10.4.2 | Metrics dashboards | Victoria Metrics datasource, pre-built dashboards (API latency, event lag, business metrics), alerting via AlertManager |
| **Monitoring (Tracing)** | OpenTelemetry | 1.28.0 | Distributed tracing | Correlation ID propagation, trace context across HTTP requests, span tracking (API → DB → OPA), 10% sampling in production |
| **Logging** | Logback (Spring Boot default) | - | Structured JSON logging | JSON encoder for structured logs, correlation IDs in every log entry, log levels (DEBUG, INFO, WARN, ERROR), log aggregation to Loki (optional) |
| **Secrets Management** | ExternalSecrets Operator + 1Password | Latest | Secrets synchronization | Pull secrets from 1Password into Kubernetes Secrets, automatic rotation, no plaintext secrets in Git, operator-managed sync |

---
