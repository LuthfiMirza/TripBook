package com.tripbook.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HotelRequest(
        @NotBlank String name,
        @NotBlank String city,
        String address,
        @NotNull @Positive BigDecimal pricePerNight,
        Integer starRating) {
}
