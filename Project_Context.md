# Project Context: EduBasic

## Project Overview
EduBasic is a multi-service educational platform designed to provide an adaptive learning and testing experience. It features secure OAuth2 authentication, a dynamic exam engine with AI-driven difficulty adjustment (via Gemini AI), a course management system, and a secure coding sandbox. The project is fully containerized for seamless development and deployment.

## Architecture
The system follows a microservices architecture using Docker Compose for orchestration:
- **Service-Oriented**: Decoupled backend services for `auth`, `course-service`, `exam`, and `sandbox-service`.
- **Database per Service**:
  - `auth` uses **Neo4j** (Graph) for complex user relationships/sessions and **Redis** for caching.
  - `course-service` uses **PostgreSQL** (`course_db`) for course content and progress tracking.
  - `exam` uses **PostgreSQL** (`exam_db`) for structured exam content and results.
  - `sandbox-service` uses **PostgreSQL** (`sandbox_db`) for coding challenge metadata.
- **Event-Driven**: Uses **Kafka** for inter-service communication (e.g., notifying `course-service` when an `exam` is completed).
- **Observability**: **Zipkin** is used for distributed tracing across services.
- **Stateless Auth**: JWT-based authentication via HttpOnly cookies (RSA key pair).
- **Communication**: Frontend communicates with services via REST APIs. Interservice communication is handled via Kafka or direct REST/WebClient calls.

## Stack
- **Frontend**: React 19, Vite 8, Zustand (State Management), React Query (TanStack), React Router 7, Axios, Lucide React, Vitest.
- **Backend (Java)**: Spring Boot 3.5.0, Spring Security (OAuth2/JWT), Spring Data JPA/Neo4j/Redis, Spring Kafka, Spring Actuator, Brave (Tracing), Flyway (Migrations).
- **AI**: Google Gemini AI (`gemini-flash-latest`).
- **Databases**: Neo4j 5.x, Redis 7.4, PostgreSQL 16.
- **Infrastructure**: Docker, Docker Compose, Kafka (Confluent 7.8), Zookeeper, Zipkin 3.4, Nginx (Frontend server).

## Concurrency & Performance Standards

### Core Mandates
- **Non-Blocking IO**: All blocking calls to external services (Neo4j, Gemini AI, Docker API) must be wrapped in `@Async` or handled via reactive patterns using a dedicated `AsyncTaskExecutor`.
- **Thread Pool Management**:
    - Use dedicated, named thread pools for different tasks (e.g., `geminiExecutor`, `dockerExecutor`).
    - Prefix all custom executors with `edubasic-` for visibility in traces and thread dumps.
    - Propagate `Brave/Zipkin` trace context into `@Async` threads using a `TraceableExecutorService` wrapper.
- **Locking & Consistency**:
    - Prefer **Optimistic Locking** (`@Version`) for handling concurrent updates to entities (e.g., exam submissions, course progress) to avoid pessimistic lock contention.
    - Use database-level atomic increments for simple counters where appropriate.
- **Messaging (Kafka)**:
    - Ensure Kafka producers are non-blocking using `CompletableFuture` or `ListenableViewFuture` callbacks.
    - Configure consumer concurrency (minimum `3` or based on partition count).
    - Implement retry and dead-letter topics for robust error handling.
- **Error Handling**: Every `@Async` method must have a corresponding `@Async` fallback or a comprehensive catch block to prevent silent failures.
- **Observability**: Expose concurrency metrics (active thread counts, Kafka lag, semaphore permits, Redis pool stats) via Spring Actuator endpoints.

### Service-Specific Standards

#### Auth Service
- Audit `JwtAuthenticationFilter` for statelessness and thread-safety.
- Use Lettuce connection pool tuning (`LettucePoolingClientConfiguration`) for Redis.
- Implement a Redis-backed token cache with TTL matching JWT expiry for session lookups.

#### Exam Service
- Wrap Gemini AI calls in a dedicated `geminiExecutor` pool with core/max sizes tuned for burst traffic.
- Handle exam submissions with optimistic locking on the `Exam/Result` JPA entity.
- Report Zipkin spans asynchronously to keep them off the critical path.

#### Course Service
- Protect course completion increments from race conditions.
- Configure `DefaultErrorHandler` with exponential backoff for Kafka consumers.

#### Sandbox Service
- Use a dedicated `dockerExecutor` for container operations.
- Maintain a **Container Warm Pool** to reduce startup latency; replenish asynchronously.
- Enforce hard timeouts using `CompletableFuture.orTimeout()` (Java 11+).
- Implement a semaphore-based concurrency limit (e.g., max 10 containers) and expose it as a metric.

#### Frontend (React 19)
- Use **Optimistic Updates** (`onMutate`) in React Query where appropriate.
- Leverage React 19 concurrent features (`useTransition`, `useDeferredValue`) for a responsive UI during expensive recalculations.
- Ensure Zustand stores are updated only via `set()` to remain safe in concurrent renders.
- Prevent UI double-submissions by disabling buttons based on React Query's `isPending`/`isLoading` states.

## Package Structure
### Backend (Spring Boot)
Standard Maven structure (`src/main/java/com/...`):
- `controller/`: REST endpoints.
- `service/`: Business logic.
- `repository/`: Data access layers.
- `domain/` / `node/` / `entity/`: Entity models (JPA/Neo4j).
- `dto/`: Data Transfer Objects.
- `config/`: Spring bean/security configurations.
- `filter/`: JWT and security filters.
- `messaging/`: Kafka producers and consumers.

