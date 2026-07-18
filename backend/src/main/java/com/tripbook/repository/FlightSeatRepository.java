package com.tripbook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.FlightSeat;

public interface FlightSeatRepository extends JpaRepository<FlightSeat, Long> {

    // Ordered by id, not seat_number: seat_number is VARCHAR ("1A".."12F"), so
    // a lexical sort would put "10A" before "2A". Seats are inserted row-by-row
    // in the correct order (Phase 1 seed, Phase 3 auto-generation), so id order
    // already matches the physical seat map.
    List<FlightSeat> findByFlight_IdOrderById(Long flightId);
}
