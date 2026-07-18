package com.tripbook.dto;

import java.math.BigDecimal;

public record HotelSearchResponse(
        Long id,
        String name,
        String city,
        String address,
        BigDecimal pricePerNight,
        Integer starRating,
        long availableRooms) {
}
