# KairoLink — Carpooling Platform
## Architecture, Folder Structure & Tech Stack

---

## 1. App Flow & Architecture

### 1.1 Overview

KairoLink is a carpooling platform connecting **Drivers** (offering rides) with **Riders** (booking seats). The system follows a classic **layered MVC architecture** built on Spring Boot, using JSP as the view layer.

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
   → Results shown in JSP view → Rider selects ride → Sends booking request
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
│  Presentation Layer (JSP + HTML/CSS/JS + Bootstrap)   │
│  - Views rendered via JSP/JSTL                        │
│  - Static assets: CSS, JS, images                     │
└───────────────────────┬────────────────────────────────┘
                         │  (Form submits / AJAX calls)
┌───────────────────────▼────────────────────────────────┐
│  Controller Layer (Spring MVC Controllers)             │
│  - @Controller classes handle requests                 │
│  - Map to JSP views (ViewResolver) or return JSON      │
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
| MVC pattern | Spring MVC with JSP views (`InternalResourceViewResolver`) |
| Packaging | **WAR** (required for embedded Tomcat + JSP support — JAR packaging does not support JSP with embedded servers) |
| Data access | Spring Data JPA (Hibernate) over raw Servlets/JDBC |
| Security | Spring Security — form login + role-based (`ROLE_RIDER`, `ROLE_DRIVER`, `ROLE_ADMIN`) |
| Dynamic UI bits | Vanilla JS + Fetch/AJAX for things like live search, seat counters, without full page reload |
| Validation | Server-side via `jakarta.validation` annotations on DTOs |

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
│   │   ├── resources/
│   │   │   ├── application.properties               # or application.yml
│   │   │   ├── application-dev.properties
│   │   │   ├── application-prod.properties
│   │   │   ├── static/                               # served directly (if not WAR-only JSP setup)
│   │   │   └── messages.properties                   # i18n (optional)
│   │   │
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── views/                            # JSP pages
│   │       │       ├── auth/
│   │       │       │   ├── login.jsp
│   │       │       │   └── register.jsp
│   │       │       ├── rider/
│   │       │       │   ├── dashboard.jsp
│   │       │       │   ├── search-ride.jsp
│   │       │       │   └── my-bookings.jsp
│   │       │       ├── driver/
│   │       │       │   ├── dashboard.jsp
│   │       │       │   ├── publish-ride.jsp
│   │       │       │   └── manage-bookings.jsp
│   │       │       ├── admin/
│   │       │       │   └── dashboard.jsp
│   │       │       ├── common/
│   │       │       │   ├── header.jsp
│   │       │       │   ├── footer.jsp
│   │       │       │   └── navbar.jsp
│   │       │       └── error/
│   │       │           ├── 404.jsp
│   │       │           └── 500.jsp
│   │       │
│   │       └── assets/                               # static frontend assets
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
- `controller/api/` separates REST-style JSON endpoints (for AJAX/JS-driven UI parts like live search) from JSP-returning controllers.
- `dto/request` and `dto/response` keep entities decoupled from what's exposed to the view/API — good practice even in a JSP app.
- `service` interfaces + `service/impl` keep business logic swappable/testable.
- `docs/` folder is where your other planning files (requirements, tech stack notes, etc.) can live alongside this one.

---

## 3. Tech Stack

### 3.1 Backend
| Layer | Technology |
|---|---|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.x |
| Web layer | Spring MVC (Controllers) + Servlets (under the hood via `DispatcherServlet`) |
| View layer | JSP + JSTL (`InternalResourceViewResolver`) |
| Data access | Spring Data JPA + Hibernate |
| Security | Spring Security (session-based auth, role-based access control) |
| Validation | Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.) |
| Build tool | Maven |
| Packaging | WAR (required to run JSP on embedded/external Tomcat) |

### 3.2 Frontend
| Purpose | Technology |
|---|---|
| Templating | JSP + JSTL |
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
| Server | Embedded Tomcat (dev) / External Tomcat (prod, for WAR deployment) |
| Boilerplate reduction | Lombok |
| Logging | SLF4J + Logback |
| API testing | Postman |
| Version control | Git + GitHub |
| IDE | IntelliJ IDEA / Eclipse STS |

### 3.5 Optional/Future Additions
- **Payment Gateway** — Razorpay/Stripe integration via `PaymentService`
- **Maps/Geo** — Google Maps API or OpenStreetMap for route matching & distance calc
- **Notifications** — JavaMail (email) or Firebase Cloud Messaging (push)
- **Caching** — Redis for frequent ride-search queries
- **Deployment** — Docker + Tomcat, hosted on AWS/Render

---

## Important Technical Note on JSP + Spring Boot

Spring Boot's embedded Tomcat has **limited JSP support**:
- You must package the app as a **WAR**, not a JAR
- JSPs must live under `src/main/webapp/WEB-INF/views/`
- For production, deploying to an **external Tomcat** server is more reliable than relying on embedded Tomcat for JSP rendering

If this becomes a pain point later, the fallback is swapping JSP for **Thymeleaf** (works cleanly with embedded Tomcat + JAR packaging) — but since you specifically want JSP/Servlets experience, the WAR + external Tomcat route is the way to go.