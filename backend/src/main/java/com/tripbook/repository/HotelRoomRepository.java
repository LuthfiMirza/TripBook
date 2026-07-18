package com.tripbook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.tripbook.entity.HotelRoom;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface HotelRoomRepository extends JpaRepository<HotelRoom, Long> {

    /**
     * Booking locks the row pessimistically because scarce remaining rooms are
     * high-contention inventory; optimistic locking would push collision retry
     * loops to callers across multiple backend JVMs.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"),
            @QueryHint(name = "javax.persistence.lock.timeout", value = "3000")
    })
    @Query("""
            SELECT r FROM HotelRoom r
            JOIN FETCH r.hotel h
            WHERE h.id = :hotelId AND r.roomNumber = :roomNumber
            """)
    Optional<HotelRoom> findByHotelIdAndRoomNumberForUpdate(
            @Param("hotelId") Long hotelId,
            @Param("roomNumber") String roomNumber);

    List<HotelRoom> findByHotel_IdOrderById(Long hotelId);
}
