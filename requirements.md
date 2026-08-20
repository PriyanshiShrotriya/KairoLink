# KairoLink — Requirements Document

---

## 1. What to Build

**KairoLink** is a web-based carpooling platform that connects people traveling the same route so they can share rides — cutting travel cost, reducing the number of vehicles on the road, and making commuting more social and sustainable.

In short: a platform where **Drivers publish rides** they're already taking, and **Riders search and book a seat** on those rides.

**Core problem it solves:**
- Commuters/travelers going the same route pay full cost alone (fuel, tolls, parking) → KairoLink lets them split it
- Empty seats in private vehicles go to waste on regular routes (office commute, intercity travel)
- Existing options (public transport, cabs) are either inflexible or expensive

**MVP scope (what "done" looks like for v1):**
- User registration/login (Rider & Driver roles)
- Driver can publish a ride (route, date/time, seats, price)
- Rider can search rides by source/destination/date and book a seat
- Driver can accept/reject booking requests
- Both parties can view ride history and rate each other
- Admin can monitor users and rides

**Out of scope for v1** (future phases): live GPS tracking, in-app chat, in-app payments/wallet, dynamic pricing, mobile app.

---

## 2. Targeted User

| User Type | Description | Primary Goal |
|---|---|---|
| **Rider** | Someone needing to travel a route (daily commuter, student, occasional traveler) who doesn't want to drive or pay full cab/transport fare | Find a reliable, cheap, and safe ride matching their route & time |
| **Driver** | Vehicle owner already making a trip (regular commute or one-off) who has spare seats | Offset fuel/travel cost by filling empty seats with verified riders |
| **Admin** | Platform operator | Ensure trust & safety — manage users, monitor rides/bookings, resolve disputes, remove bad actors |

**Typical personas:**
- *Office commuter (Rider/Driver)* — same route every weekday, wants a fixed, familiar co-passenger group
- *Intercity traveler (Rider)* — occasional trips between cities, price-sensitive, books once
- *Vehicle owner (Driver)* — owns a car, commutes daily, wants to recover fuel cost
- *Platform admin* — needs visibility into flagged users, cancelled rides, complaints

**Assumptions about users:**
- Have a smartphone/computer with a browser
- Comfortable with basic web forms (registration, search)
- Willing to share route/timing details for matching

---

## 3. Features

### 3.1 Authentication & Profile
- Register as Rider or Driver (or both — a user can toggle role)
- Login/logout (Spring Security, session-based)
- Email verification / OTP (optional for MVP, recommended for trust)
- Edit profile (name, phone, photo, emergency contact)
- Driver-specific: add vehicle details (model, number plate, seats, photo)
- Password reset flow

### 3.2 Ride Management (Driver)
- Publish a ride: source, destination, intermediate stops (optional), date, time, available seats, price per seat
- Edit/cancel a published ride (before it starts)
- View list of "My Rides" (upcoming, ongoing, completed, cancelled)
- View & respond to incoming booking requests (Accept/Reject)
- Mark ride as started / completed

### 3.3 Ride Search & Booking (Rider)
- Search rides by source, destination, date (and optionally time window)
- Filter results (price range, departure time, seats available, driver rating)
- View ride details (driver profile, vehicle, route, price, rating)
- Send booking request for N seats
- View booking status (Pending / Confirmed / Rejected / Cancelled)
- Cancel a booking (with cancellation policy/cutoff)
- View "My Bookings" history

### 3.4 Matching Logic
- Match rides by source/destination proximity (exact or nearby locality match for MVP; can evolve to geo-radius matching)
- Filter out rides with 0 seats left or that are in the past
- Sort results by relevance (time proximity, price, rating)

### 3.5 Ratings & Reviews
- After ride completion, Rider rates Driver and vice versa (1–5 stars + comment)
- Average rating shown on user profile
- Helps build trust for future matches

### 3.6 Notifications
- In-app notification for: booking request received, booking accepted/rejected, ride reminder (X hours before), ride cancelled
- Email notifications for key events (booking confirmation, cancellation)

### 3.7 Admin Panel
- View all users (Riders/Drivers), suspend/ban accounts
- View all rides & bookings (monitoring)
- Handle reported issues/disputes
- Basic dashboard stats (total users, active rides, bookings this week)

### 3.8 Nice-to-Have (Post-MVP)
- In-app chat between Driver & Rider (pre-ride coordination)
- Live location sharing during the ride
- In-app payments/wallet integration
- Recurring rides (auto-publish for daily commute)
- Route/distance-based dynamic pricing
- Google Maps integration for route visualization

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | System shall allow user registration with role selection (Rider/Driver) |
| FR-2 | System shall authenticate users via email/password with session management |
| FR-3 | Driver shall be able to create, edit, and cancel a ride listing |
| FR-4 | Rider shall be able to search rides by source, destination, and date |
| FR-5 | Rider shall be able to request a booking for available seats |
| FR-6 | Driver shall be able to accept or reject a booking request |
| FR-7 | System shall auto-update available seat count on booking confirmation/cancellation |
| FR-8 | System shall prevent booking on a ride with 0 available seats |
| FR-9 | System shall allow both parties to rate each other after ride completion |
| FR-10 | System shall send notifications on key booking/ride events |
| FR-11 | Admin shall be able to view, suspend, or remove any user or ride |
| FR-12 | System shall maintain ride and booking history per user |

## 5. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | **Performance** — Ride search results should return within 2 seconds under normal load |
| NFR-2 | **Security** — Passwords stored hashed (BCrypt); role-based access control enforced on all endpoints |
| NFR-3 | **Scalability** — Layered architecture (Controller/Service/Repository) should allow scaling individual layers/services later |
| NFR-4 | **Usability** — Responsive UI (Bootstrap) usable on both desktop and mobile browsers |
| NFR-5 | **Reliability** — No double-booking of the same seat (handled via transactional seat-decrement logic) |
| NFR-6 | **Availability** — Target 99% uptime for hosted environment |
| NFR-7 | **Maintainability** — Clear separation of concerns (DTOs, service interfaces) for testability |
| NFR-8 | **Data Integrity** — Foreign key constraints between Users, Rides, Bookings, Ratings |
| NFR-9 | **Auditability** — Timestamps (createdAt/updatedAt) on all major entities |

## 6. Key User Stories

- *As a Rider*, I want to search for rides on my route and date so that I can find a suitable carpool.
- *As a Rider*, I want to see the driver's rating and vehicle info before booking so that I can trust the ride.
- *As a Driver*, I want to publish my regular commute so that I can find riders to split fuel cost with.
- *As a Driver*, I want to accept/reject booking requests so that I control who joins my ride.
- *As a user*, I want to rate my co-passenger after the trip so that the platform stays trustworthy.
- *As an Admin*, I want to suspend a reported user so that the platform stays safe.

## 7. Constraints & Assumptions

- Built with Java, Spring Boot, Thymeleaf, HTML/CSS/JS (per finalized tech stack) — no external frontend framework (React/Angular) in v1
- Single-region deployment initially (no multi-region/geo-distribution needs at MVP stage)
- No real-time payment processing in MVP — cost settlement assumed offline (cash) unless payment module is added later
- Seat/booking data consistency handled at the application/transaction level, not via external message queues (not needed at this scale)

## 8. Success Metrics (for MVP)

- Number of rides published per week
- Booking-to-search conversion rate
- Average driver/rider rating
- Repeat usage rate (same user booking again within 30 days)