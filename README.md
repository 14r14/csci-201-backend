# CSCI 201 Backend

Spring Boot backend with REST APIs for room reservations, student matching, and study-group collaboration: **Spring Data JPA** with **Hibernate** as the ORM, Bean Validation, and **MySQL by default** so schema and data live on a real server (visible in DBeaver, MySQL Workbench, etc.). **`mvn test`** uses in-memory **H2** only.

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| **JDK 21** | This project targets Java 21. Check with `java --version` (or `java -version`). |
| **Apache Maven 3.9+** | `mvn -version` should work. |

Example install on macOS:

- **Homebrew**: `brew install openjdk@21` and `brew install maven`
- **SDKMAN**: `sdk install java 21.0.x-tem` then `sdk install maven`

Ensure Maven uses JDK 21 (same vendor as `java --version` if you use multiple JDKs):

```bash
java --version
mvn -version   # should list Java version 21 for the build
```

## Quick start

From the project root:

```bash
mvn spring-boot:run
```

The embedded server listens on **http://localhost:8080**.

## Environment variables (`.env`)

Secrets and local DB settings should live in a **`.env`** file in the project root. That file is **gitignored**; collaborators start from **`.env.example`** (tracked in git):

```bash
cp .env.example .env
# Edit .env with your real DB_USER, DB_PASSWORD, SPRING_PROFILES_ACTIVE, etc.
```

At startup, `DotenvEnvironmentPostProcessor` loads `.env` into the Spring environment (from the **process working directory**, so run Maven from the repo root). Variable names match `application.yml` (for example `DB_USER`, `MYSQL_HOST`).

`mvn test` / `mvn verify` run with **`-Dspring.dotenv.skip=true`**, and **`Csci201BackendApplicationTests`** uses **`@ActiveProfiles("h2")`** so tests use in-memory H2 and do not need MySQL.

**Debugging `.env` / MySQL “Access denied”:** On startup (before Flyway connects), logs include `dotenv:` (raw values from `.env`) and `resolved:` (merged `spring.datasource.*`). Passwords are masked by default. To log the full password temporarily, set **`DOTENV_LOG_SECRETS=true`** in the environment or **`-Ddotenv.log.secrets=true`** on the JVM — remove afterward.

## AI collaboration guides

For AI-assisted development in this repo, see:

- **`AGENTS.md`**: repo-specific workflows, guardrails, and prompt templates
- **`CLAUDE.md`**: practical implementation guidance for AI pair-programming sessions

## OpenAPI / Swagger UI

