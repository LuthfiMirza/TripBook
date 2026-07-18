# PLAN.md — TripBook

**Project:** Flight & Hotel Booking Platform
**Target role:** Software Engineer Intern @ Tiket.com (Backend)
**Timeline:** 4 weeks
**Owner:** Luthfi Mirza Darsono

---

## HOW TO USE THIS FILE

This file is a sequential execution plan for Claude Code.

**Rules — do not break these:**

1. **One prompt per session.** Copy exactly one `PROMPT` block into Claude Code. Do not combine.
2. **Build prompts and AUDIT prompts are separate messages.** Never send them together.
3. **No phase advances without evidence.** Every `AUDIT` block requires real terminal output (curl responses, test results, container logs) pasted back. "It should work" is not evidence.
4. **If an audit fails, fix before advancing.** Do not stack phases on a broken foundation.
5. Prompts are written in English on purpose. Keep them in English when pasting.

**Progress tracker** — tick only after the audit passes:

- [x] Phase 0 — Repository & environment scaffold
- [x] Phase 1 — Domain model & database schema
- [x] Phase 2 — Authentication (JWT)
- [x] Phase 3 — Flight & hotel CRUD + search
- [x] Phase 4 — Booking with pessimistic locking
- [x] Phase 5 — Redis caching
- [x] Phase 6 — Horizontal scaling (nginx + 2 instances)
- [x] Phase 7 — Concurrency proof (load test)
- [x] Phase 8 — Kafka event-driven notifications
- [x] Phase 9 — Frontend scaffold & auth
- [x] Phase 10 — Frontend search & results
- [x] Phase 11 — Frontend booking flow & my bookings
- [ ] Phase 12 — Frontend polish & animation
- [ ] Phase 13 — Unit tests
- [ ] Phase 14 — Documentation & architecture diagram

---

## ARCHITECTURE (target end state)

```
Browser (Next.js 14, Vercel)
        |
     nginx  (round-robin load balancer, :8080)
      /   \
backend-1  backend-2   (Spring Boot 3, identical, stateless)
      \   /
   +----+-------+---------+
Postgres     Redis      Kafka
                          |
                notification-consumer
```

**Design constraints that must hold throughout:**

- Backend instances are **stateless** — no in-memory session, no in-memory cache of shared state. JWT for auth, Redis for shared cache.
- Seat/room locking happens at the **database level**, never with Java `synchronized` — `synchronized` only works within one JVM and breaks the moment there are 2 instances.
- Booking response must not wait on notification delivery — that goes through Kafka.

---

## TECH STACK (locked — do not substitute)

| Layer | Choice |
|---|---|
| Backend | Java 17, Spring Boot 3.x, Maven |
| Security | Spring Security + JWT (jjwt) |
| Persistence | Spring Data JPA, Flyway migrations |
| Database | PostgreSQL 16 |
| Cache | Redis 7 (Spring Data Redis) |
| Broker | Kafka (Spring for Apache Kafka) |
| Load balancer | nginx (reverse proxy) |
| Containers | Docker Compose |
| Frontend | Next.js 14 App Router, TypeScript, Tailwind, Framer Motion |
| Testing | JUnit 5, Mockito |

**Explicitly out of scope** (must be documented in README as conscious decisions, not hidden):
3D seat rendering, real payment gateway, ElasticSearch, MongoDB, Cassandra, Kubernetes, multi-region deploy.

---

## FRONTEND DESIGN REFERENCE (locked)

**Formula:** Tiket.com **color palette** + voldogfood.com **layout & motion** + a
flight (airplane) theme. Reference site to study: https://www.voldogfood.com

