# KairoLink — Carpooling Platform
## Architecture, Folder Structure & Tech Stack

---

## 1. App Flow & Architecture

### 1.1 Overview

KairoLink is a carpooling platform connecting **Drivers** (offering rides) with **Riders** (booking seats). The system follows a classic **layered MVC architecture** built on Spring Boot, using Thymeleaf as the view layer.

**User roles:**
- **Rider** — searches rides, books seats, rates drivers
- **Driver** — publishes rides, accepts/rejects bookings, manages trips
- **Admin** — manages users, monitors rides, handles disputes/reports

### 1.2 Core App Flows

**A. Authentication Flow**
```
User → Register (Rider/Driver) → Email/OTP verification → Login
     → Spring Security validates → Session/JWT issued → Redirect to Dashboard
```

**B. Publish Ride Flow (Driver)**
```
Driver logs in → "Offer a Ride" form (source, destination, date/time, seats, price)
             → Controller validates → Service layer processes
             → RideRepository saves → Ride status = ACTIVE
```

**C. Search & Book Ride Flow (Rider)**
```
Rider searches (source, destination, date) → Controller queries Service
   → Service applies matching logic (route/date/seat availability)
   → Results shown in Thymeleaf view → Rider selects ride → Sends booking request
   → Driver notified → Driver Accepts/Rejects
   → On accept: Booking CONFIRMED → Seats decremented → Notification sent to Rider
```

**D. Ride Lifecycle**
```
CREATED → ACTIVE → (bookings happen) → ONGOING (on trip start)
        → COMPLETED (on trip end) → Rider/Driver rate each other → CLOSED
```

**E. Payment Flow (optional module)**
```
Booking confirmed → Payment initiated → Payment Service (Gateway API)
                  → On success → Booking marked PAID → Invoice generated
```

**F. Notification Flow**
```
Event triggers (booking request, accept/reject, ride reminder)
   → NotificationService → Email/In-app notification → Stored in DB + pushed to user
```

### 1.3 Layered Architecture

```
┌──────────────────────────────────────────────────────┐
│  Presentation Layer (Thymeleaf + HTML/CSS/JS + Bootstrap) │
│  - Views rendered via Thymeleaf templates              │
│  - Static assets: CSS, JS, images                     │
└───────────────────────┬────────────────────────────────┘
                         │  (Form submits / AJAX calls)
┌───────────────────────▼────────────────────────────────┐
│  Controller Layer (Spring MVC Controllers)             │
│  - @Controller classes handle requests                 │
│  - Map to Thymeleaf views (ViewResolver) or return JSON│
│  - Input validation (Bean Validation)                  │
└───────────────────────┬────────────────────────────────┘
                         │
┌───────────────────────▼────────────────────────────────┐
│  Service Layer (Business Logic)                        │
│  - Interfaces + Impl classes                            │
│  - Ride matching, booking rules, fare calculation       │
│  - Transaction management (@Transactional)              │
└───────────────────────┬────────────────────────────────┘
                         │
┌───────────────────────▼────────────────────────────────┐
│  Repository / DAO Layer (Spring Data JPA)               │
│  - JpaRepository interfaces per entity                  │
│  - Custom queries (JPQL / native SQL)                   │
└───────────────────────┬────────────────────────────────┘
                         │
┌───────────────────────▼────────────────────────────────┐
│  Database Layer (MySQL / PostgreSQL)                    │
│  - Users, Rides, Bookings, Payments, Ratings tables      │
└──────────────────────────────────────────────────────────┘

Cross-cutting concerns (applied across layers):
  • Spring Security (auth, role-based access)
  • Exception Handling (@ControllerAdvice)
  • Logging (SLF4J + Logback)
  • DTOs & Mappers (entity ↔ view/API separation)
```

### 1.4 Key Design Decisions

| Concern | Approach |
|---|---|
| MVC pattern | Spring MVC with Thymeleaf views (`ThymeleafViewResolver`, auto-configured by `spring-boot-starter-thymeleaf`) |
| Packaging | **JAR** (Spring Boot default — Thymeleaf works natively with embedded Tomcat, no external server needed) |
| Data access | Spring Data JPA (Hibernate) over raw Servlets/JDBC |
| Security | Spring Security — form login + role-based (`ROLE_RIDER`, `ROLE_DRIVER`, `ROLE_ADMIN`) |
| Dynamic UI bits | Vanilla JS + Fetch/AJAX for things like live search, seat counters, without full page reload |
| Validation | Server-side via `jakarta.validation` annotations on DTOs |
| Dev experience | Spring Boot DevTools — auto-restart + live template reload on save |

---

## 2. Folder & File Structure

