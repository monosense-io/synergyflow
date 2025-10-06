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
