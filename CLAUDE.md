# CLAUDE.md

Practical guidance for Claude (or any AI pair-programmer) in this repository.

## Mission

Help contributors ship clean, secure, reviewable backend changes quickly.

## First Steps In A New Task

1. Read `README.md` for project context and run instructions.
2. Inspect `application.yml` and relevant config classes before changing DB/runtime behavior.
3. If DB schema is touched, check existing Flyway migrations first.
4. Keep scope tight to the user request.

## Non-Negotiables

- Never expose credentials or commit secrets.
- `.env` is local-only; use `.env.example` for templates.
- Keep this repo public-safe by default.
- Do not break `mvn clean verify`.
- For schema changes, add new migration(s), do not mutate history unless asked.

## Architecture Notes

- Spring Boot app with layered structure.
- Current codebase emphasizes:
  - entities
  - repositories
  - config/bootstrap behavior
- Controllers/services are intentionally sparse and expected to grow.

## How To Implement Features Cleanly

For API features:

1. Define request/response DTOs.
2. Add/extend service methods for business logic.
3. Add controller endpoints.
4. Add repository methods only when needed.
5. Add validation and robust error handling.
6. Add tests.
7. Run `mvn clean verify`.

## Change Hygiene

- Keep diffs focused and easy to review.
- Avoid opportunistic rewrites unrelated to the task.
- Update docs when behavior/config/commands change.
- Preserve naming consistency with existing packages/classes.

## Data + SQL Guidance

- Keep SQL explicit and readable.
- Include constraints/indexes deliberately.
- Ensure MySQL and H2 migration intent stays aligned.
- Respect enum/string mappings used by JPA entities.

## Debugging Guidance

- Prefer root-cause fixes over temporary patches.
- Use startup logs and config post-processor output for env/datasource issues.
- Reproduce failures with the smallest reliable command (`mvn test` or a focused test class).

## Definition Of Done

- Requested behavior is implemented.
- Tests pass (`mvn clean verify`).
- No secrets leaked.
- Docs/examples updated if needed.
- Changes are ready for public GitHub visibility.

## Handy Commands

- Run app: `mvn spring-boot:run`
- Full verification: `mvn clean verify`
- Build artifact: `mvn package -DskipTests`
