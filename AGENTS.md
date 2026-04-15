# AGENTS.md

This file helps AI coding agents (and humans) work effectively in this repository.

## Project Snapshot

- **Stack:** Spring Boot 3, Java 21, Maven, Spring Data JPA, Flyway
- **Default runtime DB:** MySQL
- **Test DB:** in-memory H2
- **Current state:** backend skeleton with entities/repositories, no REST controllers yet

## Quick Start

1. Copy env template:
   - `cp .env.example .env`
2. Fill local credentials in `.env` (never commit this file).
3. Run app:
   - `mvn spring-boot:run`
4. Run tests:
   - `mvn clean verify`

## Safe Defaults For Agents

- Treat this repo as **public-facing**: never hardcode credentials.
- Do not commit `.env` or any secrets.
- Prefer editing `.env.example` when documenting new env vars.
- Keep schema changes in **new Flyway migration files** (`V2__...sql`, `V3__...sql`, etc.), never by changing old migrations unless explicitly requested.
- Keep changes small and focused.

## Where Things Live

- App entrypoint: `src/main/java/com/csci201/backend/Csci201BackendApplication.java`
- Config classes: `src/main/java/com/csci201/backend/config/`
- Entities: `src/main/java/com/csci201/backend/entity/`
- Repositories: `src/main/java/com/csci201/backend/repository/`
- Runtime config: `src/main/resources/application.yml`
- Flyway migrations:
  - MySQL: `src/main/resources/flyway/mysql/`
  - H2: `src/main/resources/flyway/h2/`
- Tests: `src/test/java/com/csci201/backend/`

## Recommended Development Flow

When implementing a feature:

1. Add/adjust DTOs/entities if needed.
2. Add repository query methods.
3. Add service-layer business logic.
4. Add controller endpoints.
5. Add validation + error handling.
6. Add/extend tests.
7. Run `mvn clean verify`.

## Coding Conventions

- Use constructor injection for Spring beans.
- Keep controllers thin; business logic in services.
- Validate request DTOs with `jakarta.validation`.
- Return clear HTTP status codes and error messages.
- Prefer explicit naming (`ReservationService`, `RoomController`, etc.).
- Avoid broad refactors unless requested.

## Database + Migration Rules

- Hibernate is configured for validation, not schema generation.
- Every schema evolution must be a new Flyway migration.
- Keep MySQL and H2 migration sets semantically aligned.
- Use idempotent, reviewable SQL with clear names.

## Testing Expectations

- At minimum, ensure `mvn clean verify` passes.
- For new endpoint logic, prefer:
  - unit tests for services
  - integration tests for repository/controller behavior when appropriate
- Keep tests deterministic; avoid relying on local MySQL for CI paths.

## Common Commands

- Build + test: `mvn clean verify`
- Run app: `mvn spring-boot:run`
- Package jar: `mvn package -DskipTests`
- Run jar: `java -jar target/csci201-backend-0.0.1-SNAPSHOT.jar`

## PR / Change Checklist

- [ ] No secrets introduced
- [ ] New env vars documented in `.env.example` and `README.md` if needed
- [ ] Flyway migration added for schema changes
- [ ] Tests updated and passing
- [ ] Docs updated for behavior/config changes

## Good Prompt Templates For Agents

- "Add CRUD endpoints for `Room` with validation and service layer, plus tests."
- "Add a Flyway migration for <schema change> and update entities accordingly."
- "Implement <feature> without changing unrelated files; run `mvn clean verify`."
- "Review this branch for public-repo safety (secrets, accidental local config, docs gaps)."
