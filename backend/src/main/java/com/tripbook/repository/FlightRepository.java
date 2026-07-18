package com.tripbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.Flight;

public interface FlightRepository extends JpaRepository<Flight, Long> {
}
