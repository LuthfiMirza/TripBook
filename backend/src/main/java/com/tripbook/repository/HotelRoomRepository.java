package com.tripbook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.HotelRoom;

public interface HotelRoomRepository extends JpaRepository<HotelRoom, Long> {

    List<HotelRoom> findByHotel_IdOrderById(Long hotelId);
}
