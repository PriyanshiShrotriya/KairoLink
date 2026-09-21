# KairoLink — Development Phases

A phased build plan, sequenced so each phase produces a working, testable slice of the app rather than isolated pieces. Follows the architecture and requirements already defined.

---

## Phase 0: Project Setup & Foundation

**Goal:** Get a running skeleton before writing any real feature.

- Initialize Maven project (Spring Boot, JAR packaging, `spring-boot-starter-thymeleaf` + `spring-boot-starter-web` + DevTools)
- Set up folder structure (`controller`, `service`, `repository`, `model`, `dto`, etc.)
- Configure `application.properties` (DB connection, server port — Thymeleaf's view resolver is auto-configured, no manual setup needed)
- Set up PostgreSQL database + create schema
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

## ## Phase 5: Maps, Routes & Live Location

**Goal:** Add map-based ride visualization, route calculation, and real-time driver location tracking using free/open-source technologies.

### 5.1 Map Integration

* Integrate **MapLibre GL JS** for interactive maps.
* Use **OpenStreetMap-based map data**.
* Configure a suitable OSM-compatible tile provider for development and production.
* Display maps inside the existing Thymeleaf UI.
* Keep the implementation independent of Google Maps.

### 5.2 Ride Location Selection

* Allow drivers to specify source and destination locations.
* Display source and destination markers on the map.
* Convert selected/searchable locations into latitude and longitude coordinates.
* Store required geographic coordinates without breaking the existing Ride model.

### 5.3 Route Calculation

* Integrate **OSRM (Open Source Routing Machine)** for route calculation.
* Calculate the route between source and destination.
* Display the calculated route as a polyline on the map.
* Calculate and display:

  * Route distance
  * Estimated travel time
* Handle invalid or unavailable routes gracefully.

### 5.4 Driver Ride Map

* Add a map view to the driver's ride experience.
* Display:

  * Source
  * Destination
  * Planned route
  * Driver's current location when available
* Keep existing ride-management functionality unchanged.

### 5.5 Driver Location Tracking

* Use the browser/device **Geolocation API** to obtain the driver's current latitude and longitude.
* Location tracking should only operate when appropriate, primarily while a ride is `ONGOING`.
* Send location updates securely to the KairoLink backend.
* Store the driver's latest location.
* Avoid unnecessary high-frequency database writes.

### 5.6 Live Driver Location

* Display the driver's current location on the rider's map.
* Update the driver's marker as new coordinates are received.
* Initially use a simple polling mechanism if appropriate.
* Design the backend so that WebSocket or Server-Sent Events can be introduced later without rebuilding the location model.

### 5.7 Rider Live Tracking

* Allow an authorized rider to view the driver's current location for their active/ongoing ride.
* Display:

  * Driver's current position
  * Ride route
  * Source
  * Destination
* Do not expose a driver's live location to unauthorized users.

### 5.8 Location Authorization & Security

* Verify that the requesting user is authorized to view a ride's live location.
* Integrate with the completed Phase 3 Booking functionality once available.
* Ensure riders can only access location information for rides they are actually participating in.
* Prevent unauthorized location updates from other users.

### 5.9 Ride Lifecycle Integration

* Start location tracking when the ride becomes `ONGOING`.
* Stop active location updates when the ride becomes `COMPLETED` or `CANCELLED`.
* Ensure stale driver locations are not presented as current.
* Integrate with the existing Ride lifecycle functionality from Phase 4.

### 5.10 Maps & Location Testing

* Test route calculation.
* Test valid and invalid coordinates.
* Test driver location updates.
* Test rider live-location access.
* Test unauthorized access.
* Test ride lifecycle/location behavior.
* Test graceful handling of routing/geolocation service failures.
* Maintain the existing full Maven test suite.

**Technology direction:**

* Map rendering: **MapLibre GL JS**
* Map data: **OpenStreetMap**
* Routing: **OSRM**
* Device location: **Browser Geolocation API**
* Database: **PostgreSQL / Supabase**
* Live updates: **Polling initially; WebSocket/SSE when required**

> Google Maps is not required for the core KairoLink mapping and live-location functionality.

---

## Phase 6: Admin Panel

**Goal:** Provide administrators with management and monitoring capabilities.

### 6.1 Admin Authentication & Authorization

* Restrict admin functionality to users with the `ADMIN` role.
* Protect all admin endpoints and pages using Spring Security.

### 6.2 User Management

* View registered users.
* View user details.
* Enable/disable users where appropriate.
* Manage user roles according to the application's authorization rules.

### 6.3 Ride Management

* View rides across the platform.
* View ride status and basic ride information.
* Handle administrative ride-management actions where required.

### 6.4 Booking Monitoring

* View booking/request activity.
* Provide appropriate administrative visibility without interfering with normal driver/rider workflows.

### 6.5 Ratings & Reports

* View ratings and reported/problematic content where required.
* Provide basic administrative monitoring.

### 6.6 Notifications

* Provide administrative visibility for relevant system notifications.
* Integrate with the existing Notification domain where appropriate.

### 6.7 Admin Dashboard UI

* Create a dedicated admin dashboard.
* Display useful platform statistics such as:

  * Total users
  * Total rides
  * Active rides
  * Booking activity
* Maintain consistency with the existing KairoLink design system.

---

## Phase 7: UI Polish & Responsiveness

**Goal:** Bring the complete application to a consistent, polished, responsive state.

### 7.1 Design Consistency

* Apply the existing KairoLink design system consistently.
* Standardize:

  * Colors
  * Typography
  * Buttons
  * Forms
  * Cards
  * Navigation
  * Alerts
  * Modals

### 7.2 Responsive Design

* Ensure all pages work properly on:

  * Desktop
  * Tablet
  * Mobile
* Improve navigation and mobile menu behavior.

### 7.3 Rider UI

* Polish:

  * Search
  * Ride results
  * Ride details
  * Booking/request flows
  * My bookings
  * Live ride tracking
  * Profile

### 7.4 Driver UI

* Polish:

  * Dashboard
  * Publish ride
  * My rides
  * Ride lifecycle controls
  * Ride map
  * Live location
  * Profile

### 7.5 Admin UI

* Polish the admin dashboard and management pages.

### 7.6 Accessibility & UX

* Improve keyboard navigation.
* Improve form validation and error messages.
* Improve loading, empty, and error states.
* Ensure sufficient contrast and readable typography.
* Add appropriate feedback for user actions.

---

## Phase 8: Testing & Hardening

**Goal:** Make the application reliable, secure, and production-ready.

### 8.1 Backend Testing

* Unit tests
* Service tests
* Repository tests
* Controller tests
* Security tests

### 8.2 Integration Testing

* Authentication flow
* Ride lifecycle
* Booking flow
* Rating flow
* Notification flow
* Maps and route functionality
* Live location functionality

### 8.3 Security Hardening

* Authorization checks
* Input validation
* CSRF protection
* Authentication protection
* IDOR/access-control checks
* Location-data access control
* Secure handling of API/service credentials

### 8.4 Reliability

* Handle database failures gracefully.
* Handle routing-service failures gracefully.
* Handle unavailable geolocation.
* Handle stale location data.
* Handle network interruptions.
* Add appropriate logging and error handling.

### 8.5 Performance

* Optimize database queries.
* Avoid unnecessary location writes.
* Optimize live-location update frequency.
* Review map and route API usage.
* Add appropriate indexes where required.

### 8.6 Final Regression Testing

* Run the complete Maven test suite.
* Test all major user flows.
* Review logs for unexpected errors.
* Perform a final code and dependency review.

---

## Phase 9: Deployment

**Goal:** Deploy KairoLink reliably for real-world use.

### 9.1 Production Configuration

* Configure production environment variables.
* Configure production PostgreSQL/Supabase connection.
* Configure secure secrets.
* Configure appropriate Spring profiles.

### 9.2 Database

* Verify Flyway migrations.
* Verify production database schema.
* Confirm migration ordering and compatibility.

### 9.3 Application Deployment

* Build the production JAR.
* Deploy the Spring Boot application.
* Configure production server/environment.

### 9.4 Maps & Routing Deployment

* Configure production-safe OSM-compatible map tiles.
* Configure OSRM or another suitable OSM-based routing service.
* Do not depend on public OSM tile infrastructure for heavy production traffic.
* Keep routing and map-service credentials/configuration outside source control where applicable.

### 9.5 Production Monitoring

* Monitor application errors.
* Monitor database health.
* Monitor routing/location-service failures.
* Monitor application performance.
* Configure appropriate logging.

### 9.6 Final Production Verification

* Authentication
* Ride publishing
* Ride search
* Booking
* Ride lifecycle
* Ratings
* Notifications
* Maps
* Route calculation
* Live location
* Admin functionality

---

## Phase 10: Post-MVP Enhancements

**Goal:** Add advanced features after the core KairoLink platform is stable and deployed.

### 10.1 In-App Chat

* Rider-driver messaging.
* Conversation management.
* Real-time messaging.

### 10.2 Advanced Maps

* Better route alternatives.
* Route optimization.
* Advanced navigation features.
* More detailed location history where appropriate.

### 10.3 Payments

* Integrate online payment functionality.
* Payment records and transaction status.
* Refund/cancellation handling.

### 10.4 Recurring Rides

* Allow drivers to create recurring ride schedules.
* Generate/manage recurring ride instances.

### 10.5 Advanced Live Location

* Location history.
* Improved real-time synchronization.
* More efficient real-time infrastructure.
* WebSocket-based updates if not already implemented.

### 10.6 Redis / Caching

* Introduce Redis where performance requirements justify it.
* Cache frequently accessed data.
* Support scalable real-time functionality.

### 10.7 Advanced Recommendations

* Ride recommendations.
* Route-based matching.
* User preference-based ride discovery.

### 10.8 Additional Platform Features

* Advanced notifications.
* Ride reminders.
* Safety features.
* Analytics.
* Additional user and admin capabilities.
