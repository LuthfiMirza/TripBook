package com.tripbook.exception;

/** Generic 404 — reused by later phases (flights, hotels, bookings), not just auth. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
