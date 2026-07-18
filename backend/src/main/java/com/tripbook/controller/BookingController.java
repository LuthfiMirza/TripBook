package com.tripbook.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tripbook.dto.BookingResponse;
import com.tripbook.dto.FlightBookingRequest;
import com.tripbook.dto.HotelBookingRequest;
import com.tripbook.dto.PagedResponse;
import com.tripbook.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/flight")
    public ResponseEntity<BookingResponse> bookFlight(
            Authentication authentication,
            @Valid @RequestBody FlightBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.bookFlight(authentication.getName(), request));
    }

    @PostMapping("/hotel")
    public ResponseEntity<BookingResponse> bookHotel(
            Authentication authentication,
            @Valid @RequestBody HotelBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.bookHotel(authentication.getName(), request));
    }

    @GetMapping
    public PagedResponse<BookingResponse> myBookings(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookingService.getMyBookings(authentication.getName(), page, size);
    }

    @GetMapping("/{reference}")
    public BookingResponse myBooking(Authentication authentication, @PathVariable String reference) {
        return bookingService.getMyBooking(authentication.getName(), reference);
    }

    @PostMapping("/{reference}/cancel")
    public BookingResponse cancel(Authentication authentication, @PathVariable String reference) {
        return bookingService.cancel(authentication.getName(), reference);
    }
}
