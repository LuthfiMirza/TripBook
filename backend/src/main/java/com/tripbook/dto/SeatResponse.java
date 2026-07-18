package com.tripbook.dto;

import com.tripbook.entity.FlightSeat;

public record SeatResponse(Long id, String seatNumber, String seatClass, String status) {

    public static SeatResponse from(FlightSeat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getSeatClass(), seat.getStatus());
    }
}
