# TripBook

Flight & hotel booking platform. Built to explore the engineering problems real
booking systems face — concurrent seat/room inventory, caching, horizontal
scaling, and event-driven side effects.

> Status: **Phase 0 — scaffold.** Full documentation lands in Phase 14. See
> [`PLAN.md`](PLAN.md) for the phased build plan.

## Stack

| Layer | Choice |
|---|---|
| Backend | Java 17, Spring Boot 3.5.x, Maven |
| Persistence | Spring Data JPA, Flyway migrations |
| Database | PostgreSQL 16 |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind, Framer Motion |
| Containers | Docker Compose |

Later phases add: Spring Security + JWT, Redis cache, nginx load balancer with
2 backend instances, and Kafka notifications.

## Repository layout

```
tripbook/
  backend/      Spring Boot 3 app (Java 17, Maven)
  frontend/     Next.js app (scaffolded in Phase 9; design assets already in public/)
  infra/        docker-compose.yml + nginx/
  docs/         architecture & captured evidence
  PLAN.md       phased build plan
```

## Run locally (Phase 0)

Requires Docker, and a JDK 17 on PATH to run the backend.

```bash
# 1. start Postgres
cd infra && docker compose up -d

# 2. run the backend (from repo root)
cd ../backend && ./mvnw spring-boot:run

# 3. health check
curl -i http://localhost:8080/api/health   # -> {"status":"UP"}
```