```
kairolink/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/kairolink/
│   │   │       ├── KairoLinkApplication.java
│   │   │       │
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── WebConfig.java              # ViewResolver, static resource mapping
│   │   │       │   └── AppConfig.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── RiderController.java
│   │   │       │   ├── DriverController.java
│   │   │       │   ├── RideController.java
│   │   │       │   ├── BookingController.java
│   │   │       │   ├── AdminController.java
│   │   │       │   └── api/                        # REST endpoints for AJAX/JS calls
│   │   │       │       └── RideApiController.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── UserService.java
│   │   │       │   ├── RideService.java
│   │   │       │   ├── BookingService.java
│   │   │       │   ├── NotificationService.java
│   │   │       │   ├── PaymentService.java
│   │   │       │   └── impl/
│   │   │       │       ├── UserServiceImpl.java
│   │   │       │       ├── RideServiceImpl.java
│   │   │       │       ├── BookingServiceImpl.java
│   │   │       │       ├── NotificationServiceImpl.java
│   │   │       │       └── PaymentServiceImpl.java
│   │   │       │
│   │   │       ├── repository/
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── RideRepository.java
│   │   │       │   ├── BookingRepository.java
│   │   │       │   ├── RatingRepository.java
│   │   │       │   └── PaymentRepository.java
│   │   │       │
│   │   │       ├── model/                          # JPA Entities
│   │   │       │   ├── User.java
│   │   │       │   ├── Driver.java
│   │   │       │   ├── Rider.java
│   │   │       │   ├── Ride.java
│   │   │       │   ├── Booking.java
│   │   │       │   ├── Vehicle.java
│   │   │       │   ├── Rating.java
│   │   │       │   ├── Payment.java
│   │   │       │   └── enums/
│   │   │       │       ├── RideStatus.java
│   │   │       │       ├── BookingStatus.java
│   │   │       │       └── Role.java
│   │   │       │
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   │   ├── RideCreateRequest.java
│   │   │       │   │   ├── BookingRequest.java
│   │   │       │   │   └── RegisterRequest.java
│   │   │       │   └── response/
│   │   │       │       ├── RideResponse.java
│   │   │       │       └── UserResponse.java
│   │   │       │
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── ResourceNotFoundException.java
│   │   │       │   └── BookingConflictException.java
│   │   │       │
│   │   │       ├── security/
│   │   │       │   ├── CustomUserDetailsService.java
│   │   │       │   └── SecurityUtils.java
│   │   │       │
│   │   │       └── util/
│   │   │           ├── DateUtils.java
│   │   │           ├── DistanceCalculator.java     # route/geo matching helper
│   │   │           └── Constants.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties                # or application.yml
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── messages.properties                    # i18n (optional)
│   │       │
│   │       ├── templates/                             # Thymeleaf views
│   │       │   ├── auth/
│   │       │   │   ├── login.html
│   │       │   │   └── register.html
│   │       │   ├── rider/
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── search-ride.html
│   │       │   │   └── my-bookings.html
│   │       │   ├── driver/
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── publish-ride.html
│   │       │   │   └── manage-bookings.html
│   │       │   ├── admin/
│   │       │   │   └── dashboard.html
│   │       │   ├── fragments/                         # reusable Thymeleaf fragments
│   │       │   │   ├── header.html
│   │       │   │   ├── footer.html
│   │       │   │   └── navbar.html
│   │       │   └── error/
│   │       │       ├── 404.html
│   │       │       └── 500.html
│   │       │
│   │       └── static/                                # static frontend assets, served directly
│   │           ├── css/
│   │           │   ├── style.css
│   │           │   └── dashboard.css
│   │           ├── js/
│   │           │   ├── ride-search.js
│   │           │   ├── booking.js
│   │           │   └── validation.js
│   │           └── images/
│   │
│   └── test/
│       └── java/
│           └── com/kairolink/
│               ├── service/
│               │   ├── RideServiceTest.java
│               │   └── BookingServiceTest.java
│               └── controller/
│                   └── RideControllerTest.java
│
└── docs/
    ├── requirements.md
    ├── tech-stack.md
    ├── er-diagram.png
    └── api-contracts.md
```

**Notes on structure:**
- No `webapp/` directory needed — Thymeleaf templates and static assets both live under `src/main/resources/`, which is standard Spring Boot JAR layout.
- `controller/api/` separates REST-style JSON endpoints (for AJAX/JS-driven UI parts like live search) from Thymeleaf-returning controllers.
- `templates/fragments/` holds reusable `header.html`/`footer.html`/`navbar.html` included via Thymeleaf's `th:insert`/`th:replace`, replacing what used to be JSP includes.
- `dto/request` and `dto/response` keep entities decoupled from what's exposed to the view/API — good practice regardless of templating engine.
- `service` interfaces + `service/impl` keep business logic swappable/testable.
- `docs/` folder is where your other planning files (requirements, tech stack notes, etc.) can live alongside this one.

---

## 3. Tech Stack

### 3.1 Backend
| Layer | Technology |
|---|---|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.x |
| Web layer | Spring MVC (Controllers) via `DispatcherServlet` |
| View layer | Thymeleaf (`spring-boot-starter-thymeleaf`, auto-configured `ThymeleafViewResolver`) |
| Data access | Spring Data JPA + Hibernate |
| Security | Spring Security (session-based auth, role-based access control) + `thymeleaf-extras-springsecurity6` for role-aware template rendering (`sec:authorize`) |
| Validation | Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.) |
| Build tool | Maven |
| Packaging | **JAR** (Spring Boot default — embedded Tomcat serves Thymeleaf natively, no WAR/external server needed) |

