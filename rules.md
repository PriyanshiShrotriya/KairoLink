# KairoLink — Rules for Vibe Coding

Ground rules to keep the project consistent, working, and not a mess of AI-generated inconsistency. This project is vibe-coded in **VS Code using GitHub Copilot / Copilot Chat**. Reference this file at the start of every coding session with the AI.

---

## What to Use

### Core Stack (locked — don't deviate)
- **Java 21** as the project baseline for this development track, with Spring Boot 4.1.x (on Spring Framework 7.0.x)
- **Spring MVC** for controllers, **Thymeleaf** for views (`spring-boot-starter-thymeleaf`, auto-configured view resolver)
- **Spring Data JPA + Hibernate** for all DB access — no raw JDBC unless there's a specific reason
- **Spring Security** for auth — no custom-rolled auth/session handling (use `thymeleaf-extras-springsecurity6` for role checks in templates)
- **Maven** for build/dependency management
- **PostgreSQL** is the chosen database for this project — use it consistently for local dev and deployment; no MySQL in this codebase
- **JAR packaging** (Spring Boot default — embedded Tomcat, no external server required)
- **Spring Boot DevTools** in dev for auto-restart + template hot-reload

> Note: OpenJDK 25.0.4 is compatible as a local runtime and may be used for testing in a workstation environment, but the project baseline for this development track is Java 21 per the Spring Initializr selection.

### Frontend
- **Thymeleaf** for all server-rendered pages — templates live under `src/main/resources/templates/`, valid HTML with `th:*` attributes
- **Bootstrap 5** for layout/styling — don't hand-roll a full CSS framework
- **Vanilla JavaScript** (Fetch API for AJAX) — no jQuery, no frontend framework (React/Vue/Angular) unless the project scope explicitly changes
- Keep JS in separate `.js` files under `src/main/resources/static/js/`, not inline `<script>` blocks scattered across templates
- Shared layout pieces (`header.html`, `footer.html`, `navbar.html`) go under `templates/fragments/` and are pulled in via `th:insert`/`th:replace` — no copy-pasting the navbar into every page

### Project Structure
- Follow the layered structure already defined in `architecture.md`: Controller → Service → Repository → Model
- New features get their own controller/service/repository trio, not bolted onto existing ones
- DTOs for anything crossing the controller boundary — don't expose JPA entities directly in forms/views
- Lombok for boilerplate (`@Getter`, `@Setter`, `@Builder`) — don't hand-write getters/setters

### Validation & Errors
- Use `jakarta.validation` annotations (`@NotNull`, `@Size`, `@Email`, etc.) on DTOs
- Centralize error handling via `@ControllerAdvice` / `GlobalExceptionHandler` — one place, not scattered try-catch blocks
- Use custom exceptions (`ResourceNotFoundException`, `BookingConflictException`) that map to meaningful HTTP/Thymeleaf error pages

### Database
- **Flyway** or **Liquibase** for schema migrations once past Phase 0 — no manually-run ad-hoc SQL scripts that aren't tracked anywhere
- Foreign keys enforced at the DB level, not just assumed in code

---

## What to Avoid

