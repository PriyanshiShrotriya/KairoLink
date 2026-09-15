#History of work done
#update it regularly

- 2026-08-20 (a): Confirmed tech stack stays Java 17 + Spring Boot + Spring MVC + JSP/JSTL (no change at that point — this was already locked in). Updated `architecture.md`, `rules.md`, and `README.md` to reflect that the project will be vibe-coded in **VS Code using GitHub Copilot / Copilot Chat** instead of IntelliJ: added recommended VS Code extensions, a Copilot-specific workflow/guardrails section in `rules.md` (context-referencing docs, being extra cautious with inline ghost-text suggestions, reviewing layer-by-layer), and VS Code run instructions in `README.md`.

- 2026-08-20 (b): **Switched the view layer from JSP/JSTL to Thymeleaf, and packaging from WAR to JAR**, since the goal is now a real, deployable project rather than a JSP/Servlet learning exercise. Changed across all docs:
  - `architecture.md` — view layer, packaging, folder structure (`src/main/resources/templates/` + `static/` replacing `webapp/WEB-INF/views/`), tech stack tables, added a "Note on Thymeleaf vs JSP" section explaining the rationale, added Docker/Render/Railway/Fly.io as deployment options.
  - `rules.md` — locked stack now says Thymeleaf (not JSP), flipped the "no Thymeleaf" restriction to "no JSP", updated Copilot guardrails to flag JSP/JSTL drift instead.
  - `README.md` — tech stack table, architecture diagram, project structure, prerequisites (dropped hard Apache Tomcat requirement), run instructions (`java -jar ...`), added a "Deploying" section.
  - `phases.md` — Phase 0 (Thymeleaf template + DevTools setup instead of JSP config), Phase 6 (templates instead of JSPs), Phase 8 (JAR + Docker + Render/Railway/Fly.io deployment instead of WAR + external Tomcat).
  - `design.md` — replaced JSP references with Thymeleaf in usage-principle notes and font-loading instructions.
  - `requirements.md` — constraints section updated to say Thymeleaf instead of JSP/Servlets.

- 2026-08-20 (c): **Bumped Spring Boot from 3.x to 4.1.x** (on Spring Framework 7.0.x), since Spring Boot 3.5 reached open-source EOL on June 30, 2026, and Spring Initializr no longer offers 3.x. Updated `architecture.md` (tech stack table + new "A Note on Spring Boot 4" section covering Jackson 3, JUnit 5-only, dropped Undertow, Java 17 baseline retained) and `rules.md` (locked stack version). No other doc pinned a Spring Boot version, so no further changes needed. Thymeleaf/JPA/Security/Lombok choices are unaffected — all have Boot-4-compatible versions.

- 2026-08-20 (d): **Completed Phase - 0 ** , successfully established project foundation along with project structure. Presentation layer , logic and database layer are all in sync.

- 2026-09-08 (e): ## Current Branch
`feature/user-registration`

## Completed: User Registration

The complete user registration feature is implemented and tested.

### Backend
- Created `User` entity and `Role` enum.
- Added `UserRepository`.
- Added Flyway database migration for `users` and `user_roles`.
- Configured PostgreSQL/Supabase with Flyway.
- Changed Hibernate schema handling to `ddl-auto=validate`.
- Added BCrypt password hashing using `PasswordEncoder`.
- Created `RegistrationRequest` DTO.
- Created `UserRegistrationService`.
- Added validation for:
  - Password confirmation
  - Minimum password length of 8 characters
  - BCrypt 72-byte password limit
  - Duplicate email
  - Public role restrictions
- Only `RIDER` and `DRIVER` can register publicly.
- `ADMIN` registration is rejected.
- Email is normalized using trim + lowercase.
- Phone is trimmed; empty/blank values become `null`.
- Database unique constraint remains the final protection against concurrent duplicate registrations.
- Added `DuplicateEmailException` and `InvalidRegistrationException`.
- Registration runs inside a transaction.

### Controller
- Created `AuthController`.
- Implemented:
  - `GET /register`
  - `POST /register`
- Uses `@Valid` and `BindingResult`.
- Handles duplicate email and invalid registration errors.
- Clears password fields when returning the form after errors.
- Successful registration redirects to `/register?success`.

### Frontend
- Created `templates/auth/register.html`.
- Created `static/css/auth.css`.
- Registration page uses a responsive split-layout inspired by the Figma design.
- Includes validation errors and success feedback.
- Only RIDER and DRIVER roles are shown.
- Password values are never redisplayed.

### Testing
- Added mock-based `UserRegistrationServiceTest`.
- Added `AuthControllerTest`.
- Added one controlled `RegistrationEndToEndTest`.
- Added test-scoped H2 database configuration.
- Tests use an isolated H2 in-memory database and do not contact Supabase.
- Full test suite last verified successfully:

  **21 tests passed, 0 failures, 0 errors.**

## Important Architecture Decisions
- Keep code production-ready but avoid unnecessary abstractions.
- Do not add interfaces, factories, mappers, or helpers unless genuinely needed.
- Use layered architecture: Controller → Service → Repository → Database.
- Flyway manages database schema changes.
- Hibernate uses schema validation, not automatic schema updates.
- Production database is PostgreSQL/Supabase.
- Tests must use isolated infrastructure and must not modify production data.

## Next Task
User registration is complete.

Next major Phase 1 task: **Login / Authentication**.

Before implementing login:
1. Read `architecture.md`.
2. Read `phases.md`.
3. Read `requirements.md`.
4. Read `rules.md`.
5. Inspect the current authentication-related code and dependencies.

Do not assume whether authentication should use JWT or Spring Security sessions until the project requirements and architecture are checked.

## Development Preference
The project should be built as a real-world deployable application:
- Not unnecessarily bulky.
- Not oversimplified.
- Changes should stay within the approved scope.
- Inspect existing code before modifying files.
- Prefer focused implementation plans.
- Run appropriate tests after implementation.

- 2026-09-16 (f): **Completed Phase 1 Steps 1–6 and verified Phase 1**
  - Step 1: Spring Security foundation with session-based authentication, public routes, protected routes, CSRF, and deliberate logout behavior.
  - Step 2: Database-backed authentication using normalized email lookup, BCrypt hashes, enabled-state checks, and RIDER/DRIVER/ADMIN authority mapping.
  - Step 3: Custom login page, successful and failed login behavior, logout flow, and authentication-aware navigation.
  - Step 4: Role-based Rider, Driver, and Admin dashboard shells with cross-role authorization.
  - Step 5: Authenticated profile view/edit for name, phone, and profile photo reference. Email remains read-only and security-sensitive fields are preserved.
  - Step 6: Driver-only one-vehicle management with add/view/edit flows, a unique vehicle-per-driver constraint, server/database validation, and text-only photo references.
  - Step 7: Password reset is intentionally deferred because it is optional in Phase 1. Email OTP verification and email delivery are also deferred.
  - Step 8: Final verification completed on 2026-09-16. The full Maven test suite passed, `git diff --check` passed, Flyway migrations V1 and V2 validated against the isolated H2 test database, and no production database configuration was used by tests.

### Phase 1 Status
- Completed: registration, session authentication, database-backed login, logout, role-based dashboards, profile management, and driver vehicle management.
- Deferred: optional password reset, email/OTP verification, and email delivery.
- Not implemented: ride, booking, payment, rating, notification, passenger, and admin-management functionality; these belong to later phases.