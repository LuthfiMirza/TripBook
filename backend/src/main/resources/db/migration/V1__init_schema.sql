-- TripBook core domain schema.
-- Flyway owns this schema; Hibernate runs with ddl-auto=validate, never create/alter.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE flights (
    id             BIGSERIAL PRIMARY KEY,
    flight_code    VARCHAR(20)   NOT NULL UNIQUE,
    airline        VARCHAR(100)  NOT NULL,
    origin         VARCHAR(10)   NOT NULL,
    destination    VARCHAR(10)   NOT NULL,
    departure_time TIMESTAMP     NOT NULL,
    arrival_time   TIMESTAMP     NOT NULL,
    price          NUMERIC(12,2) NOT NULL,
    total_seats    INT           NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_flights_route_departure ON flights (origin, destination, departure_time);

CREATE TABLE flight_seats (
    id          BIGSERIAL PRIMARY KEY,
    flight_id   BIGINT       NOT NULL REFERENCES flights (id),
    seat_number VARCHAR(10)  NOT NULL,
    seat_class  VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    version     BIGINT       NOT NULL DEFAULT 0,
    UNIQUE (flight_id, seat_number)
);

CREATE TABLE hotels (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)  NOT NULL,
    city            VARCHAR(100)  NOT NULL,
    address         VARCHAR(255),
    price_per_night NUMERIC(12,2) NOT NULL,
    star_rating     INT,
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_hotels_city ON hotels (city);

CREATE TABLE hotel_rooms (
    id          BIGSERIAL PRIMARY KEY,
    hotel_id    BIGINT      NOT NULL REFERENCES hotels (id),
    room_number VARCHAR(20) NOT NULL,
    room_type   VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version     BIGINT      NOT NULL DEFAULT 0,
    UNIQUE (hotel_id, room_number)
);

CREATE TABLE bookings (
    id                 BIGSERIAL PRIMARY KEY,
    booking_reference  VARCHAR(20)   NOT NULL UNIQUE,
    user_id            BIGINT        NOT NULL REFERENCES users (id),
    booking_type       VARCHAR(20)   NOT NULL, -- FLIGHT | HOTEL
    flight_seat_id     BIGINT        NULL REFERENCES flight_seats (id),
    hotel_room_id      BIGINT        NULL REFERENCES hotel_rooms (id),
    check_in           DATE          NULL,
    check_out          DATE          NULL,
    total_price        NUMERIC(12,2) NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING | CONFIRMED | CANCELLED
    created_at         TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_bookings_user ON bookings (user_id);
