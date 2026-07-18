package com.tripbook.dto;

import java.math.BigDecimal;
import java.util.List;

public record HotelDetailResponse(
        Long id,
        String name,
        String city,
        String address,
        BigDecimal pricePerNight,
        Integer starRating,
        List<RoomResponse> rooms) {
}