### Libraries / Dependencies
- **Don't add a new dependency just because the AI suggests it mid-task.** Every new library goes into `pom.xml` deliberately, and you should know *why* it's there.
- No frontend frameworks (React/Angular/Vue) — breaks the Thymeleaf-based server-rendered architecture already committed to
- No JSP, FreeMarker, Mustache, or other template engines — Thymeleaf only, per the locked stack
- No random utility libraries duplicating what Spring already provides (e.g. don't add Apache Commons for something `StringUtils`/Spring already covers)
- No ORMs other than Hibernate/JPA — don't let AI suggest MyBatis, jOOQ, etc. mid-project
- Avoid pulling in a full auth library (e.g. Auth0 SDK, JWT libraries) unless you've deliberately decided to move off session-based Spring Security auth

### Error Handling
- **Never let AI swallow exceptions silently** (`catch (Exception e) {}` with nothing inside) — every catch block should log or rethrow meaningfully
- Don't scatter try-catch in controllers — push error handling to the service layer or global handler
- Don't return raw stack traces or exception messages to the template/user — map to clean user-facing messages
- Don't let AI "fix" a bug by wrapping everything in a broad try-catch instead of finding the actual root cause

### Boundaries for the AI (Important — read before every session)
- **Don't let AI restructure the folder layout** without you reviewing it first — structure is defined in `architecture.md`, deviations should be a conscious decision, not a side effect of a prompt
- **Don't let AI silently change the tech stack** (e.g. "let's just use Thymeleaf instead, it's easier with Spring Boot") — if it suggests this, treat it as a discussion point, not something to auto-apply
- **Don't accept AI-generated DB schema changes without reading them** — always check generated entities/migrations before running them against your DB
- **Don't let AI generate an entire feature end-to-end in one shot without checkpoints** — build one layer at a time (entity → repository → service → controller → view) and verify each before moving to the next
- **Never commit AI-generated code you haven't read.** Vibe coding doesn't mean blind coding — skim every file before accepting/committing
- **Don't let AI touch Security config casually** — changes to `SecurityConfig.java`, password handling, or role checks should always be reviewed line by line, since bugs here are the ones that actually hurt you
- **Don't let AI invent new entities/fields ad hoc** — if a new field is needed on `Ride`/`Booking`/`User`, that's a deliberate schema decision, not something to introduce silently while "just fixing a bug"
- **No copy-pasted code without understanding it** — if the AI generates something non-trivial (matching logic, transactional seat-decrement, security config), ask it to explain it before moving on
- **Don't let scope creep in through AI suggestions** — if a suggestion adds a feature not in `requirements.md` or the current `phases.md` phase, flag it and defer instead of accepting

### Code Quality
- No giant "God controllers" or "God services" handling multiple unrelated responsibilities — one controller/service per domain concern (Ride, Booking, User, etc.)
- No magic numbers/strings — use `enums` (`RideStatus`, `BookingStatus`) and `Constants.java`, already scoped in the structure
- No commented-out dead code left in — either it's used or it's deleted
- No TODO comments left unresolved past the phase they were written in — track them in `phases.md` progress instead

---

## Working with GitHub Copilot in VS Code

Copilot behaves differently from a chat-only assistant, so a few extra guardrails apply:

- **Start each Copilot Chat session by referencing the docs**, e.g. `#file:architecture.md #file:rules.md #file:requirements.md` (or open them as tabs) — Copilot only respects boundaries it can actually see in context, it won't recall a prior conversation.
- **Be suspicious of inline (ghost-text) suggestions more than chat replies.** Inline autocomplete is pattern-matching on your open files and public training data — it's the most likely place a stray JSP/JSTL tag (`<c:forEach>`, `<%= %>`), a jQuery snippet, or an unrequested dependency sneaks in, since older Spring MVC tutorials skew JSP-heavy. Read every inline suggestion before hitting Tab; don't accept multi-line completions on autopilot.
- **Use Copilot Chat's `/fix`, `/explain`, and inline chat for anything touching Security, transactions, or schema** — don't accept an inline ghost-text block for these; ask for it explicitly in chat so you get a chance to review before it lands in the file.
- **One file/layer per Copilot Chat request** — asking "build the whole booking feature" in one prompt is how you end up with an entity, repo, service, controller, and template all generated (and half-read) in one shot. Ask for entity → repository → service → controller → view, reviewing between each, same as with any other AI assistant per the checklist below.
- **Turn off Copilot's "next edit suggestions"/multi-file edit features for this project if they start restructuring folders or touching multiple layers at once** — the "don't let AI restructure the folder layout" rule above applies just as much to Copilot's newer agentic edit modes as to a chat prompt.
- **Commit often, in small chunks**, so a bad Copilot suggestion is a one-command `git checkout` away, not a multi-file untangling job.

## Session Checklist (quick reference before/during each coding session)

- [ ] Am I working within the current phase from `phases.md`, not jumping ahead?
- [ ] Does this feature match what's in `requirements.md`?
- [ ] Did AI introduce any new dependency? If yes — was it deliberate?
- [ ] Did I read the generated code before accepting it, especially anything touching Security or the DB?
- [ ] Are errors being handled centrally, not swallowed or scattered?
- [ ] Does the new code follow the existing folder structure from `architecture.md`?