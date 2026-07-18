package com.tripbook.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FlightRequest(
        @NotBlank String flightCode,
        @NotBlank String airline,
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull LocalDateTime departureTime,
        @NotNull LocalDateTime arrivalTime,
        @NotNull @Positive BigDecimal price,
        @NotNull @Positive Integer totalSeats) {
}
