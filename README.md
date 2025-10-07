# SynergyFlow

**Enterprise ITSM & PM Platform** - A unified system combining IT Service Management and Project Management capabilities.

## Overview

SynergyFlow is a Spring Boot modulith application with React frontend, designed to provide comprehensive ITSM and PM functionality in a single, scalable platform.

## Quick Start

### Prerequisites

- JDK 21 (Temurin or Oracle)
- Node.js 20+ LTS
- Docker Desktop
- Gradle 8.10+ (wrapper included)

### Backend (Spring Boot)

```bash
cd backend
./gradlew bootRun
```

Backend runs on: http://localhost:8080

### Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on: http://localhost:3000

### Local Development Environment

Start PostgreSQL, Redis, and Keycloak:

```bash
cd infrastructure
docker-compose up -d
```

### Authentication & Authorization

- The backend acts as an OAuth2 Resource Server and validates JWT tokens issued by Keycloak.
- Import the `infrastructure/keycloak/synergyflow-realm.json` realm (contains roles and test users) into your local Keycloak instance.
- Required environment variables (defaults already provided in `backend/src/main/resources/application.yml`):
  - `KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/synergyflow`
  - `KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/synergyflow/protocol/openid-connect/certs`
- Obtain a token and call the secured health endpoint:

```bash
# Request access token using direct grant (test credentials)
TOKEN=$(curl -s -X POST http://localhost:8080/realms/synergyflow/protocol/openid-connect/token \
  -d "client_id=synergyflow-backend" \
  -d "client_secret=synergyflow-secret-change-in-production" \
  -d "grant_type=password" \
  -d "username=admin@test.com" \
  -d "password=admin123" | jq -r .access_token)

# Call secured actuator endpoint (requires valid JWT)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/actuator/health
```

The `/actuator/health` endpoint responds with `{"status":"UP"}` only when a valid Bearer token is supplied.

## Project Structure

```
synergyflow/
├── backend/           # Spring Boot 3.4+ modulith
├── frontend/          # React 18 + Vite application
├── infrastructure/    # Docker Compose and Kubernetes/Helm
├── docs/              # Documentation and specifications
└── testing/           # Integration, E2E, and performance tests
```

## Documentation

Comprehensive documentation is available in the [`docs/`](./docs/) directory:

- [Architecture Documentation](./docs/architecture/) - System architecture and design decisions
- [Epic Technical Specifications](./docs/epics/) - Epic-level technical specifications
- [API Documentation](./docs/api/) - OpenAPI specifications for REST APIs
- [Product Requirements](./docs/product/) - Product Requirements Document (PRD)

## Technology Stack

**Backend:**
- Spring Boot 3.4+
- Spring Modulith 1.2+
- Java 21 LTS
- PostgreSQL 16+
- Redis 7+
- Gradle 8.10+ (Kotlin DSL)

**Frontend:**
- React 18.3+
- TypeScript 5.x
- Vite 5.x
- Ant Design 5.x

## License

See [LICENSE](./LICENSE) file for details.
