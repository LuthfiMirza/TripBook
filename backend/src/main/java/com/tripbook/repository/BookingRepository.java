package com.tripbook.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"flightSeat", "flightSeat.flight", "hotelRoom", "hotelRoom.hotel"})
    Page<Booking> findByUser_EmailOrderByCreatedAtDesc(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"flightSeat", "flightSeat.flight", "hotelRoom", "hotelRoom.hotel"})
    Optional<Booking> findByBookingReferenceAndUser_Email(String bookingReference, String email);

    boolean existsByBookingReference(String bookingReference);
}
