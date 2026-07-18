package com.tripbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
