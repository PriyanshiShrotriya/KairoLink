# KairoLink — Development Phases

A phased build plan, sequenced so each phase produces a working, testable slice of the app rather than isolated pieces. Follows the architecture and requirements already defined.

---

## Phase 0: Project Setup & Foundation

**Goal:** Get a running skeleton before writing any real feature.

- Initialize Maven project (Spring Boot, JAR packaging, `spring-boot-starter-thymeleaf` + `spring-boot-starter-web` + DevTools)
- Set up folder structure (`controller`, `service`, `repository`, `model`, `dto`, etc.)
- Configure `application.properties` (DB connection, server port — Thymeleaf's view resolver is auto-configured, no manual setup needed)
- Set up MySQL/PostgreSQL database + create schema
- Confirm a basic "Hello KairoLink" Thymeleaf page renders at `src/main/resources/templates/`, with DevTools hot-reload working
- Set up Git repo, `.gitignore`, README
- Set up base layout fragments (`fragments/header.html`, `fragments/footer.html`, `fragments/navbar.html`) and global CSS/JS structure under `static/`

**Output:** App boots, DB connects, one static Thymeleaf page renders end-to-end.

---

## Phase 1: User Authentication & Profile

**Goal:** Users can register, log in, and manage their profile — the foundation every other feature depends on.

- `User` entity (with role: RIDER/DRIVER/ADMIN, or toggleable role flags)
- Registration flow (form → validation → hashed password → save)
- Login/logout via Spring Security (session-based)
- Role-based access control (secure routes by role)
- Profile view/edit page (name, phone, photo)
- Driver-specific: `Vehicle` entity + form to add vehicle details
- Password reset flow (optional in this phase, can defer)

**Output:** A user can sign up as Rider or Driver, log in, and see a role-appropriate dashboard shell.

---

## Phase 2: Ride Management (Driver Side)

**Goal:** Drivers can publish and manage rides.

- `Ride` entity (source, destination, date/time, seats, price, status)
- "Publish a Ride" form + controller + service + repository
- "My Rides" page (list upcoming/ongoing/completed/cancelled)
- Edit/cancel a ride (before it starts)
- Ride status transitions (CREATED → ACTIVE → ONGOING → COMPLETED/CANCELLED)

**Output:** A logged-in Driver can create a ride and see it listed on their dashboard.

---

## Phase 3: Ride Search & Booking (Rider Side)

**Goal:** Riders can find and book rides — this is the core transaction of the platform.

- Search form (source, destination, date) + results page
- Matching/filter logic in `RideService` (route match, date, seats > 0, not expired)
- Ride details page (driver info, vehicle, price, rating placeholder)
- Booking request flow (`Booking` entity: status PENDING/CONFIRMED/REJECTED/CANCELLED)
- Driver accepts/rejects booking → seat count updates transactionally
- Prevent overbooking (transactional seat-decrement check)
- Rider's "My Bookings" page

**Output:** A Rider can search, request a seat, and see booking status update once the Driver responds.

---

## Phase 4: Ratings, Notifications & Ride Lifecycle Completion

**Goal:** Close the loop on a ride and start building trust signals.

- Mark ride as started/completed (Driver action)
- Post-ride rating flow (`Rating` entity, 1–5 stars + comment, both directions)
- Average rating calculation shown on user profile
- In-app notifications (booking requested/accepted/rejected, ride reminder, cancellation)
- Email notifications for key events (JavaMail integration)

**Output:** Full ride lifecycle works start to finish, with ratings and notifications wired in.

---

## Phase 5: Admin Panel

**Goal:** Give the platform operator visibility and control.

- Admin dashboard (basic stats: total users, active rides, bookings this week)
- User management (view, suspend, ban)
- Ride/booking monitoring view
- Handle reported issues (basic flagging + resolution status)

**Output:** Admin can log in, see platform activity, and take moderation actions.

---

## Phase 6: UI Polish & Responsiveness

**Goal:** Make it look and feel like a real product, not a prototype.

- Apply consistent theme (per `design.md`) across all Thymeleaf templates
- Responsive layout pass (Bootstrap grid, mobile breakpoints)
- Form validation feedback (client-side JS + server-side error display)
- Loading states, empty states (e.g. "no rides found"), error pages (404/500)
- Dashboard UX cleanup (Rider/Driver/Admin views)

**Output:** App is visually consistent and usable across screen sizes.

---

## Phase 7: Testing & Hardening

**Goal:** Catch issues before real users do.

- Unit tests for service layer (`RideServiceTest`, `BookingServiceTest`, etc.)
- Controller-level tests for key flows
- Manual QA pass on all user stories from `requirements.md`
- Edge case testing: double booking, expired rides, cancelled ride with active bookings, invalid inputs
- Security review (role access checks, password handling, session fixation)

**Output:** Core flows are test-covered and edge cases handled gracefully.

---

## Phase 8: Deployment

**Goal:** Get KairoLink live.

- Package as a self-contained JAR (`mvn clean package`), containerize with the project `Dockerfile`
- Set up production database (separate from dev — managed Postgres/MySQL on the chosen host, e.g. Render/Railway/Neon/Supabase)
- Environment-specific configs (`application-prod.properties` or environment variables — never commit prod DB credentials)
- Set up basic monitoring/logging for production
- Deploy to chosen host (Render / Railway / Fly.io — all support "deploy from Dockerfile" or "deploy a Spring Boot JAR" with minimal config)

**Output:** KairoLink is live and accessible via a public URL.

---

## Phase 9 (Post-MVP): Enhancements

**Goal:** Iterate based on real usage.

- In-app chat between Driver & Rider
- Google Maps integration for route visualization & better matching
- In-app payments/wallet
- Recurring rides (auto-publish daily commute)
- Live location sharing during active ride
- Caching (Redis) for frequent searches

**Output:** Platform evolves beyond MVP based on user feedback and adoption.

---

## Suggested Sequencing Notes

- **Phases 0–3 are the critical path** — nothing else matters until publish → search → book works end-to-end.
- **Phase 4 (ratings/notifications)** can partially overlap with Phase 3 once `Booking` entity exists.
- **Phase 5 (Admin)** can be built in parallel by a second contributor, since it depends only on Phase 1 (User) and Phase 2/3 (Ride/Booking) entities existing.
- **Phase 6 (UI polish)** is easiest done incrementally per-phase rather than saved entirely for the end — but a dedicated pass here is still worth it before launch.
- **Phase 7 (Testing)** should ideally start as early as Phase 2, not be bolted on at the end.