# CSCI 201 — Study Room App

Full-stack app for USC study room reservations, student matching, and study-group collaboration.

- **Backend**: Spring Boot + Spring Data JPA + MySQL + Flyway
- **Frontend**: React 19 + TypeScript + Vite (in `frontend/`)

---

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| **JDK 21** | `java --version` should show 21. macOS: `brew install openjdk@21` |
| **Apache Maven 3.9+** | `mvn -version`. macOS: `brew install maven` |
| **Node.js 18+** | `node -v`. macOS: `brew install node` |
| **MySQL** | Running locally or via Docker for the backend runtime |

---

## Quick start

### 1 — Backend

```bash
# from repo root
cp .env.example .env        # fill in DB_USER, DB_PASSWORD, etc.
mvn spring-boot:run
```

Backend listens on **http://localhost:8080**. Flyway auto-creates tables on first run.

### 2 — Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend listens on **http://localhost:5173** and proxies API calls to `http://localhost:8080` via the `VITE_API_BASE_URL` env var (already set in `frontend/.env`).

---

## Environment variables

### Backend — `.env` (project root)

Gitignored. Copy from `.env.example` and fill in values:

```bash
cp .env.example .env
```

Key variables (matched to `application.yml`):

| Variable | Default | Purpose |
|----------|---------|---------|
| `MYSQL_HOST` | `localhost` | MySQL host |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_DATABASE` | `csci201` | Database name |
| `DB_USER` | `root` | MySQL user |
| `DB_PASSWORD` | _(empty)_ | MySQL password |
| `SPRING_PROFILES_ACTIVE` | _(none)_ | Set to `h2` for in-memory mode |

On startup, `DotenvEnvironmentPostProcessor` loads `.env` from the working directory. Startup logs print `dotenv:` (raw values) and `resolved:` (merged datasource config) for debugging. Set `DOTENV_LOG_SECRETS=true` to unmask passwords temporarily.

### Frontend — `frontend/.env`

Tracked in git (no secrets). One variable:

```
VITE_API_BASE_URL=http://localhost:8080
```

Change this if the backend runs on a different host or port.

---

## Auth flow

The app uses a simple username/password scheme — no JWTs. After login/signup/guest, the backend returns a user object that the frontend stores in `localStorage` (`"session_user"` key) and React context. All protected routes (`/rooms`, `/social`) redirect to `/login` if no session is found.

**Guest** users can browse rooms but cannot book, rate, or join a waitlist.

---

## Repository layout

```
csci201-backend/
├── pom.xml
├── README.md
├── .env.example              # backend env template (copy → .env)
├── frontend/                 # React + TypeScript frontend
│   ├── .env                  # VITE_API_BASE_URL (tracked, no secrets)
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── App.tsx            # router + AuthProvider
│       ├── api/               # typed fetch modules (auth, rooms, reservations, etc.)
│       ├── context/           # AuthContext (session, login, logout)
│       ├── components/        # NavBar, ProtectedRoute, BuildingAvailabilityMap
│       ├── layouts/           # MainLayout (NavBar + Outlet)
│       ├── pages/             # LoginPage, RoomBrowsePage, SocialPage
│       ├── types/             # room.ts TypeScript types
│       └── utils/             # timeAgo helper
└── src/
    ├── main/
    │   ├── java/com/csci201/backend/
    │   │   ├── Csci201BackendApplication.java
    │   │   ├── config/        # SecurityConfig (CORS), OpenApiConfig, DotenvPostProcessor
    │   │   ├── controller/    # REST controllers
    │   │   ├── service/       # business logic
    │   │   ├── repository/    # JpaRepository interfaces
    │   │   ├── entity/        # JPA entities + enums
    │   │   ├── dto/           # request/response types
    │   │   └── exception/     # GlobalExceptionHandler
    │   └── resources/
    │       ├── application.yml
    │       └── flyway/        # SQL migrations (mysql/ and h2/)
    └── test/
```

---

## API endpoints

### Auth — `/auth`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/signup` | Create account (`userName`, `password`, `firstName`, `lastName`) |
| POST | `/auth/login` | Login (`userName`, `password`) |
| POST | `/auth/guest` | Anonymous guest session |

### Rooms — `/api/rooms`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/rooms` | List all rooms |
| GET | `/api/rooms/{id}` | Get single room |

### Reservations — `/api/reservations`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/reservations` | Book a room (`userId`, `roomId`, `startTime`, `endTime`) |
| DELETE | `/api/reservations/{id}` | Cancel reservation (promotes next waitlist user) |
| GET | `/api/reservations?userId=X` | User's reservations |

