package com.tripbook.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightSearchResponse(
        Long id,
        String flightCode,
        String airline,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        BigDecimal price,
        long availableSeats) {
}
