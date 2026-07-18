package com.tripbook.exception;

/** Generic 400 for request-level validation that isn't bean-validation on a body (e.g. query param cross-checks). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
