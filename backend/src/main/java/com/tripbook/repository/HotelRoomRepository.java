package com.tripbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.HotelRoom;

public interface HotelRoomRepository extends JpaRepository<HotelRoom, Long> {
}
