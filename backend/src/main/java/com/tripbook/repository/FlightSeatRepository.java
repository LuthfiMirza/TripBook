package com.tripbook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.tripbook.entity.FlightSeat;

public interface FlightSeatRepository extends JpaRepository<FlightSeat, Long> {

    /**
     * Ordered by the seat_number's numeric row then its letter — NOT by id.
     * id order was tried first and looked right by construction (rows
     * inserted row-by-row, letter by letter), but Postgres does not guarantee
     * CROSS JOIN / generate_series iteration order without an explicit
     * ORDER BY on the feeding SELECT, and the Phase 1 seed migration
     * (V2__seed_data.sql) has no such ORDER BY. It happened to insert in
     * letter-major order (all rows' "A" seats, then all "B" seats, ...),
     * which this endpoint's own audit caught: GET /api/flights/1 returned
     * 1A, 2A, 3A, ... instead of 1A, 1B, 1C, .... Parsing the actual seat
     * number is correct regardless of insertion order, including for any
     * future insertion path.
     */
    @Query(value = """
            SELECT * FROM flight_seats
            WHERE flight_id = :flightId
            ORDER BY (regexp_replace(seat_number, '[^0-9]', '', 'g'))::int,
                     regexp_replace(seat_number, '[0-9]', '', 'g')
            """, nativeQuery = true)
    List<FlightSeat> findSeatMapByFlightId(@Param("flightId") Long flightId);
}
