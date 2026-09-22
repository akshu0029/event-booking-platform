# Event Booking Platform

A secure, backend-focused event ticket booking platform built with Spring Boot. The project is designed around a real distributed-systems problem — preventing ticket overselling under concurrent load — rather than functioning as a standard CRUD application.

## Overview

Users can browse events, reserve tickets, and manage their bookings through both a REST API and a web interface. The system enforces strict seat-inventory consistency using optimistic locking and a Redis-backed temporary hold mechanism, ensuring no two users can claim the same seat simultaneously.

## Core Features

- **Authentication & Authorization** — JWT-based stateless authentication with BCrypt password hashing; role-based access control (`USER`, `ORGANIZER`, `ADMIN`) enforced via method-level security
- **Dual Security Architecture** — a stateless JWT filter chain secures the REST API, while a separate session-based filter chain secures the server-rendered web interface
- **Concurrency-Safe Booking** — optimistic locking (JPA `@Version`) prevents overselling when multiple users book the same event simultaneously
- **Redis-Backed Seat Holds** — reserved seats are held with a time-to-live (TTL) during checkout; a scheduled reconciliation job automatically releases unconfirmed holds and restores inventory
- **Polyglot Persistence** — PostgreSQL for relational data, MongoDB for booking activity logs, and Redis for ephemeral hold state
- **API Documentation** — interactive API reference generated via Swagger/OpenAPI
- **Containerized Deployment** — the full application stack (API, PostgreSQL, MongoDB, Redis) is orchestrated with Docker Compose

## Technology Stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 17, Spring Boot |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA (PostgreSQL), Spring Data MongoDB, Spring Data Redis |
| Frontend | Thymeleaf |
| Documentation | Springdoc OpenAPI |
| Infrastructure | Docker, Docker Compose |

## Architecture Notes

**Seat-hold lifecycle:** upon booking, seat inventory is decremented immediately and the booking is placed in a `HELD` state, paired with a Redis key set on a fixed TTL. If the user confirms within the window, the booking transitions to `CONFIRMED`. Otherwise, a scheduled background task detects the expired hold based on the booking's timestamp, restores the seat count, and marks the booking `EXPIRED`. This design keeps PostgreSQL (the system of record) and Redis (the ephemeral cache) consistent without requiring event-driven messaging infrastructure.

## Getting Started

```bash
git clone https://github.com/akshu0029/event-booking-platform.git
cd event-booking-platform
docker compose up --build
```

Once running:
- Web interface: `http://localhost:8080/web/login`
- API documentation: `http://localhost:8080/swagger-ui/index.html`

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Authenticate and receive a JWT |
| GET | `/api/events` | List upcoming events |
| POST | `/api/events` | Create an event (ORGANIZER/ADMIN) |
| POST | `/api/bookings` | Reserve tickets for an event |
| POST | `/api/bookings/{id}/confirm` | Confirm a held booking |
| GET | `/api/bookings/my` | Retrieve the authenticated user's bookings |

Full request/response schemas are available via the Swagger UI.

## Configuration

Application configuration (database credentials, JWT secret, seat-hold TTL) is provided via environment variables in `docker-compose.yml` for local development. In a production environment, these values should be externalized through a secrets management solution rather than committed to source control.