### Waitlist — `/api/waitlist`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/waitlist` | Join waitlist (`userId`, `roomId`, `requestedTimeSlot`) |
| DELETE | `/api/waitlist/{id}` | Leave waitlist |
| GET | `/api/waitlist?userId=X` | User's waitlist entries |
| GET | `/api/waitlist/rooms/{roomId}?requestedTimeSlot=...` | Waitlist for a room/slot |

### Reviews — `/api/reviews`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/reviews` | Submit a review (`userId`, `roomId`, `rating 1–5`, `comment`). Updates room's `averageRating` and `ratingsCount`. |
| GET | `/api/reviews?roomId=X` | Reviews for a room (newest first) |

### Matching — `/matches`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/matches/suggestions?userId=X&limit=N` | Top N partner suggestions by compatibility score |
| GET | `/matches/search?userId=X&course=...&minScore=...` | Filtered/paginated partner search |

### Study Groups — `/study-groups`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/study-groups` | Create group |
| GET | `/study-groups?userId=X` | Groups the user belongs to |
| GET | `/study-groups/{groupId}` | Group detail + members |
| POST | `/study-groups/{groupId}/join` | Join a PUBLIC group |
| POST | `/study-groups/{groupId}/invites` | Invite a user |
| GET | `/study-groups/invites?userId=X` | Pending invitations for a user |
| POST | `/study-groups/invites/{inviteId}/accept` | Accept invitation |
| POST | `/study-groups/invites/{inviteId}/decline` | Decline invitation |

### Group Reservations — `/group-reservations`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/group-reservations` | Book a room for a study group |

### Courses — `/api/users/{userId}/courses`

| Method | Path | Description |
|--------|------|-------------|
| PUT | `/api/users/{userId}/courses` | Replace course list |
| GET | `/api/users/{userId}/courses` | Get course list |

---

## Database schema

- **ORM**: JPA entities under `entity/`; Hibernate validates schema (`ddl-auto: validate`).
- **Migrations**: Flyway runs automatically on startup before Hibernate.
  - Default (MySQL): `flyway/mysql/`
  - Tests / H2 profile: `flyway/h2/`

Add new migrations as `V5__...`, `V6__...` etc. Never mutate existing scripts.

### Tables

| Table | PK | Notes |
|-------|----|-------|
| `users` | `user_id` | Unique `user_name`; stores courses as JSON string |
| `rooms` | `room_id` | Unique `(building_name, room_number)`; tracks `average_rating`, `ratings_count` |
| `reservations` | `reservation_id` | Status: `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED` |
| `waitlist` | `waitlist_id` | Unique `(user_id, room_id, requested_time_slot)`; ordered by `queue_position` |
| `reviews` | `review_id` | Single `rating` (1–5); `comment` may encode sub-ratings as `noise:N,cleanliness:N\|text` |
| `user_matches` | `match_id` | Cached compatibility scores; unique `(user_id, matched_user_id)` |
| `study_groups` | `group_id` | Visibility: `PUBLIC`, `INVITE_ONLY` |
| `study_group_members` | `group_member_id` | Role: `OWNER`, `MEMBER`; unique `(group_id, user_id)` |
| `study_group_invitations` | `invitation_id` | Status: `PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED` |
| `group_reservations` | `group_reservation_id` | Links a reservation to a study group |

---

## Build and test

```bash
# backend unit + integration tests (H2, no MySQL needed)
mvn clean verify

# build JAR
mvn package -DskipTests
java -jar target/csci201-backend-0.0.1-SNAPSHOT.jar

# frontend type check
cd frontend && npx tsc --noEmit

# frontend production build
cd frontend && npm run build
```

---

## OpenAPI / Swagger

After `mvn spring-boot:run`:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Port 8080 in use | Change `server.port` in `application.yml` |
| Java version mismatch | Align `JAVA_HOME` and `mvn -version` with JDK 21 |
| Maven can't resolve artifacts | `mvn -U clean verify` |
| Frontend can't reach backend | Check `VITE_API_BASE_URL` in `frontend/.env` and that CORS allows your origin |
| "Access denied" on MySQL | Check `DB_USER`/`DB_PASSWORD` in `.env`; set `DOTENV_LOG_SECRETS=true` to debug |
| Login/signup fails | Backend must be running; check browser console for CORS errors |

---

## AI collaboration guides

- **`AGENTS.md`**: repo-specific workflows, guardrails, and prompt templates
- **`CLAUDE.md`**: practical implementation guidance for AI pair-programming sessions

---

## License

Educational use for CSCI 201. Add your own license if you redistribute.
