# LUXORA

Production-oriented e-commerce foundation using a modular monolith architecture.

## Stack

- Backend: Java 21, Spring Boot 4, Maven
- Frontend: Next.js 16, TypeScript
- Data: SQL Server, Redis, Flyway
- Local infra: Docker Compose

## Structure

- `backend/` - Spring Boot application under `com.luxora.commerce`
- `frontend/` - Next.js App Router application
- `docker-compose.yml` - local SQL Server, Redis, backend, and frontend services

## Local Setup

Copy `.env.example` to `.env` and adjust values as needed.
Anonymous carts are stored in Redis with a local development TTL of 24 hours (`CART_TTL=PT24H`).

```bash
docker compose up --build
```

Backend health endpoint:

```bash
curl http://localhost:8080/actuator/health
```
