package com.tripbook.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Consistent error shape returned by every failure path in the API. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        this(LocalDateTime.now(), status, error, message, path, fieldErrors);
    }
}
