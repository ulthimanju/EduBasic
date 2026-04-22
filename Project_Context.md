# Project Context: EduBasic

## Project Overview
EduBasic is a multi-service educational platform designed to provide an adaptive learning and testing experience. It features secure OAuth2 authentication, a dynamic exam engine with AI-driven difficulty adjustment (via Gemini AI), and a modern React frontend. The project is fully containerized for seamless development and deployment.

## Architecture
The system follows a microservices-inspired architecture using Docker Compose for orchestration:
- **Service-Oriented**: Decoupled backend services for `auth` and `exam`.
- **Database per Service**:
  - `auth` uses **Neo4j** (Graph) for complex user relationships/sessions and **Redis** for caching.
  - `exam` uses **PostgreSQL** (Relational) for structured exam content and results.
- **Stateless Auth**: JWT-based authentication via HttpOnly cookies.
- **Communication**: Frontend communicates with services via REST APIs. Interservice communication (if any) is minimal/direct.

## Stack
- **Frontend**: React 19, Vite, Zustand (State Management), React Router 7, Axios, Lucide React, Vitest.
- **Backend (Java)**: Spring Boot 3.x, Spring Security (OAuth2/JWT), Spring Data JPA/Neo4j/Redis, Lombok, Flyway (Migrations).
- **AI**: Google Gemini AI (`gemini-flash-latest`).
- **Databases**: Neo4j 5.x, Redis 7.x, PostgreSQL 16.
- **Infrastructure**: Docker, Docker Compose, Nginx (Frontend server).

## Package Structure
### Backend (Spring Boot)
Standard Maven structure (`src/main/java/com/app/...`):
- `controller/`: REST endpoints.
- `service/`: Business logic.
- `repository/`: Data access layers.
- `domain/` / `node/`: Entity models (JPA/Neo4j).
- `dto/`: Data Transfer Objects.
- `config/`: Spring bean/security configurations.
- `filter/`: JWT and security filters.

### Frontend (React/Vite)
Modular structure within `src/`:
- `features/`: Feature-based modules (e.g., `auth`) containing their own components, hooks, services, and stores.
- `components/`: Shared UI (`ui/`) and Layout (`layout/`) components.
- `pages/`: Top-level page components.
- `services/`: Global API clients.
- `hooks/`: Global custom hooks.
- `context/`: React Context providers (e.g., `PromptContext`).
- `utils/`: Shared utility functions and view models.
- `__tests__/`: Global integration and runtime tests.

## Conventions
- **Naming**:
  - **Java**: `PascalCase` for classes, `camelCase` for variables/methods, `UPPER_SNAKE_CASE` for constants.
  - **React**: `PascalCase` for components and files (`LoginPage.jsx`), `camelCase` for hooks (`useAuth.js`) and utilities.
  - **Database**: `snake_case` for table names and columns.
- **API Design**: Standard RESTful patterns. Error responses follow a consistent format handled by `GlobalExceptionHandler`.
- **Security**: 
  - Never expose `JWT_SECRET` or API keys in code.
  - Use `HttpOnly`, `Secure`, `SameSite=Strict` cookies for production.
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
- **Auth Boundary**: The `auth` service is the source of truth for user identity and session validation.
- **Exam Boundary**: The `exam` service manages course content, question banks, and result calculations. It depends on `JWT` issued by `auth` for authorization.
- **Frontend Boundary**: Serves as the UI layer; it must not contain sensitive business logic or direct database credentials.

## Production Configuration

### 1. Infrastructure (Docker Compose)
The application is containerized using Docker Compose. The orchestration includes:
- **Auth Service (`auth`)**: Port `8080`, Spring Boot.
- **Exam Service (`exam`)**: Port `8081`, Spring Boot.
- **Frontend (`frontend`)**: Port `5173`, React/Vite served via Nginx.
- **Neo4j**: Graph database for user and session management.
- **Redis**: Cache and session storage.
- **PostgreSQL**: Relational database for exam data and results.

### 2. Essential Environment Variables
For production, ensure the following variables are set in a `.env` file (based on `.env.example`):

| Variable | Description | Production Requirement |
|----------|-------------|------------------------|
| `GOOGLE_CLIENT_ID` | OAuth2 Client ID | Valid Google Cloud Console ID |
| `GOOGLE_CLIENT_SECRET` | OAuth2 Client Secret | Valid Google Cloud Console Secret |
| `JWT_SECRET` | Secret for signing tokens | High-entropy random string |
| `NEO4J_PASSWORD` | Neo4j DB Password | Strong unique password |
| `REDIS_PASSWORD` | Redis Auth Password | Strong unique password |
| `POSTGRES_PASSWORD` | PostgreSQL DB Password | Strong unique password |
| `GEMINI_API_KEY` | Google Gemini AI Key | Valid API Key for Adaptive Engine |
| `FRONTEND_URL` | Application base URL | e.g., `https://edubasic.com` |
| `COOKIE_SECURE` | Secure flag for cookies | **Must be set to `true`** |

### 3. Service-Specific Configs

#### Auth Service (`auth/src/main/resources/application.yml`)
- **OAuth2**: Configured for Google SSO.
- **Cache**: Uses Redis with connection pooling.
- **Security**: HttpOnly cookies with `Strict` SameSite policy.

#### Exam Service (`exam/src/main/resources/application.yml`)
- **Database**: PostgreSQL with Flyway migrations.
- **Adaptive Engine**: Integrates with Gemini AI (`gemini-flash-latest`).
- **Persistence**: Validates schema on startup (`ddl-auto: validate`).

#### Frontend (`frontend/Dockerfile`)
- **Build Args**: `VITE_API_BASE_URL` and `VITE_EXAM_API_BASE_URL` are required at build time to point to production endpoints.

### 4. Deployment Checklist
1. [ ] Copy `.env.example` to `.env` and populate secrets.
2. [ ] Set `COOKIE_SECURE=true`.
3. [ ] Verify `FRONTEND_URL` matches the production domain.
4. [ ] Run `docker-compose up -d --build` to deploy.