### 3.2 Frontend
| Purpose | Technology |
|---|---|
| Templating | Thymeleaf (server-rendered HTML, natural templating — files are valid HTML even before rendering) |
| Styling | CSS3 + Bootstrap 5 (responsive layout) |
| Interactivity | Vanilla JavaScript (AJAX/Fetch for live search, dynamic seat counters, form validation) |
| Icons | Bootstrap Icons / Font Awesome |

### 3.3 Database
| Purpose | Technology |
|---|---|
| Primary DB | MySQL (or PostgreSQL) |
| ORM | Hibernate (via Spring Data JPA) |
| Migrations | Flyway or Liquibase (recommended for schema versioning) |

### 3.4 Supporting Tools
| Purpose | Tool |
|---|---|
| Dependency/Build | Maven |
| Server | Embedded Tomcat (dev **and** prod) — single self-contained JAR, `java -jar kairolink.jar` |
| Boilerplate reduction | Lombok |
| Logging | SLF4J + Logback |
| API testing | Postman |
| Version control | Git + GitHub |
| IDE | **VS Code** (Extension Pack for Java, Spring Boot Extension Pack, Lombok Annotations Support) |
| AI pair-programmer | GitHub Copilot / Copilot Chat (in VS Code) — see `rules.md` for boundaries |
| Containerization | Docker (optional but recommended — trivial with a JAR: `FROM eclipse-temurin:17-jre`, `COPY target/*.jar app.jar`, `ENTRYPOINT` run) |
| Hosting | Render / Railway / Fly.io (free/low-cost tiers all support "deploy a Spring Boot JAR" directly or via Docker) |

### 3.5 Optional/Future Additions
- **Payment Gateway** — Razorpay/Stripe integration via `PaymentService`
- **Maps/Geo** — Google Maps API or OpenStreetMap for route matching & distance calc
- **Notifications** — JavaMail (email) or Firebase Cloud Messaging (push)
- **Caching** — Redis for frequent ride-search queries
- **Deployment** — Docker + Tomcat, hosted on AWS/Render

---

## VS Code Setup Notes

Since this project is now being vibe-coded in **VS Code with GitHub Copilot** (rather than IntelliJ), a few practical notes:

- Install: **Extension Pack for Java** (Debugger, Maven, Test Runner), **Spring Boot Extension Pack** (Spring Boot Dashboard, Initializr, Tools), **Lombok Annotations Support for VS Code**, **GitHub Copilot** + **GitHub Copilot Chat**. Also grab a Thymeleaf-aware extension (e.g. "vscode-thymeleaf") for `th:*` attribute highlighting/completion — Thymeleaf templates are plain HTML, so even without it you get full HTML tooling for free, unlike JSP.
- Run the app via the **Spring Boot Dashboard** panel or `mvn spring-boot:run` from the integrated terminal. Enable **Spring Boot DevTools** (`spring-boot-devtools` dependency) so template edits under `src/main/resources/templates/` hot-reload in the browser without a manual restart.
- Use **Copilot Chat** with `#file` references to point it at `architecture.md`, `rules.md`, and `requirements.md` at the start of a session so its suggestions stay inside the locked stack — Copilot doesn't automatically know these files exist unless they're open or referenced.
- Copilot's inline suggestions are more prone to drifting toward JSP/JSTL syntax (older, more common in its training data for "Spring MVC view" completions) than a chat-based assistant — double-check any inline template suggestion actually uses Thymeleaf's `th:` attribute syntax, not JSTL tags.

---

## Note on Thymeleaf vs JSP

This project uses **Thymeleaf**, not JSP, for the view layer. Reasons this fits a project you intend to actually deploy:

- **JAR packaging** — Spring Boot's default, single self-contained artifact (`java -jar kairolink.jar`), no WAR/external Tomcat step.
- **Natural templating** — Thymeleaf files are valid, browser-openable HTML even before Spring renders them (`th:*` attributes degrade gracefully), which also means designers/Copilot can reason about them as plain HTML.
- **First-class Spring Boot support** — `spring-boot-starter-thymeleaf` auto-configures the view resolver; no manual `ViewResolver` bean wiring the way JSP needs.
- **Deployment-friendly** — works identically on any host that runs a JAR or a Docker container (Render, Railway, Fly.io, a plain VPS) — no dependency on an external servlet container being present.
- **Security integration** — `thymeleaf-extras-springsecurity6` gives clean `sec:authorize="hasRole('DRIVER')"`-style conditionals in templates, directly using the same roles as `SecurityConfig.java`.

The layered MVC pattern, controller/service/repository structure, and everything else in this document is otherwise unchanged from the earlier JSP-based plan — only the view layer and packaging format changed.