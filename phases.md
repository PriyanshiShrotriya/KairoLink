# KairoLink – Development Phases

## Phase 0 – Project Setup & Foundation
- [x] Initialize Spring Boot MVC project
- [x] Configure Maven
- [x] Configure PostgreSQL / Supabase
- [x] Configure Thymeleaf
- [x] Configure Flyway database migrations
- [x] Establish project package structure
- [x] Establish shared CSS / JavaScript structure
- [x] Configure Git and GitHub workflow
- [x] Create base application architecture

---

## Phase 1 – User Authentication & Profile
- [x] User registration
- [x] User login/logout
- [x] Password hashing
- [x] Role-based access control
  - RIDER
  - DRIVER
  - ADMIN
- [x] User principal / authentication integration
- [x] User profile
- [x] Driver profile support
- [x] Authentication and authorization tests

---

## Phase 2 – Ride Management
- [x] Driver create ride
- [x] View driver's rides
- [x] Edit ride
- [x] Cancel ride
- [x] Ride status management
  - CREATED
  - ACTIVE
  - ONGOING
  - COMPLETED
  - CANCELLED
- [x] Future-ride validation
- [x] Driver ownership validation
- [x] Ride management UI
- [x] Driver dashboard integration
- [x] Ride management tests

---

## Phase 3 – Ride Search & Booking
- [x] Rider ride search
- [x] Search/filter available rides
- [x] Ride details
- [x] Rider booking request
- [x] Driver view booking requests
- [x] Driver accept booking
- [x] Driver reject booking
- [x] Booking status management
  - PENDING
  - CONFIRMED
  - REJECTED
  - CANCELLED
- [x] Duplicate active-booking prevention
- [x] Rider booking flow
- [x] Driver booking management UI
- [x] Booking authorization and ownership validation
- [x] Booking tests
- [x] H2/PostgreSQL migration compatibility

---

## Phase 4 – Ratings, Notifications & Ride Lifecycle

### Ride Lifecycle
- [x] Driver can start an active ride
- [x] ACTIVE → ONGOING transition
- [x] Driver can complete an ongoing ride
- [x] ONGOING → COMPLETED transition
- [x] Driver-only lifecycle authorization
- [x] Ride lifecycle validation
- [x] Ride lifecycle tests

### Ratings
- [x] Create rating domain
- [x] Rider can rate the driver after ride completion
- [x] Driver can rate a confirmed rider after ride completion
- [x] Prevent unauthorized users from rating
- [x] Validate rating participants against the ride
- [x] Prevent duplicate ratings
- [x] Require completed ride before rating
- [x] Rating form and UI
- [x] Rating service tests
- [x] Rating authorization tests

### In-App Notifications
- [x] Create notification domain
- [x] Store notifications in database
- [x] Booking-request notification to driver
- [x] Booking-accepted notification to rider
- [x] Booking-rejected notification to rider
- [x] Unread notification count
- [x] Notification navbar integration
- [x] Notifications page
- [x] Link booking-request notifications to the relevant booking
- [x] Highlight relevant booking request from notification
- [x] Notification service tests
- [x] Booking notification integration tests

### Phase 4 Testing & Stability
- [x] Integrate Phase 3 booking functionality with Phase 4 notifications
- [x] Preserve Phase 3 booking behavior during Phase 4 integration
- [x] Resolve H2/PostgreSQL Flyway compatibility issues
- [x] Verify authorization across ride lifecycle, booking, rating, and notifications
- [x] Run focused Phase 4 tests
- [x] Run full Maven test suite
- [x] Verify Flyway migrations V1–V7
- [x] Review implementation for unintended changes

---

## Phase 5 – Maps, Routes & Live Location

### Map Integration
- [ ] Integrate MapLibre GL JS
- [ ] Integrate OpenStreetMap map data/provider
- [ ] Display interactive maps
- [ ] Select pickup and destination locations
- [ ] Store latitude/longitude coordinates

### Route Calculation
- [ ] Integrate OSRM
- [ ] Calculate route between pickup and destination
- [ ] Display route on map
- [ ] Calculate/display estimated distance
- [ ] Calculate/display estimated travel time

### Driver Location
- [ ] Add browser Geolocation API support
- [ ] Request location permission
- [ ] Store driver's current latitude/longitude
- [ ] Update driver location
- [ ] Validate location updates
- [ ] Add location authorization/security rules

### Rider Live Tracking
- [ ] Display driver's current location
- [ ] Show driver location during an active ride
- [ ] Connect ride lifecycle with live tracking
- [ ] Implement initial polling-based location updates
- [ ] Evaluate WebSocket/SSE for future real-time updates

### Phase 5 Testing
- [ ] Test map rendering
- [ ] Test coordinate storage
- [ ] Test route calculation
- [ ] Test location authorization
- [ ] Test driver location updates
- [ ] Test rider live tracking
- [ ] Test lifecycle/location integration

---

## Phase 6 – Admin Panel
- [ ] Admin dashboard
- [ ] User management
- [ ] Driver management
- [ ] Ride management
- [ ] Booking management
- [ ] Rating moderation
- [ ] Notification management
- [ ] Basic system statistics
- [ ] Admin authorization/security
- [ ] Admin tests

---

## Phase 7 – UI Polish & Responsiveness
- [ ] Complete responsive design
- [ ] Improve mobile layout
- [ ] Improve desktop layout
- [ ] Consistent typography
- [ ] Consistent spacing
- [ ] Loading states
- [ ] Empty states
- [ ] Error states
- [ ] Form validation feedback
- [ ] Accessibility improvements
- [ ] Finalize visual consistency across dashboards
- [ ] Cross-browser UI testing

---

## Phase 8 – Testing & Hardening
- [ ] Expand unit test coverage
- [ ] Expand integration test coverage
- [ ] Controller tests
- [ ] Service tests
- [ ] Repository tests
- [ ] Security/authorization testing
- [ ] Database migration testing
- [ ] Edge-case testing
- [ ] Error-handling review
- [ ] Input validation review
- [ ] Performance review
- [ ] Security review
- [ ] Full regression testing

---

## Phase 9 – Deployment
- [ ] Prepare production configuration
- [ ] Configure production database
- [ ] Configure environment variables/secrets
- [ ] Configure production logging
- [ ] Build production JAR
- [ ] Deploy backend
- [ ] Deploy frontend/UI
- [ ] Configure domain
- [ ] Configure HTTPS
- [ ] Verify production database migrations
- [ ] Production smoke testing
- [ ] Deployment documentation

---

## Phase 10 – Post-MVP Enhancements
- [ ] Email notifications
- [ ] Ride reminder notifications
- [ ] Ride cancellation notifications
- [ ] Average rating displayed on user profile
- [ ] Advanced ride search/filtering
- [ ] Improved live location with WebSockets/SSE
- [ ] Ride history improvements
- [ ] User reporting/complaint system
- [ ] Advanced admin analytics
- [ ] Additional UI/UX improvements
- [ ] Performance optimizations
- [ ] Additional security hardening
- [ ] Other post-MVP features based on user feedback