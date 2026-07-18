package com.tripbook.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingConfirmedEvent(
        String bookingReference,
        Long userId,
        String userEmail,
        String bookingType,
        String itemSummary,
        BigDecimal totalPrice,
        LocalDateTime occurredAt) {
}
