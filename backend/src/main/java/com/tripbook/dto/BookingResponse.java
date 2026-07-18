package com.tripbook.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tripbook.entity.Booking;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingResponse(
        String bookingReference,
        String bookingType,
        String status,
        BigDecimal totalPrice,
        LocalDate checkIn,
        LocalDate checkOut,
        LocalDateTime createdAt,
        FlightItem flight,
        HotelItem hotel) {

    public static BookingResponse from(Booking booking) {
        FlightItem flight = null;
        if (booking.getFlightSeat() != null) {
            var seat = booking.getFlightSeat();
            var flightEntity = seat.getFlight();
            flight = new FlightItem(
                    flightEntity.getId(), flightEntity.getFlightCode(), flightEntity.getAirline(),
                    flightEntity.getOrigin(), flightEntity.getDestination(), flightEntity.getDepartureTime(),
                    flightEntity.getArrivalTime(), seat.getSeatNumber(), seat.getSeatClass());
        }

        HotelItem hotel = null;
        if (booking.getHotelRoom() != null) {
            var room = booking.getHotelRoom();
            var hotelEntity = room.getHotel();
            hotel = new HotelItem(
                    hotelEntity.getId(), hotelEntity.getName(), hotelEntity.getCity(),
                    room.getRoomNumber(), room.getRoomType());
        }

        return new BookingResponse(
                booking.getBookingReference(), booking.getBookingType(), booking.getStatus(),
                booking.getTotalPrice(), booking.getCheckIn(), booking.getCheckOut(),
                booking.getCreatedAt(), flight, hotel);
    }

    public record FlightItem(
            Long flightId,
            String flightCode,
            String airline,
            String origin,
            String destination,
            java.time.LocalDateTime departureTime,
            java.time.LocalDateTime arrivalTime,
            String seatNumber,
            String seatClass) {
    }

    public record HotelItem(Long hotelId, String name, String city, String roomNumber, String roomType) {
    }
}