**Palette (Tiket.com direction — do NOT switch to Voldog's olive/lime):**
- Primary blue: `#2563EB`  | Primary dark: `#1D4ED8`  | Primary tint bg: `#EFF6FF`
- Accent (for CTAs/pills, the role Voldog's lime plays): a warm accent —
  `#FACC15` (amber) used sparingly on the main CTA button only
- Background: `#F8FAFC`  | Section alt block: `#EEF2FF` (indigo-50) and `#FFFFFF`
- Text: `#0F172A` (slate-900) headings, `#475569` (slate-600) body
- Card: white, `rounded-2xl`, soft shadow

**Layout patterns to copy from Voldog (this is the whole point):**
1. **Full-bleed hero as a solid color block** (primary blue), giant wordmark
   "TripBook" centered and bold, with a **transparent-PNG airplane cut-out that
   overflows the bottom edge** of the hero block (Voldog does this with a dog).
2. **Floating pill navbar**: rounded, translucent, with a `rounded-full` accent
   CTA button and a circular hamburger on the right.
3. **Segmented search/finder bar overlapping the hero's bottom edge** — Voldog's
   multi-field finder maps directly onto our Flight/Hotel search
   (origin · destination · date · passengers · [Search]).
4. **Two-tab pill toggle** (Voldog's Dog/Cat) → **Flight / Hotel**.
5. **Alternating colored section blocks** separated by **curved cutout dividers**
   (SVG / clip-path), not straight lines. Color rhythm: blue → white → indigo-50.
6. **Horizontal card carousel** with a circular arrow nav button (Voldog's
   product row) → "Explore Top Destinations" and flight-result rows.
7. **Hand-drawn decorative accents** (arrow scribble, curved underline) — inline
   SVG, used sparingly to add playfulness. Recolor to blue/amber, not red.

**Motion (Framer Motion, matches Voldog's feel — see Phase 12 for full spec):**
- Sections fade + slide-up on scroll (`whileInView`, `once: true`)
- Staggered children on card grids
- Hover lift on cards; scale-pop on seat select
- Hero subject (airplane) subtle float loop
- Respect `prefers-reduced-motion`

**ASSETS status:**
- [ ] `frontend/public/hero/plane.png` — **STILL NEEDED from owner.** High-res
  (≥2000px) transparent-PNG airplane cut-out, ¾/diagonal angle, to overflow the
  hero block. Optional `plane-2.png`. Sourcing notes in
  `frontend/public/hero/README.md`. This is the only asset Claude cannot fetch.
- [x] `frontend/public/destinations/{dps,cgk,jog,sub,upg,kno}.webp` — DONE. Real
  city photos (Ulun Danu Bratan, Monas, Borobudur, Suramadu, Losari, Lake Toba),
  cropped 3:4, WebP, ~1000×1334. Sourced from Wikimedia Commons; licenses &
  attribution recorded in `frontend/public/destinations/CREDITS.md` (mostly
  CC-BY-SA — the attribution must appear in the app footer or an /about page).
- [x] Display font — LOCKED to **Space Grotesk** (variable TTF at
  `frontend/public/fonts/SpaceGrotesk.ttf`, OFL license alongside). Self-host via
  `next/font/local` for the "TripBook" wordmark + headings. **Inter stays for
  body text.**
- Airlines: use **text chips** (Garuda, Lion Air, Citilink, Batik, AirAsia) —
  do NOT ship real airline logos (copyright). If logos are truly wanted, owner
  supplies self-made SVGs and accepts the risk.
- Generated in code, no owner action: hand-drawn SVG accents, curved section
  cutouts, icons (lucide-react), favicon + OG image (Phase 12).

---

# WEEK 1 — BACKEND FOUNDATION

## Phase 0 — Repository & environment scaffold

### PROMPT 0.1 — Build

```
Create a new project repository called "tripbook" with this structure:

tripbook/
  backend/          <- Spring Boot 3 app, Java 17, Maven
  frontend/         <- placeholder folder, empty for now
  infra/
    docker-compose.yml
    nginx/
  docs/
  README.md
  .gitignore

For backend/, generate a Spring Boot 3 Maven project with:
- groupId: com.tripbook
- artifactId: tripbook-backend
- Java 17
- Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa,
  spring-boot-starter-validation, postgresql driver, flyway-core,
  spring-boot-starter-test, lombok

Create the package structure under com.tripbook:
  config/
  controller/
  service/
  repository/
  entity/
  dto/
  exception/

For infra/docker-compose.yml, define ONE service only for now:
- postgres:16 named "tripbook-postgres", port 5432, database "tripbook",
  user "tripbook", password "tripbook", with a named volume for persistence

Configure backend/src/main/resources/application.yml to connect to that Postgres.
Set spring.jpa.hibernate.ddl-auto to "validate" (Flyway owns the schema, not Hibernate).

Add a health check controller: GET /api/health returning {"status":"UP"}.

Write a .gitignore appropriate for Java/Maven + Node.

Do NOT create any entities, security config, or business logic yet.
```

### PROMPT 0.2 — Audit

```
Verify Phase 0 with real evidence. Run these and show me the ACTUAL terminal output:

1. cd infra && docker compose up -d
2. docker compose ps                        (postgres must be healthy/running)
3. cd ../backend && ./mvnw clean compile    (must succeed)
4. ./mvnw spring-boot:run                   (must start without errors)
5. In another shell: curl -i http://localhost:8080/api/health

Paste the real output of every command. If any step fails, report the exact
error and stop — do not attempt to advance to the next phase.

Confirm explicitly: did the app connect to Postgres successfully?
```

---

## Phase 1 — Domain model & database schema

### PROMPT 1.1 — Build

```
Implement the TripBook domain model using Flyway migrations and JPA entities.

Create Flyway migration V1__init_schema.sql under
backend/src/main/resources/db/migration with these tables:

users
  id BIGSERIAL PK, email VARCHAR UNIQUE NOT NULL, password_hash VARCHAR NOT NULL,
  full_name VARCHAR NOT NULL, role VARCHAR NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP NOT NULL DEFAULT now()

flights
  id BIGSERIAL PK, flight_code VARCHAR UNIQUE NOT NULL, airline VARCHAR NOT NULL,
  origin VARCHAR NOT NULL, destination VARCHAR NOT NULL,
  departure_time TIMESTAMP NOT NULL, arrival_time TIMESTAMP NOT NULL,
  price NUMERIC(12,2) NOT NULL, total_seats INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
  INDEX on (origin, destination, departure_time)

flight_seats
  id BIGSERIAL PK, flight_id BIGINT FK -> flights, seat_number VARCHAR NOT NULL,
  seat_class VARCHAR NOT NULL, status VARCHAR NOT NULL DEFAULT 'AVAILABLE',
  version BIGINT NOT NULL DEFAULT 0
  UNIQUE (flight_id, seat_number)

hotels
  id BIGSERIAL PK, name VARCHAR NOT NULL, city VARCHAR NOT NULL,
  address VARCHAR, price_per_night NUMERIC(12,2) NOT NULL,
  star_rating INT, created_at TIMESTAMP NOT NULL DEFAULT now()
  INDEX on (city)

hotel_rooms
  id BIGSERIAL PK, hotel_id BIGINT FK -> hotels, room_number VARCHAR NOT NULL,
  room_type VARCHAR NOT NULL, status VARCHAR NOT NULL DEFAULT 'AVAILABLE',
  version BIGINT NOT NULL DEFAULT 0
  UNIQUE (hotel_id, room_number)

bookings
  id BIGSERIAL PK, booking_reference VARCHAR UNIQUE NOT NULL,
  user_id BIGINT FK -> users, booking_type VARCHAR NOT NULL,  -- FLIGHT | HOTEL
  flight_seat_id BIGINT NULL FK -> flight_seats,
  hotel_room_id BIGINT NULL FK -> hotel_rooms,
  check_in DATE NULL, check_out DATE NULL,
  total_price NUMERIC(12,2) NOT NULL,
  status VARCHAR NOT NULL DEFAULT 'PENDING',  -- PENDING | CONFIRMED | CANCELLED
  created_at TIMESTAMP NOT NULL DEFAULT now()
  INDEX on (user_id)

Then create matching JPA entities in com.tripbook.entity using Lombok,
and Spring Data repositories in com.tripbook.repository for each.

Add a Flyway migration V2__seed_data.sql that inserts realistic Indonesian
travel data: at least 15 flights (CGK, DPS, SUB, UPG, JOG, KNO routes with
real airline names), seats for each flight, at least 10 hotels across those
cities, and rooms for each hotel.

Do NOT implement any controllers or services in this phase.
```

### PROMPT 1.2 — Audit

```
Verify Phase 1 with real evidence. Show ACTUAL output for:

1. ./mvnw clean compile
2. Restart the app and show the Flyway migration log lines
3. docker exec -it tripbook-postgres psql -U tripbook -d tripbook -c "\dt"
4. Run these queries and show real results:
   SELECT count(*) FROM flights;
   SELECT count(*) FROM flight_seats;
   SELECT count(*) FROM hotels;
   SELECT count(*) FROM hotel_rooms;
   SELECT * FROM flights LIMIT 3;

Confirm: did ddl-auto=validate pass, meaning entities match the Flyway schema
exactly? If Hibernate reported any schema mismatch, show the error and fix it
before we advance.
```

---

## Phase 2 — Authentication (JWT)

### PROMPT 2.1 — Build

```
Implement stateless JWT authentication in the Spring Boot backend.

Add dependencies: spring-boot-starter-security, io.jsonwebtoken jjwt-api,
jjwt-impl, jjwt-jackson.

Implement:
- SecurityConfig: stateless session policy (SessionCreationPolicy.STATELESS),
  CSRF disabled, CORS enabled for http://localhost:3000
- Public endpoints: /api/health, /api/auth/**, GET /api/flights/**,
  GET /api/hotels/**
- All other endpoints require authentication
- JwtService: generate and validate tokens, HS256, secret from application.yml,
  24h expiry, subject = user email, claim for role
- JwtAuthenticationFilter: OncePerRequestFilter reading the Authorization
  Bearer header
- CustomUserDetailsService loading users from the users table
- BCryptPasswordEncoder for password hashing

Endpoints:
- POST /api/auth/register  {email, password, fullName} -> 201, returns user info
- POST /api/auth/login     {email, password} -> 200, returns {token, user}
- GET  /api/auth/me        -> 200, returns current user from the JWT

Use DTOs with Bean Validation (@Email, @NotBlank, @Size min 8 for password).
Never return password_hash in any response.

CRITICAL CONSTRAINT: authentication must be fully stateless. No HttpSession,
no in-memory user store, no server-side session state. This project will run
2 backend instances behind a load balancer, so any request must be servable
by any instance.

Also add a GlobalExceptionHandler (@RestControllerAdvice) returning a
consistent error shape: {timestamp, status, error, message, path}.
Handle validation errors (400), auth errors (401), not found (404),
and a generic 500 fallback.
```

### PROMPT 2.2 — Audit

```
Verify Phase 2 with real evidence. Run and show ACTUAL curl output for:

1. Register a user:
   curl -i -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"email":"test@tripbook.com","password":"password123","fullName":"Test User"}'

2. Register the SAME email again (must fail cleanly, not 500)

3. Register with an invalid email and a 3-char password
   (must return 400 with field-level validation messages)

4. Login:
   curl -i -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"test@tripbook.com","password":"password123"}'

5. Login with the wrong password (must return 401, not 500)

6. Call /api/auth/me WITHOUT a token (must return 401)

7. Call /api/auth/me WITH the token from step 4 (must return 200 + user info)

8. Show the users table row: confirm password_hash is BCrypt, not plaintext.

Also confirm explicitly: is there any HttpSession or in-memory state in the
auth path? Search the code and tell me yes or no.
```

---

## Phase 3 — Flight & hotel CRUD + search

### PROMPT 3.1 — Build

```
Implement flight and hotel endpoints in the Spring Boot backend.

Public read endpoints:
- GET /api/flights/search?origin=CGK&destination=DPS&date=2026-08-01&passengers=2
  Returns flights matching route + date, with availableSeats computed from
  flight_seats where status='AVAILABLE'. Only return flights with enough
  available seats for the passenger count.
- GET /api/flights/{id}          -> flight detail + seat map (list of seats
                                    with seatNumber, seatClass, status)
- GET /api/hotels/search?city=Bali&checkIn=2026-08-01&checkOut=2026-08-03&guests=2
  Returns hotels in that city with availableRooms count
- GET /api/hotels/{id}           -> hotel detail + room list

Admin-only write endpoints (require role ADMIN):
- POST/PUT/DELETE /api/admin/flights
- POST/PUT/DELETE /api/admin/hotels
When a flight is created, auto-generate its flight_seats rows based on
total_seats (e.g. rows of 6 seats: 1A-1F, 2A-2F, first 2 rows BUSINESS,
rest ECONOMY).

Requirements:
- Use DTOs, never expose entities directly
- Validate query params (@RequestParam with validation; date must not be in
  the past)
- Support sorting on search: ?sort=price_asc | price_desc | departure_asc
- Support pagination: ?page=0&size=20, return {content, page, size, totalElements}
- Search queries must use the indexes defined in V1 — no N+1 queries.
  Use JPQL joins or projections.

Do NOT add caching in this phase. Do NOT implement booking yet.
```

### PROMPT 3.2 — Audit

```
Verify Phase 3 with real evidence. Show ACTUAL curl output for:

1. curl "http://localhost:8080/api/flights/search?origin=CGK&destination=DPS&date=2026-08-01&passengers=2"
2. curl "http://localhost:8080/api/flights/search?origin=CGK&destination=DPS&date=2026-08-01&sort=price_asc"
   (confirm results are actually sorted by price ascending)
3. curl "http://localhost:8080/api/flights/1"  (must include a seat map)
4. curl "http://localhost:8080/api/hotels/search?city=Bali&checkIn=2026-08-01&checkOut=2026-08-03&guests=2"
5. curl "http://localhost:8080/api/flights/search?origin=CGK&destination=DPS&date=2020-01-01"
   (past date -> must return 400, not results)
6. POST /api/admin/flights with a USER-role token (must return 403)

7. N+1 check: enable
   logging.level.org.hibernate.SQL=DEBUG
   restart, run the flight search once, and show me the SQL log.
   Count the queries. If the search fires one query per flight to count seats,
   that is an N+1 problem — report it and fix it before advancing.

Tell me the exact number of SQL queries the search endpoint executed.
```

---

# WEEK 2 — BOOKING, CONCURRENCY, CACHE, LOAD BALANCER

## Phase 4 — Booking with pessimistic locking

### PROMPT 4.1 — Build

```
Implement booking with database-level locking to prevent double-booking.

Endpoints (all require authentication):
- POST /api/bookings/flight  {flightId, seatNumber, passengerName}
- POST /api/bookings/hotel   {hotelId, roomNumber, checkIn, checkOut, guestName}
- GET  /api/bookings         -> current user's bookings (paginated, newest first)
- GET  /api/bookings/{reference} -> single booking (404 if not owned by user)
- POST /api/bookings/{reference}/cancel -> releases the seat/room back to AVAILABLE

Booking flow for flights:
1. Open a transaction
2. Load the flight_seat row with PESSIMISTIC_WRITE lock:
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT s FROM FlightSeat s WHERE s.id = :id")
   Optional<FlightSeat> findByIdForUpdate(@Param("id") Long id);
3. If status != AVAILABLE -> throw SeatUnavailableException (409 Conflict)
4. Set status = BOOKED
5. Create the booking row, status CONFIRMED, generate a booking_reference
   (format: TB-XXXXXXXX, uppercase alphanumeric)
6. Commit

Same pattern for hotel rooms.

CRITICAL CONSTRAINTS — these are the whole point of this phase:
- Locking MUST be at the database level (SELECT ... FOR UPDATE via
  PESSIMISTIC_WRITE). Do NOT use Java `synchronized`, ReentrantLock, or any
  JVM-local locking. This app runs 2 instances; JVM locks do not work across
  instances.
- Set a lock timeout (javax.persistence.lock.timeout hint, 3000ms) so a stuck
  transaction cannot hang requests forever.
- The whole booking must be one atomic transaction (@Transactional).
- Cancel must also lock the seat/room row before releasing it.

Add SeatUnavailableException -> 409 in the GlobalExceptionHandler.

In a code comment above the locking repository method, write a short note
explaining WHY pessimistic locking was chosen over optimistic locking here
(high contention on the last seats; retry storms with optimistic locking).
```

### PROMPT 4.2 — Audit

```
Verify Phase 4 with real evidence. Show ACTUAL output for:

1. Login and get a token.
2. Book a specific seat:
   curl -i -X POST http://localhost:8080/api/bookings/flight \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"flightId":1,"seatNumber":"1A","passengerName":"Test User"}'
   Must return 201 with a booking_reference.
3. Book the SAME seat again -> must return 409, NOT 500, NOT 201.
4. Show the DB row: SELECT status FROM flight_seats WHERE flight_id=1 AND seat_number='1A';
   Must be 'BOOKED'.
5. GET /api/bookings with the token -> must show the booking.
6. GET /api/bookings/{reference} with a DIFFERENT user's token -> must 404 or 403,
   never leak another user's booking.
7. Cancel the booking, then show the seat row again -> must be back to 'AVAILABLE'.
8. Book the same seat again after cancel -> must now succeed (201).

9. Prove the lock is real: enable SQL logging and show me the log line for the
   seat load during booking. It MUST contain "for update". Paste that line.

10. Search the entire codebase for `synchronized`, `ReentrantLock`, and
    `static` mutable state in the booking path. Report what you find.
    If any exist in the booking path, remove them and explain why.
```

---

## Phase 5 — Redis caching

### PROMPT 5.1 — Build

```
Add Redis caching to the search endpoints.

1. Add redis:7-alpine to infra/docker-compose.yml as "tripbook-redis", port 6379.
2. Add spring-boot-starter-data-redis and configure the connection in
   application.yml.
3. Enable caching with @EnableCaching and configure a RedisCacheManager with
   JSON serialization (GenericJackson2JsonRedisSerializer) — not JDK
   serialization.

Cache these:
- Flight search results: cache name "flightSearch", key derived from
  origin+destination+date+passengers+sort+page, TTL 5 minutes
- Hotel search results: cache name "hotelSearch", TTL 5 minutes
- Flight detail: cache name "flightDetail", key = flight id, TTL 2 minutes
- Hotel detail: cache name "hotelDetail", TTL 2 minutes

Invalidation rules — this is the important part:
- When a booking or cancellation changes seat/room availability, evict the
  affected flight/hotel detail AND the related search caches (@CacheEvict).
  Stale availability is a correctness bug, not just a performance issue.
- When an admin creates/updates/deletes a flight or hotel, evict the relevant
  caches.

CRITICAL CONSTRAINT: the cache MUST live in Redis, not in application memory.
Do NOT use ConcurrentMapCacheManager, Caffeine, or any local cache. With 2
backend instances, a local cache would mean instance-1 serves stale
availability after instance-2 processes a booking.

Add a code comment in the cache config explaining that decision.
```

### PROMPT 5.2 — Audit

```
Verify Phase 5 with real evidence. Show ACTUAL output for:

1. docker compose ps  (redis must be running)
2. Run a flight search twice. Show the SQL log for BOTH calls.
   First call must hit the DB; second call must produce NO SQL query.
3. Show the keys landing in Redis:
   docker exec -it tripbook-redis redis-cli KEYS '*'
4. Inspect one cached value:
   docker exec -it tripbook-redis redis-cli GET "<one of the keys>"
   Confirm it is readable JSON, not JDK-serialized binary garbage.
5. Check TTL: docker exec -it tripbook-redis redis-cli TTL "<key>"
   Must be a positive number ≈ 300.

6. Invalidation proof — this is the one that matters:
   a. Search flight 1, note availableSeats.
   b. Book a seat on flight 1.
   c. Search flight 1 again immediately.
   d. availableSeats MUST have decreased. If it shows the stale number,
      invalidation is broken — report it and fix it.
   Show the actual before/after JSON.

7. Search the codebase for ConcurrentMapCacheManager or Caffeine.
   Confirm none are used.
```

---

## Phase 6 — Horizontal scaling (nginx + 2 instances)

### PROMPT 6.1 — Build

```
Make the backend run as 2 identical instances behind an nginx load balancer.

1. Add a Dockerfile for the backend (multi-stage: maven build stage, then a
   slim JRE 17 runtime stage).

2. Update infra/docker-compose.yml to define:
   - postgres (existing)
   - redis (existing)
   - backend-1: built from backend/, INSTANCE_ID=backend-1, internal port 8080
   - backend-2: built from backend/, INSTANCE_ID=backend-2, internal port 8080
   - nginx: image nginx:alpine, exposes host port 8080, config mounted from
     infra/nginx/nginx.conf
   Both backends share the SAME postgres and redis.

3. infra/nginx/nginx.conf:
   - upstream block with backend-1:8080 and backend-2:8080, round-robin
     (default, no ip_hash — the app is stateless, so sticky sessions are not
     needed and using them would hide statefulness bugs)
   - proxy_pass to the upstream
   - forward X-Real-IP and X-Forwarded-For
   - proxy_next_upstream on error/timeout so a dead instance fails over

4. In the backend, add:
   - An INSTANCE_ID env var read into the app
   - Response header "X-Instance-Id" on every response (via a filter or
     interceptor), so we can prove which instance served each request
   - Include instanceId in the /api/health response body

5. Update the frontend base URL target to http://localhost:8080 (nginx),
   never directly to a backend instance.

Do NOT add sticky sessions. Do NOT add any instance-local state.
```

### PROMPT 6.2 — Audit

```
Verify Phase 6 with real evidence. Show ACTUAL output for:

1. cd infra && docker compose up -d --build
2. docker compose ps  (nginx + backend-1 + backend-2 + postgres + redis all up)

3. Prove load balancing is real:
   for i in {1..10}; do curl -s -i http://localhost:8080/api/health | grep -i x-instance-id; done
   Show all 10 lines. Both backend-1 and backend-2 MUST appear.

4. Prove statelessness across instances:
   a. Login through nginx, capture the token, note which instance served it.
   b. Call /api/auth/me repeatedly with that token until you can show it being
      served by the OTHER instance. It must return 200 both times.
   Show the actual output with the X-Instance-Id headers visible.
   If the token only works on the instance that issued it, auth is stateful —
   report it and stop.

5. Prove shared cache:
   a. Run a flight search (note the serving instance).
   b. Run the same search until the OTHER instance serves it.
   c. Show the SQL logs of both containers:
      docker compose logs backend-1 | grep -i select | tail
      docker compose logs backend-2 | grep -i select | tail
   The second instance must serve from Redis WITHOUT its own DB query.

6. Prove failover:
   docker compose stop backend-2
   then run 10 health requests -> all must still return 200 from backend-1.
   Show the output. Then bring backend-2 back up.

Report honestly: did anything break when there were 2 instances? Any bug found
here is the most valuable interview material in this project — document it.
```

---

## Phase 7 — Concurrency proof (load test)

### PROMPT 7.1 — Build

```
Build a reproducible concurrency test that proves the locking works under a
real race, across 2 instances behind the load balancer.

Create scripts/concurrency-test.sh that:
1. Resets a specific flight seat to AVAILABLE (via psql).
2. Registers/logs in N=50 distinct users, collecting 50 JWTs.
3. Fires 50 SIMULTANEOUS POST /api/bookings/flight requests through nginx
   (localhost:8080), ALL targeting the SAME seat on the SAME flight.
   Use xargs -P 50 or GNU parallel — they must be genuinely concurrent, not
   sequential.
4. Collects every HTTP status code returned.
5. Prints a summary:
   - count of 201 responses  (MUST be exactly 1)
   - count of 409 responses  (MUST be 49)
   - count of 500 responses  (MUST be 0)
   - which instances served the requests (from X-Instance-Id)
6. Verifies the DB afterwards:
   SELECT count(*) FROM bookings WHERE flight_seat_id = <that seat>;
   MUST be exactly 1.
7. Exits non-zero if any assertion fails.

Save the full output to docs/concurrency-test-result.txt — this file is
interview evidence, so it must be real captured output, never hand-written.
```

### PROMPT 7.2 — Audit

```
Run the concurrency test and show me the COMPLETE real output.

bash scripts/concurrency-test.sh

Required results:
- exactly 1 x HTTP 201
- exactly 49 x HTTP 409
- 0 x HTTP 500
- exactly 1 booking row in the DB for that seat
- both backend-1 and backend-2 appear among the servers

Then answer these, based on what actually happened — not on theory:
1. Did any request return 500? If so, why? (Lock timeout? Deadlock?)
2. What was the slowest request? Did the lock create a queue?
3. Run it 3 times in a row. Is the result stable every time?
   Show all 3 runs.
4. Now stop backend-2 and run it again with only 1 instance.
   Compare the results. Same guarantee?

If exactly 1 success does not hold, the locking is broken. Report it, fix it,
and re-run — do not advance.

Confirm docs/concurrency-test-result.txt contains real captured output.
```

---

# WEEK 3 — KAFKA + FRONTEND CORE

## Phase 8 — Kafka event-driven notifications

### PROMPT 8.1 — Build

```
Add event-driven notifications with Kafka.

1. Add kafka + zookeeper to infra/docker-compose.yml (confluentinc images),
   Kafka reachable at kafka:9092 inside the network.

2. Add spring-kafka to the backend.

3. Producer: after a booking transaction COMMITS successfully, publish a
   BookingConfirmedEvent to topic "booking-events":
   {bookingReference, userId, userEmail, bookingType, itemSummary,
    totalPrice, occurredAt}
   Use JSON serialization.

   CRITICAL: publish AFTER commit, not inside the transaction. Use
   @TransactionalEventListener(phase = AFTER_COMMIT). If we publish inside the
   transaction and the transaction later rolls back, we would have announced a
   booking that does not exist.

   CRITICAL: the booking HTTP response must NOT wait for Kafka. If the broker
   is down, booking must still succeed. Make the send non-blocking and log
   failures — do not propagate the exception to the user.

4. Consumer: a @KafkaListener on "booking-events", group "notification-service",
   that simulates sending an email by logging:
   "[NOTIFICATION] Email sent to {email}: Booking {reference} confirmed"
   Add an artificial 2-second delay to make the async nature visible in the logs.

5. Error handling: if the consumer throws, retry 3 times with backoff, then
   write the failed event to a "failed_events" table (new Flyway migration V3)
   with the payload, error message, and timestamp. Do not lose the event.

6. Both backend-1 and backend-2 run the consumer in the SAME consumer group,
   so Kafka distributes partitions between them — do not let both process the
   same event twice.
```

### PROMPT 8.2 — Audit

```
Verify Phase 8 with real evidence. Show ACTUAL output for:

1. docker compose up -d --build && docker compose ps  (kafka up)

2. Make a booking through nginx. Show:
   a. The curl response WITH timing:
      curl -w "\ntime_total: %{time_total}\n" ...
      The response must return in well under 2 seconds — proving it did not
      wait for the notification.
   b. The consumer log line appearing ~2s LATER:
      docker compose logs -f backend-1 backend-2 | grep NOTIFICATION
   Show both, with timestamps, so the async gap is visible.

3. Verify the event landed in the topic:
   docker exec -it <kafka-container> kafka-console-consumer \
     --bootstrap-server localhost:9092 --topic booking-events --from-beginning --max-messages 5

4. Prove no duplicate processing:
   Make 5 bookings, then grep NOTIFICATION across BOTH backends.
   There must be exactly 5 notification lines total across both instances —
   not 10. Show the counts per instance.

5. Prove booking survives a broker outage — this is the key test:
   a. docker compose stop kafka
   b. Make a booking. It MUST still return 201.
   c. Show the booking row exists in the DB.
   d. Show the error was logged, not thrown to the user.
   e. docker compose start kafka
   Paste the real output of every step.

6. Prove AFTER_COMMIT ordering: show the code and confirm the publish happens
   after commit. Then force a booking failure (409 on a taken seat) and confirm
   NO event was published for it.
```

---

## Phase 9 — Frontend scaffold & auth

### PROMPT 9.1 — Build

```
Scaffold the TripBook frontend.

In frontend/, create a Next.js 14 project:
- App Router, TypeScript, Tailwind CSS, ESLint
- Add framer-motion, lucide-react, zod, react-hook-form
- NO Three.js, NO 3D libraries — out of scope by decision

Structure:
  app/
    layout.tsx
    page.tsx                 (landing — placeholder for now)
    (auth)/login/page.tsx
    (auth)/register/page.tsx
    search/page.tsx          (placeholder)
    bookings/page.tsx        (placeholder)
  components/ui/             (Button, Input, Card, Badge — reusable primitives)
  lib/api.ts                 (typed fetch wrapper)
  lib/auth.ts
  types/index.ts             (TypeScript types mirroring the backend DTOs)

lib/api.ts requirements:
- Base URL from NEXT_PUBLIC_API_URL, defaulting to http://localhost:8080
  (nginx — never a backend instance directly)
- Typed request helper with generics
- Attaches the JWT automatically
- Throws a typed ApiError carrying the backend's error shape
  {timestamp, status, error, message, path}

Auth implementation:
- Login and register pages with react-hook-form + zod validation
- On successful login, store the JWT in an httpOnly cookie via a Next.js Route
  Handler (app/api/auth/session/route.ts) — NOT localStorage
- Middleware protecting /bookings and the booking flow, redirecting to /login
- A useAuth hook exposing the current user

Design tokens (define in Tailwind config, use everywhere from here on).
These follow the FRONTEND DESIGN REFERENCE section at the top of this file —
Tiket.com palette + voldogfood.com layout, airplane theme:
- Primary blue: #2563EB   | Primary dark: #1D4ED8   | Primary tint: #EFF6FF
- Accent (main CTA only): #FACC15 (amber) — this plays the role Voldog's lime does
- Background: #F8FAFC   | Section alt blocks: #FFFFFF and #EEF2FF
- Text: #0F172A headings, #475569 body
- Card: white, rounded-2xl, subtle shadow
- Body font: Inter. Display/wordmark font: **Space Grotesk** (already downloaded
  to frontend/public/fonts/SpaceGrotesk.ttf) — self-host via next/font/local.
- Generous whitespace, clean and airy — reference is a modern flight-search
  landing page, not a dashboard

Build pages functional first. Polish comes in Phase 12. But already wire up the
navbar and page shell as a floating pill navbar (rounded, blurred bg, rounded-full
CTA, circular hamburger) per the design reference — the layout scaffolding is
cheaper to get right now than to retrofit in Phase 12.
```

### PROMPT 9.2 — Audit

```
Verify Phase 9 with real evidence:

1. cd frontend && npm run build   (must succeed, zero TypeScript errors — show output)
2. npm run dev, then:
   - Register a new user through the UI. Show the network request/response.
   - Confirm the user row appeared in Postgres (show the psql query output).
   - Log in. Show that the JWT is in an httpOnly cookie — open DevTools >
     Application > Cookies and confirm HttpOnly is checked. Confirm
     localStorage is EMPTY of any token.
   - Visit /bookings while logged out -> must redirect to /login.
   - Visit /bookings while logged in -> must render.
3. Show that requests go to http://localhost:8080 (nginx). Grep the codebase
   for any hardcoded backend-1/backend-2 URL — there must be none.
4. Trigger an error (login with wrong password) and confirm the UI shows the
   backend's message, not a raw stack trace or "undefined".
```

---

## Phase 10 — Frontend search & results

### PROMPT 10.1 — Build

```
Implement the search experience in the frontend.

Follow the FRONTEND DESIGN REFERENCE at the top of this file: Tiket.com blue
palette, voldogfood.com layout & motion, airplane theme.

Landing page (app/page.tsx) — Voldog-style layout, top to bottom:

- HERO as a full-bleed solid PRIMARY BLUE block:
  - Giant bold wordmark "TripBook" centered (display font), Voldog-scale
  - Subheadline under it
  - The transparent-PNG airplane (frontend/public/hero/plane.png) positioned to
    OVERFLOW the bottom edge of the blue block — the plane breaks out of the hero
    the way Voldog's dog does. Subtle float-loop animation (Phase 12).
  - A hand-drawn SVG accent (arrow scribble or curved underline) near the headline
- SEGMENTED SEARCH BAR overlapping the hero's bottom edge (the Voldog finder):
  - Two-tab pill toggle: Flight | Hotel
  - Flight tab fields inline in one bar: origin, destination (autocomplete from a
    static Indonesian city/airport list), departure date, passenger count, and a
    rounded-full accent Search button at the right end
  - Hotel tab: city, check-in, check-out, guests, Search button
- Sections below alternate color blocks (white / indigo-50) separated by CURVED
  CUTOUT dividers (SVG / clip-path), not straight lines:
  - "Explore Top Destinations": a horizontal card CAROUSEL with a circular arrow
    nav button (Voldog product row). 4-6 cards: city image from
    frontend/public/destinations/*.webp, name, date range, "price from". Use the
    LOCAL downloaded images — do NOT hotlink Unsplash Source URLs.
  - "Why book with TripBook" or a benefits strip: small cards with icon + text,
    optional floating badge card offset over an image (Voldog benefit card style)
  - "Popular Airlines": simple grid of airline name TEXT chips (Garuda, Lion Air,
    Citilink, Batik, AirAsia) — no real logos.
- Footer

Search results page (app/search/page.tsx):
- Reads query params, calls the backend search endpoint
- Left sidebar filters: price range slider, airline checkboxes, departure time
  buckets (morning/afternoon/evening)
- Result list: each card shows airline, flight code, departure/arrival time,
  duration, price, available seats, and a Select button
- Sort dropdown: cheapest / earliest departure
- Skeleton loading state while fetching
- Empty state when no results
- Error state when the API fails
- URL params stay in sync with filters (shareable/refreshable URLs)

Requirements:
- Use Server Components for the initial data fetch where sensible; Client
  Components only where interactivity requires it
- Type everything against types/index.ts — no `any`
- Responsive: mobile-first, sidebar collapses to a filter drawer on mobile
```

### PROMPT 10.2 — Audit

```
Verify Phase 10 with real evidence:

1. npm run build  (must pass, zero TS errors — show output)
2. Search CGK -> DPS through the UI. Show:
   - The network request URL and the response JSON
   - A screenshot description of the rendered results
   Confirm the data shown matches what curl returns for the same query.
3. Apply a price filter and confirm the URL updates and results change.
4. Change sort to cheapest — confirm the list reorders correctly.
5. Search a route with no flights -> confirm the empty state renders,
   not a crash or infinite spinner.
6. Stop the backend (docker compose stop backend-1 backend-2), search again ->
   confirm the error state renders gracefully.  Restart afterwards.
7. Test at 375px width — confirm the filter drawer works and nothing overflows.
8. Grep for `any` in frontend/ — report every occurrence and justify or fix it.
```

---

## Phase 11 — Frontend booking flow & my bookings

### PROMPT 11.1 — Build

```
Implement the booking flow in the frontend.

Flight detail / seat selection (app/flights/[id]/page.tsx):
- Flight summary header (airline, route, times, price)
- Seat map: a 2D CSS grid, rows of 6 seats with an aisle gap (A B C | D E F)
  - Available: white with border, clickable
  - Booked: gray, disabled, not clickable
  - Selected: primary blue
  - Business class rows visually distinguished from economy
- IMPORTANT: this is a flat 2D grid. NO 3D rendering, NO Three.js —
  explicitly out of scope.
- Selecting a seat opens a passenger detail form
- Confirm button -> POST /api/bookings/flight

Hotel detail (app/hotels/[id]/page.tsx):
- Hotel summary, room list (type, status), select a room, guest form, confirm

Booking result:
- Success: booking reference displayed prominently, status, price, and a link
  to My Bookings
- 409 conflict: a clear message — "This seat was just booked by someone else.
  Please choose another." — then auto-refresh the seat map. Do NOT show a raw
  error. This is the real race condition surfacing in the UI; handle it
  deliberately.

My Bookings (app/bookings/page.tsx):
- List of the user's bookings, newest first
- Status badges: PENDING (amber), CONFIRMED (green), CANCELLED (gray)
- Cancel button with a confirmation dialog
- After cancelling, the list refreshes and the status updates
- Empty state when there are no bookings

All mutations show a loading state and disable the button while in flight, so
double-clicking cannot fire two booking requests.
```

### PROMPT 11.2 — Audit

```
Verify Phase 11 with real evidence:

1. npm run build (must pass — show output)
2. Complete a full booking through the UI: search -> select flight -> pick seat
   -> fill form -> confirm. Show:
   - The network POST request and response
   - The booking reference shown in the UI
   - The matching row in Postgres (psql output)
3. Confirm the booked seat now renders as gray/disabled on refresh.
4. Race condition UX test — do this for real:
   a. Open the seat map in the browser.
   b. In a terminal, book that exact seat via curl as a different user.
   c. In the browser, try to book the same seat.
   d. Confirm the UI shows the friendly 409 message and refreshes the seat map.
   Show the actual output/behavior.
5. Cancel a booking through the UI. Confirm:
   - The status changes to CANCELLED
   - The seat returns to AVAILABLE in the DB (show psql output)
   - The seat is selectable again in the UI
6. Double-click the confirm button rapidly. Confirm only ONE booking is created
   (show the DB count).
7. Test the whole flow at 375px width.
```

---

# WEEK 4 — POLISH, TESTS, DOCS

## Phase 12 — Frontend polish & animation

### PROMPT 12.1 — Build

```
Polish the TripBook frontend visually. Content and functionality stay the same
— this phase only improves presentation.

Visual direction (per the FRONTEND DESIGN REFERENCE at the top — Tiket.com
palette, voldogfood.com layout & motion, airplane theme):
- Clean, airy, modern travel-booking aesthetic
- Hero: solid primary-blue block with the airplane PNG overflowing its bottom edge
- Alternating section color blocks (white / indigo-50) with CURVED CUTOUT
  dividers between them (SVG / clip-path), like Voldog's section transitions
- Floating pill navbar (rounded, blurred bg, rounded-full accent CTA, circular
  hamburger)
- Cards: rounded-2xl, subtle shadow, hover lift
- Hand-drawn SVG accents (arrow scribble, curved underline) in blue/amber
- Generous whitespace, clear typographic hierarchy
- Inter body, display font for the wordmark, tight tracking on headings

Framer Motion — keep it cheap and tasteful, matching Voldog's feel:
- Fade + slide-up on scroll for sections (whileInView, once: true) — sections
  start empty and reveal on scroll, as Voldog does
- Stagger children on card grids
- Hover lift on cards (scale 1.02, shadow increase)
- Hero airplane: subtle continuous float loop (gentle Y translate)
- Horizontal destination carousel: smooth scroll with the circular arrow button
- Page transitions between routes
- Skeleton shimmer while loading
- Smooth seat selection feedback (scale pop on select)

Hard constraints:
- NO 3D, NO Three.js, NO WebGL, NO heavy scroll-jacking
- Respect prefers-reduced-motion: disable animation when set
- Animation must not delay content becoming interactive
- Lighthouse performance must not regress below 85

Also add:
- Proper page metadata / titles per route
- A favicon and an OG image
- Loading and error boundaries per route segment
- 404 page
```

### PROMPT 12.2 — Audit

```
Verify Phase 12 with real evidence:

1. npm run build (must pass — show output)
2. Run Lighthouse on the landing page (production build, not dev).
   Show the actual scores. Performance must be >= 85. If not, report what is
   costing the most and fix it.
3. Enable prefers-reduced-motion in DevTools and confirm animations are
   disabled. Describe what actually changed.
4. Confirm no layout shift on load (show the CLS score).
5. Grep the frontend for "three", "@react-three", "webgl" — confirm none exist.
6. Test at 375px, 768px, and 1440px. Report any overflow or broken layout.
7. Confirm the app still works end-to-end after the visual changes: run the
   full booking flow once more and show it succeeding.
```

---

## Phase 13 — Unit tests

### PROMPT 13.1 — Build

```
Write meaningful unit tests for the backend. Prioritize the logic that would
actually be probed in an interview — do not chase coverage on getters.

Priority 1 — BookingService (JUnit 5 + Mockito):
- Booking an available seat succeeds and returns a reference
- Booking a BOOKED seat throws SeatUnavailableException
- Booking a nonexistent seat throws NotFoundException
- Cancelling releases the seat back to AVAILABLE
- Cancelling someone else's booking is rejected
- Cancelling an already-cancelled booking is rejected
- The event is published only on success, never on failure
- Verify the locking repository method is the one called — not the plain
  findById. This test exists to catch a future refactor silently removing the
  lock.

Priority 2 — JwtService:
- A generated token validates
- An expired token is rejected
- A tampered token is rejected
- The subject and role claims round-trip correctly

Priority 3 — Search services:
- Past dates are rejected
- Sorting works as specified
- availableSeats is computed correctly

Priority 4 — Integration test with Testcontainers (only if time allows):
- Real Postgres, full booking flow through the service layer
- A concurrent double-booking test at the integration level

Use @ExtendWith(MockitoExtension.class), AssertJ assertions, and
descriptive test method names (shouldRejectBookingWhenSeatAlreadyBooked).
Do NOT write tests that assert mocks were called for their own sake — test
behavior.
```

### PROMPT 13.2 — Audit

```
Verify Phase 13 with real evidence:

1. ./mvnw test — show the FULL output including the test count and results.
   All tests must pass.
2. Show the test report summary: how many tests, how many per class.
3. Generate a coverage report (add jacoco if needed) and show the actual
   numbers for BookingService and JwtService specifically. I care about those
   two, not the overall percentage.
4. Prove the tests are real: deliberately break BookingService (change the
   locking call to plain findById), run the tests, and show that a test FAILS.
   Then revert and show the tests pass again.
   A test suite that stays green while the lock is removed is worthless.
5. Report honestly: which parts of the booking logic are NOT covered?
```

---

## Phase 14 — Documentation & architecture diagram

### PROMPT 14.1 — Build

```
Write the project documentation. Be accurate and honest — do not oversell.
Anything not actually implemented must be labeled as not implemented.

README.md at repo root:

1. Title + one-paragraph description: a flight & hotel booking platform built
   to explore the engineering problems real booking systems face — concurrent
   inventory, caching, horizontal scaling, and event-driven side effects.

2. Architecture diagram (Mermaid):
   browser -> nginx -> backend-1/backend-2 -> postgres/redis/kafka ->
   notification consumer

3. Tech stack table with a WHY column for each choice.

4. "Engineering decisions" section — the most important part. For each, state
   the problem, the decision, and the tradeoff:
   a. Pessimistic locking over optimistic locking for seat inventory
      (high contention on last seats; optimistic retries would storm)
   b. Database-level locking over JVM locking
      (synchronized does not work across 2 instances — explain explicitly)
   c. Redis over in-process cache
      (instance-local cache serves stale availability after the other instance
      books)
   d. JWT over server-side sessions
      (statelessness is what makes round-robin load balancing possible)
   e. Kafka publish AFTER_COMMIT, non-blocking
      (never announce a booking that rolled back; never make the user wait on
      notification delivery)

5. "Verified behavior" section, linking to real captured evidence:
   - docs/concurrency-test-result.txt — 50 concurrent requests for 1 seat ->
     exactly 1 success, 49 conflicts
   - Load balancing across both instances
   - Failover with one instance down
   - Booking succeeding while Kafka is down

6. Setup instructions: docker compose up, seed data, frontend npm run dev,
   and the URLs.

7. API documentation: every endpoint, method, auth requirement, request/response
   example. Add springdoc-openapi and link the Swagger UI.

8. "Scope and limitations" — explicit and unapologetic:
   - No real payment gateway (dummy status only)
   - No 3D seat rendering — a deliberate choice to spend the time on backend
     depth instead
   - No ElasticSearch/MongoDB/Cassandra — Postgres is sufficient at this scale;
     ElasticSearch would be the next addition for full-text search
   - No Kubernetes/multi-region — nginx + 2 instances demonstrates the concept
   - Frontend deployed to Vercel; backend runs locally via Docker Compose

9. Screenshots of the UI.

Also create docs/ARCHITECTURE.md going deeper on the booking sequence
(Mermaid sequence diagram: request -> nginx -> backend -> SELECT FOR UPDATE ->
commit -> AFTER_COMMIT publish -> consumer), and the failure modes considered.

Write in clear English. No filler, no marketing language.
```

### PROMPT 14.2 — Audit

```
Final audit. Verify with real evidence:

1. Fresh-clone test — the most important one:
   Clone the repo to a brand new directory and follow the README setup steps
   EXACTLY as written, with nothing from memory. Report every step that fails
   or requires undocumented knowledge. Fix the README until a clean run works.
   Show the output.

2. Confirm every claim in the README is true. Go claim by claim. If the README
   says something works, prove it with a command. Remove or correct anything
   unproven.

3. Confirm docs/concurrency-test-result.txt is real captured output, not
   hand-written.

4. Verify the Mermaid diagrams render (check on GitHub or a Mermaid preview).

5. Confirm Swagger UI loads and every endpoint is documented.

6. Full end-to-end smoke test on the fresh clone: register -> login -> search ->
   book -> see notification in logs -> cancel. Show the output.

7. Check the repo for leaked secrets, .env files, or hardcoded credentials.

8. Confirm the README's limitations section is honest — nothing claimed that
   is not built.
```

---

## PRE-INTERVIEW PREP (after the build)

Questions to be able to answer without notes. If any answer is shaky, the
project is not finished:

**Concurrency**
- Why pessimistic and not optimistic locking here? When would you flip?
- What happens with 1000 concurrent requests instead of 50?
- What if the DB connection pool is smaller than the concurrent requests?
- How would you handle this if seats lived across multiple databases?

**Scaling**
- Why does JWT let you round-robin, and what does that cost you?
  (No instant revocation — how would you solve it?)
- Why not sticky sessions? What would break the moment you added them?
- Where is the bottleneck now, and what would you scale first?

**Caching**
- Why 5-minute TTL? What is the cost of stale data here?
- What happens on cache stampede when a popular route expires?
- Why is a local cache wrong here specifically?

**Kafka**
- Why publish after commit? What breaks if you publish inside the transaction?
- What happens if the consumer dies mid-processing?
- At-least-once vs exactly-once — which do you have, and does it matter here?

**Honest gaps — say these out loud, do not hide them**
- Java and Spring Boot are new to me; I learned them for this project over four
  weeks. My production experience is in PHP/Laravel and Node/TypeScript.
- I have not run this at real scale — 2 instances on one machine is not 50
  million users.
- ElasticSearch, Cassandra, and MongoDB I have read about, not shipped.
