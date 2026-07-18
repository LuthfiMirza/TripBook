package com.tripbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripbook.entity.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}