### Frontend (React/Vite)
Modular structure within `src/`:
- `features/`: Feature-based modules (e.g., `auth`, `exam`) containing their own components, hooks, services, and stores.
- `api/`: API client definitions and service calls.
- `components/`: Shared UI components (common, layout, etc.).
- `pages/`: Top-level page components.
- `config/`: Configuration (runtime, page configs).
- `hooks/`: Global custom hooks.
- `context/`: React Context providers.
- `stores/`: Zustand stores for global state.
- `utils/`: Shared utility functions and view models.
- `__tests__/`: Global integration and runtime tests.

## Conventions
- **Naming**:
  - **Java**: `PascalCase` for classes, `camelCase` for variables/methods, `UPPER_SNAKE_CASE` for constants.
  - **React**: `PascalCase` for components and files (`LoginPage.jsx`), `camelCase` for hooks (`useAuth.js`) and utilities.
  - **Database**: `snake_case` for table names and columns.
- **API Design**: Standard RESTful patterns. Error responses follow a consistent format handled by `GlobalExceptionHandler`.
- **Security**: 
  - Never expose secrets or private keys in code. Use environment variables.
  - Use `HttpOnly`, `Secure`, `SameSite=Strict` cookies for authentication.
- **Testing**:
  - Frontend: `vitest` with `@testing-library/react`.
  - Backend: `JUnit 5` with `Mockito`.

## Ignore
Standard ignores as defined in `.gitignore`:
- Dependency folders: `node_modules/`, `target/`.
- Build output: `dist/`, `build/`.
- Environment files: `.env`, `.env.*`.
- Editor-specific: `.vscode/`, `.idea/`, `*.log`.

## Boundaries
- **Auth Boundary**: Source of truth for user identity and session validation. Issues JWTs using RSA keys.
- **Course Boundary**: Manages curriculum, student progress, and enrollment.
- **Exam Boundary**: Manages question banks, adaptive exams, and grading. Integrates with Gemini AI.
- **Sandbox Boundary**: Provides isolated environments for code execution.
- **Frontend Boundary**: UI layer; communicates with specific service backends via configured URLs.

## Production Configuration

### 1. Infrastructure (Docker Compose)
The application is containerized using Docker Compose:
- **Auth Service (`auth`)**: Port `8080`.
- **Exam Service (`exam`)**: Port `8081`.
- **Course Service (`course-service`)**: Port `8083`.
- **Sandbox Service (`sandbox-service`)**: Internal service for code execution.
- **Frontend (`frontend`)**: Port `5173`, served via Nginx.
- **Persistence**: Neo4j (Auth), Redis (Cache), PostgreSQL (Course, Exam, Sandbox).
- **Messaging**: Kafka + Zookeeper for event-driven workflows.
- **Observability**: Zipkin for tracing.

### 2. Essential Environment Variables
For production, ensure the following variables are set in a `.env` file:

| Variable | Description | Production Requirement |
|----------|-------------|------------------------|
| `GOOGLE_CLIENT_ID` | OAuth2 Client ID | Valid Google Cloud Console ID |
| `GOOGLE_CLIENT_SECRET` | OAuth2 Client Secret | Valid Google Cloud Console Secret |
| `JWT_PRIVATE_KEY` | RSA Private Key (PEM) | Base64 encoded private key |
| `JWT_PUBLIC_KEY` | RSA Public Key (PEM) | Base64 encoded public key |
| `NEO4J_PASSWORD` | Neo4j DB Password | Strong unique password |
| `REDIS_PASSWORD` | Redis Auth Password | Strong unique password |
| `POSTGRES_PASSWORD` | PostgreSQL DB Password | Strong unique password |
| `GEMINI_API_KEY` | Google Gemini AI Key | Valid API Key |
| `FRONTEND_URL` | Application base URL | e.g., `https://edubasic.com` |
| `COOKIE_SECURE` | Secure flag for cookies | **Must be set to `true`** |

### 3. Service-Specific Configs

#### Auth Service (`auth`)
- **OAuth2**: Google SSO integration.
- **Security**: JWT signing with RSA key pair.
- **Database**: Neo4j for identity graph.

#### Course Service (`course-service`)
- **Database**: PostgreSQL (`course_db`).
- **Messaging**: Listens for `exam-completed` events on Kafka.

#### Exam Service (`exam`)
- **AI Engine**: Integrates with Gemini AI for adaptive difficulty.
- **Messaging**: Publishes `exam-completed` events to Kafka.

#### Sandbox Service (`sandbox-service`)
- **Isolation**: Uses Docker Java API to manage ephemeral containers.

#### Frontend (`frontend`)
- **Build Args**: `VITE_API_BASE_URL`, `VITE_EXAM_API_BASE_URL`, and `VITE_COURSE_SERVICE_URL` are required.

### 4. Deployment Checklist
1. [ ] Populate `.env` with RSA keys and DB secrets.
2. [ ] Set `COOKIE_SECURE=true`.
3. [ ] Verify all healthchecks in `docker-compose.yml` pass.
4. [ ] Run `docker-compose up -d --build`.
