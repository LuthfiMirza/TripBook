package com.tripbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FlightBookingRequest(
        @NotNull Long flightId,
        @NotBlank String seatNumber,
        @NotBlank String passengerName) {
}
