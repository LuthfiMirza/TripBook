package com.tripbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.FlightSeat;

public interface FlightSeatRepository extends JpaRepository<FlightSeat, Long> {
}
