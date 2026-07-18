package com.tripbook.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FlightDetailResponse(
        Long id,
        String flightCode,
        String airline,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        BigDecimal price,
        int totalSeats,
        List<SeatResponse> seats) {
}