[SpringDoc OpenAPI](https://springdoc.org/) exposes the currently available controllers and DTO schemas.

After `mvn spring-boot:run`:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

Paths are set in `application.yml` under `springdoc.*`.

## MySQL (default runtime)

Create an empty database (for example `csci201`), put credentials in **`.env`** (see **`.env.example`**), then `mvn spring-boot:run`. Flyway creates tables in that database; connect your client to the same host, port, and database name to browse schema and rows.

Optional **`SPRING_PROFILES_ACTIVE=h2`** runs the app against in-memory H2 when you cannot reach MySQL (data is not visible in a MySQL client).

## Repository layout

```
csci201-backend/
├── pom.xml
├── README.md
├── .gitignore
├── .env.example          # template; copy to `.env` (gitignored)
└── src/
    ├── main/
    │   ├── java/com/csci201/backend/
    │   │   ├── Csci201BackendApplication.java   # entry point
    │   │   ├── config/
    │   │   │   └── OpenApiConfig.java           # OpenAPI title/description
    │   │   ├── controller/                      # REST controllers
    │   │   ├── service/                         # business logic
    │   │   ├── repository/                      # JpaRepository interfaces
    │   │   ├── entity/                          # JPA entities + enums
    │   │   ├── dto/                             # request/response types
    │   │   └── exception/                       # error handling / exception mapping
    │   └── resources/
    │       ├── application.yml                  # datasource, Flyway, JPA, profiles
    │       └── flyway/                          # Flyway SQL (h2 / mysql)
    └── test/java/com/csci201/backend/
        └── Csci201BackendApplicationTests.java
```

## Database schema & migrations

- **ORM**: JPA entities under `entity/` map to tables; **Hibernate** validates the schema against the database (`spring.jpa.hibernate.ddl-auto: validate`).
- **Migrations**: **Flyway** runs **before** Hibernate.
  - **Default (MySQL)**: `flyway/mysql/V1__initial_schema.sql`
  - **Profile `h2`** (tests / optional local): `flyway/h2/V1__initial_schema.sql`

Add new files as incrementing versions (`V3__...`, `V4__...`) in the same folder as the active profile.

### How to run the initial migration

You **do not** run Flyway as a separate manual step in normal development. **Flyway runs automatically** when the Spring Boot application starts: it connects to the datasource, applies any pending scripts from `spring.flyway.locations` (for example `V1__initial_schema.sql`), then Hibernate starts with `ddl-auto: validate`.

**MySQL (default)** — create the database, configure **`.env`**, then from the repo root:

```bash
mvn spring-boot:run
```

You should see Flyway migrate to version `1` and then `Started Csci201BackendApplication`. If MySQL is down or credentials are wrong, startup fails.

**In-memory H2** — only when you set `SPRING_PROFILES_ACTIVE=h2` (tests do this automatically); tables are not on MySQL.

### Tables (logical model)

| Table | PK | Notes |
|-------|-----|--------|
| `users` | `user_id` | Unique `user_name`. |
| `rooms` | `room_id` | Unique `(building_name, room_number)`. |
| `reservations` | `reservation_id` | FKs → `users`, `rooms`. |
| `waitlist` | `waitlist_id` | FKs → `users`, `rooms`. Unique `(user_id, room_id, requested_time_slot)`. |
| `reviews` | `review_id` | FKs → `users`, `rooms`. |
| `user_matches` | `match_id` | FKs `user_id`, `matched_user_id` → `users`. Includes cached match score timestamp and unique `(user_id, matched_user_id)`. |
| `study_groups` | `group_id` | Group metadata, visibility (`PUBLIC`, `INVITE_ONLY`), owner FK to `users`. |
| `study_group_members` | `group_member_id` | Group membership + role (`OWNER`, `MEMBER`), unique `(group_id, user_id)`. |
| `study_group_invitations` | `invitation_id` | Invite lifecycle (`PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED`). |
| `group_reservations` | `group_reservation_id` | Links a reservation to a study group and booking member. |

The Java entity for matches is **`UserMatch`** (table `user_matches`) to avoid clashing with `java.lang.Match` and reserved SQL keywords.

**Enums** (stored as strings): `UserRole`, `ReservationStatus`, `RoomCurrentStatus`, `GroupVisibility`, `GroupMemberRole`, `GroupInvitationStatus`.

## API endpoints

- **Reservations**
  - `POST /reservations`
- **Matching**
  - `GET /matches/suggestions?userId=...&limit=...`
  - `GET /matches/search?userId=...&course=...&interest=...&minScore=...`
- **Study Groups**
  - `POST /study-groups`
  - `GET /study-groups?userId=...`
  - `GET /study-groups/{groupId}`
  - `POST /study-groups/{groupId}/join`
  - `POST /study-groups/{groupId}/invites`
  - `POST /study-groups/invites/{inviteId}/accept`
  - `POST /study-groups/invites/{inviteId}/decline`
- **Group Reservations**
  - `POST /group-reservations`

## Dependencies (`pom.xml`)

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | Servlet stack, Spring MVC, JSON for REST when you add controllers. |
| `spring-boot-starter-data-jpa` | JPA + Hibernate (ORM), `JpaRepository`, transactions. |
| `spring-boot-starter-validation` | `jakarta.validation` on DTOs. |
| `dotenv-java` | Parses `.env` in the project root via `DotenvEnvironmentPostProcessor`. |
| `mysql-connector-j` | JDBC driver (default runtime database). |
| `h2` | In-memory DB for **tests** only (`test` scope). |
| `flyway-core`, `flyway-mysql` | Flyway + MySQL support; H2 uses Flyway’s built-in H2 integration in tests. |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI 3 + Swagger UI (empty spec until controllers exist). |
| `spring-boot-starter-test` | JUnit 5, Spring Test (test scope). |

## Configuration (`application.yml`)

- **Default**: **MySQL** — JDBC URL from `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`; credentials `DB_USER` / `DB_PASSWORD`. Flyway `classpath:flyway/mysql`, `ddl-auto: validate`.
- **Profile `h2`**: In-memory H2 + `classpath:flyway/h2` (used by tests via `@ActiveProfiles("h2")`; optional for local runs without MySQL).

Run the app (MySQL must exist and accept the connection):

```bash
# credentials often come from .env
mvn spring-boot:run
```

Schema changes belong in **new Flyway scripts**; Hibernate only **validates** that entities still match the database.

## Build and test

```bash
mvn clean verify
```

Package and run the JAR:

```bash
mvn package -DskipTests
java -jar target/csci201-backend-0.0.1-SNAPSHOT.jar
```

## Security note

No authentication is configured. For real deployments, add Spring Security and manage database secrets properly.

## Troubleshooting

- **Port 8080 in use**: Change `server.port` in `application.yml`.
- **Java version mismatch**: Align `JAVA_HOME` and `mvn -version` with JDK 21.
- **Maven cannot resolve artifacts**: Check network; try `mvn -U clean verify`.

## License

Educational use for CSCI 201. Add your own license if you redistribute.
